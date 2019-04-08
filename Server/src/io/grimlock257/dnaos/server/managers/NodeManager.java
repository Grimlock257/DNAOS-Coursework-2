package io.grimlock257.dnaos.server.managers;

import io.grimlock257.dnaos.server.node.Node;

import java.util.LinkedList;

/**
 * Node Manager for Server project
 * This class handles the storage of connected nodes and the allocation of jobs to nodes
 * <p>
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

        System.out.println("[DEBUG] Nodes list: \n" + this.toString());
    }

    /**
     * Remove a node from the nodes LinkedList
     *
     * @param node The node to remove from the nodes LinkedList
     */
    // TODO: Untested
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
     * Used to display the nodes LinkedList in a nice, readable format
     *
     * @return The formatted string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (Node node : nodes) {
            i++;

            sb.append(node.toString());

            if (i != nodes.size())
                sb.append("\n");
        }

        return sb.toString();
    }
}
