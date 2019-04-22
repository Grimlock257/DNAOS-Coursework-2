package io.grimlock257.dnaos.loadbalancer.managers;

import io.grimlock257.dnaos.loadbalancer.AllocationMethod;
import io.grimlock257.dnaos.loadbalancer.message.MessageTypeOut;
import io.grimlock257.dnaos.loadbalancer.node.Node;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Node Manager for Load Balancer project
 * This class handles the storage of connected nodes and the allocation of jobs to nodes
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class NodeManager {
    private static NodeManager instance = null;

    private AllocationMethod allocationMethod;
    private int nodeToUse = 0; // Stores index of next Node to use, used in non-weighted round-robin

    private LinkedHashMap<Node, Timer> nodes;

    // How frequently to check whether a node is still available or not
    private final int CHECK_ALIVE_INTERVAL = 3 * 60 * 1000;

    // Maximum amount of allowed strikes before removing the node
    private final int MAXIMUM_STRIKES = 3;

    /**
     * NodeManager constructor
     */
    private NodeManager() {
        this.nodes = new LinkedHashMap<>();
    }

    /**
     * Get the instance of the NodeManager singleton
     *
     * @return The instance of the NodeManager
     */
    public static NodeManager getInstance() {
        if (instance == null) {
            instance = new NodeManager();
        }

        return instance;
    }

    /**
     * Add a node to the nodes LinkedHashMap providing a node of the same name doesn't already exist
     * or the supplied address / port combination already exists
     *
     * @param node The node to add to the nodes LinkedHashMap
     *
     * @return Whether or not the addition was successful, false is matching name or address / port combination found
     */
    public boolean addNode(Node node) {
        for (Node searchNode : nodes.keySet()) {
            if (searchNode.getName().equals(node.getName()) || (searchNode.getAddr().equals(node.getAddr()) && searchNode.getPort() == node.getPort())) {
                return false;
            }
        }

        nodes.put(node, new Timer());
        resetIsAliveTimer(node.getName());

        // Sort the nodes as new node
        if (allocationMethod == AllocationMethod.WEIGHTED) {
            sortNodes();
        }

        return true;
    }

    /**
     * Remove a node from the nodes LinkedHashMap
     *
     * @param node The node to remove from the nodes LinkedHashMap
     */
    public void removeNode(Node node) {
        // Stop the isAliveTimer for the node
        stopIsAliveTimer(node);

        nodes.remove(node);

        // Sort the nodes as node removed
        if (allocationMethod == AllocationMethod.WEIGHTED) {
            sortNodes();
        }
    }

    /**
     * Retrieve the freest node, chosen based on the current allocation method
     *
     * @return The freest Node
     */
    public Node getFreestNode() {
        Node freestNode = null;

        // Select the freest node based on the allocation method
        switch (allocationMethod) {
            case WEIGHTED:
                // Make sure the nodes are sorted
                sortNodes();

                // If the LinkedHashMap is empty, return null, otherwise make sure the first element in the list has a
                // usage of less than 100%, is so return this node otherwise return null
                if (!nodes.isEmpty()) {
                    freestNode = (getNode(0).calcUsage() < 100) ? getNode(0) : null;
                }

                break;
            case NON_WEIGHTED:
                // If the LinkedHashMap is empty, return null, otherwise check if the nodeToUse = nodes
                // LinkedHashMap size (if match, we reached end of the LinkedHashMap), if so, set nodeToUse
                // to 0 and return the node at that position, otherwise just return the node at nodeToUse
                if (!nodes.isEmpty()) {
                    freestNode = (getNode(nodeToUse).calcUsage() < 100) ? getNode(nodeToUse) : null;

                    incrementNodeToUse();
                }

                break;
        }

        return freestNode;
    }

    /**
     * Attempt to increment the nodeToUse variable to know which node to use next
     */
    private void incrementNodeToUse() {
        for (int i = 0; i < nodes.size(); i++) {
            nodeToUse = ++nodeToUse % nodes.size();

            if ((getNode(nodeToUse).calcUsage() < 100)) {
                break;
            }
        }
    }

    /**
     * Sort the nodes LinkedHashMap by ascending workload then descending capacity (for use with the Weighted Round-Robin)
     */
    private void sortNodes() {
        nodes = nodes.entrySet().stream().sorted(new Comparator<Map.Entry<Node, Timer>>() {
            @Override
            public int compare(Map.Entry<Node, Timer> entry1, Map.Entry<Node, Timer> entry2) {
                Node node1 = entry1.getKey();
                Node node2 = entry2.getKey();

                double usageDifference = node1.calcUsage() - node2.calcUsage();

                if (usageDifference < 0) {
                    return -1;
                } else if (usageDifference > 0) {
                    return 1;
                }

                return node2.getCapacity() - node1.getCapacity();
            }
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new
        ));
    }

    /**
     * Reset the isAlive timer for the specified Node. The timer sends a IS_ALIVE message to the associated Node
     * at a specified time interval to see if the node is still reachable. This method should only be called when
     * a message has been received from the Node in question
     *
     * @param nodeName The name of the node to reset the timer for
     */
    public void resetIsAliveTimer(String nodeName) {
        // Iterate through the LinkedHashMap to find the Node matching the supplied nodeName
        for (Map.Entry<Node, Timer> nodeDetails : nodes.entrySet()) {
            // Store key/value as variable for ease
            Node theNode = nodeDetails.getKey();
            Timer theTimer = nodeDetails.getValue();

            // The current iteration node name matches the supplied nodeName
            if (theNode.getName().equalsIgnoreCase(nodeName)) {
                // Received a message from the node (as this method has been called), so reset strikes
                theNode.resetStrikes();

                // Cancel the existing time and remove
                theTimer.cancel();
                theTimer.purge();

                // Create a new Timer object in its place
                theTimer = new Timer();

                // Store the new Timer object in the LinkedHashMap, overwriting the old
                nodeDetails.setValue(theTimer);

                // Create a timer to send a IS_ALIVE message every specified time interval
                theTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            if (theNode.getStrikes() >= MAXIMUM_STRIKES) {
                                System.out.println("===============================================================================");
                                System.out.println("[INFO] Maximum strikes for IS_ALIVE reached for node '" + theNode.getName() + "', initiating removal...");
                                shutdownNode(theNode);
                            } else {
                                // Send a IS_ALIVE message to the node
                                System.out.println("===============================================================================");
                                MessageManager.getInstance().send(MessageTypeOut.IS_ALIVE.toString(), theNode.getAddr(), theNode.getPort());
                                System.out.println("[INFO] Issue IS_ALIVE message to node '" + theNode.getName() + "'");
                                theNode.incrementStrikes();
                            }
                        } catch (Exception e) {
                            System.err.println("[ERROR] Unhandled Exception thrown");
                            e.printStackTrace();
                        }
                    }
                }, CHECK_ALIVE_INTERVAL, CHECK_ALIVE_INTERVAL);
            }
        }
    }

    /**
     * Stop the isAlive timer for a node
     *
     * @param node The node whose isAliveTimer to stop
     */
    private void stopIsAliveTimer(Node node) {
        // Iterate through the LinkedHashMap to find the Node matching the supplied node
        for (Map.Entry<Node, Timer> nodeDetails : nodes.entrySet()) {
            // Store key/value as variable for ease
            Node theNode = nodeDetails.getKey();
            Timer theTimer = nodeDetails.getValue();

            // The current iteration node name matches the supplied nodeName
            if (theNode.equals(node)) {
                // Received a message from the node (as this method has been called), so reset strikes
                theNode.resetStrikes();

                // Cancel the existing time and remove
                theTimer.cancel();
                theTimer.purge();

                // Create a new Timer object in its place
                theTimer = new Timer();

                // Store the new Timer object in the LinkedHashMap, overwriting the old
                nodeDetails.setValue(theTimer);
            }
        }
    }

    /**
     * Send a data dump request to all connected nodes
     */
    public void issueDataDumps() {
        for (Node node : nodes.keySet()) {
            MessageManager.getInstance().send(MessageTypeOut.DATA_DUMP_NODE.toString(), node.getAddr(), node.getPort());
            System.out.println("[INFO] Data dump request sent to node '" + node.getName() + "'\n");
        }
    }

    /**
     * Send shutdown message all connected nodes
     */
    public void shutdownAllNodes() {
        // Create an iterator to iterate over the nodes ArrayList
        Iterator<Node> itr = nodes.keySet().iterator();

        // While there is another item, get that item and remove it
        while (itr.hasNext()) {
            Node node = itr.next();

            // Send message to node saying shutdown
            MessageManager.getInstance().send(MessageTypeOut.NODE_SHUTDOWN.toString(), node.getAddr(), node.getPort());

            // Stop the isAliveTimer for the node
            stopIsAliveTimer(node);

            // Remove the node from the list
            itr.remove();

            // Deallocate jobs relating to the current node
            JobManager.getInstance().deallocateJobs(node);

            System.out.println("Node removed from node list: " + node.toString());

            // If there's another entry in the iterator, add a new line
            if (itr.hasNext())
                System.out.println("");
        }
    }

    /**
     * Send shutdown message to the specific node
     *
     * @param node The node to shutdown
     */
    public void shutdownNode(Node node) {
        MessageManager.getInstance().send(MessageTypeOut.NODE_SHUTDOWN.toString(), node.getAddr(), node.getPort());
        System.out.println("");

        removeNode(node);

        // Deallocate jobs relating to the specified node
        JobManager.getInstance().deallocateJobs(node);
    }

    /**
     * Remove and deallocate jobs from the resigned node
     *
     * @param node The node that has resigned
     */
    public void shutdownViaResign(Node node) {
        removeNode(node);

        // Deallocate jobs relating to the specified node
        JobManager.getInstance().deallocateJobs(node);
    }

    /**
     * Find the specified node object in the nodes LinkedHashMap using the supplied name
     *
     * @param nodeName The name of the node to locate in the nodes LinkedHashMap
     *
     * @return The node object matching the name, or null if not found
     */
    public Node findByName(String nodeName) {
        for (Node node : nodes.keySet()) {
            if (node.getName().equalsIgnoreCase(nodeName)) {
                return node;
            }
        }

        return null;
    }

    /**
     * Find the Node object at the specified index
     *
     * @param index The index at which to retrieve a node
     *
     * @return The specified Node, or null if not found
     */
    private Node getNode(int index) {
        return (Node) nodes.keySet().toArray()[index];
    }

    /**
     * Set the allocation method to use to the supplied value
     *
     * @param allocationMethod The new allocation method to use
     */
    public void setAllocationMethod(AllocationMethod allocationMethod) {
        this.allocationMethod = allocationMethod;
    }

    /**
     * Used to display the nodes LinkedHashMap in a nice, readable format
     *
     * @return The formatted string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Iterate through the nodes LinkedHashMap, appending each node to the output
        int i = 0;
        for (Node node : nodes.keySet()) {
            i++;

            sb.append(node.toString());

            // If we haven't reached the end of the list, add a new line
            if (i != nodes.size())
                sb.append("\n");
        }

        return sb.toString();
    }
}
