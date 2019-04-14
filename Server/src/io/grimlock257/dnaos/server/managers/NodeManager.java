package io.grimlock257.dnaos.server.managers;

import io.grimlock257.dnaos.server.message.MessageType;
import io.grimlock257.dnaos.server.node.Node;

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
     * Add a node to the nodes LinkedList
     *
     * @param node The node to add to the nodes LinkedList
     */
    public void addNode(Node node) {
        nodes.add(node);
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
     * Work out which Node is the most free by calculating the current usage of each Node
     *
     * @return The freest Node
     */
    public Node getFreestNode() {
        Node freestNode = null;

        // Iterate over the nodes LinkedList, checking the usage of the current found freest
        // Node and comparing it with the current iteration node
        for (Node node : nodes) {
            if (freestNode == null) {
                freestNode = node;
            } else if (node.calcUsage() < freestNode.calcUsage()) {
                freestNode = node;
            }
        }

        return freestNode;
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

        removeNode(node);
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
