package io.grimlock257.dnaos.loadbalancer.managers;

import io.grimlock257.dnaos.loadbalancer.AllocationMethod;
import io.grimlock257.dnaos.loadbalancer.message.MessageType;
import io.grimlock257.dnaos.loadbalancer.node.Node;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

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

    private LinkedList<Node> nodes;

    /**
     * NodeManager constructor
     */
    private NodeManager() {
        this.nodes = new LinkedList<>();
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
     * Add a node to the nodes LinkedList providing a node of the same name doesn't already exist
     * or the supplied address / port combination already exists
     *
     * @param node The node to add to the nodes LinkedList
     *
     * @return Whether or not the addition was successful, false is matching name or address / port combination found
     */
    public boolean addNode(Node node) {
        for (Node searchNode : nodes) {
            if (searchNode.getName().equals(node.getName()) || (searchNode.getAddr().equals(node.getAddr()) && searchNode.getPort() == node.getPort())) {
                return false;
            }
        }

        nodes.add(node);

        return true;
    }

    /**
     * Remove a node from the nodes LinkedList
     *
     * @param node The node to remove from the nodes LinkedList
     */
    public void removeNode(Node node) {
        nodes.remove(node);
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

                // If the LinkedList is empty, return null, otherwise make sure the first element in the list has a
                // usage of less than 100%, is so return this node otherwise return null
                if (!nodes.isEmpty()) {
                    freestNode = (nodes.get(0).calcUsage() < 100) ? nodes.get(0) : null;
                }

                break;
            case NON_WEIGHTED:
                // If the LinkedList is empty, return null, otherwise check if the nodeToUse = nodes
                // LinkedLIst size (if match, we reached end of the LinkedList), if so, set nodeToUse
                // to 0 and return the node at that position, otherwise just return the node at nodeToUse
                if (!nodes.isEmpty()) {
                    freestNode = (nodes.get(nodeToUse).calcUsage() < 100) ? nodes.get(nodeToUse) : null;

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

            if ((nodes.get(nodeToUse).calcUsage() < 100)) {
                break;
            }
        }
    }

    /**
     * Sort the nodes LinkedList by ascending workload then descending capacity (for use with the Weighted Round-Robin)
     */
    private void sortNodes() {
        nodes.sort(new Comparator<Node>() {
            @Override
            public int compare(Node node1, Node node2) {
                double usageDifference = node1.calcUsage() - node2.calcUsage();

                if (usageDifference < 0) {
                    return -1;
                } else if (usageDifference > 0) {
                    return 1;
                }

                return node2.getCapacity() - node1.getCapacity();
            }
        });
    }

    /**
     * Send a data dump request to all connected nodes
     */
    public void issueDataDumps() {
        for (Node node : nodes) {
            MessageManager.getInstance().send(MessageType.DATA_DUMP_NODE.toString(), node.getAddr(), node.getPort());
            System.out.println("[INFO] Data dump request sent to node '" + node.getName() + "'\n");
        }
    }

    /**
     * Send shutdown message all connected nodes
     */
    public void shutdownAllNodes() {
        // Create an iterator to iterate over the nodes ArrayList
        Iterator<Node> itr = nodes.iterator();

        // While there is another item, get that item and remove it
        while (itr.hasNext()) {
            Node node = itr.next();

            // Send message to node saying shutdown
            MessageManager.getInstance().send(MessageType.NODE_SHUTDOWN.toString(), node.getAddr(), node.getPort());

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
        MessageManager.getInstance().send(MessageType.NODE_SHUTDOWN.toString(), node.getAddr(), node.getPort());
        System.out.println("");

        removeNode(node);

        // Deallocate jobs relating to the specified node
        JobManager.getInstance().deallocateJobs(node);
    }

    /**
     * Find the specified node object in the nodes LinkedList using the supplied name
     *
     * @param nodeName The name of the node to locate in the nodes LinkedList
     *
     * @return The node object matching the name, or null if not found
     */
    public Node findByName(String nodeName) {
        for (Node node : nodes) {
            if (node.getName().equals(nodeName)) {
                return node;
            }
        }

        return null;
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
     * Used to display the nodes LinkedList in a nice, readable format
     *
     * @return The formatted string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Iterate through the nodes LinkedList, appending each node to the output
        int i = 0;
        for (Node node : nodes) {
            i++;

            sb.append(node.toString());

            // If we haven't reached the end of the list, add a new line
            if (i != nodes.size())
                sb.append("\n");
        }

        return sb.toString();
    }
}
