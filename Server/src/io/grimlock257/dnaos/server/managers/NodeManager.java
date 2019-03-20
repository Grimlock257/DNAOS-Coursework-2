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
    private LinkedList<Node> nodes;

    /**
     * NodeManager constructor
     */
    public NodeManager() {
        this.nodes = new LinkedList<>();
    }

    /**
     * Add a node to the nodes LinkedList
     *
     * @param node The node to add to the nodes LinkedList
     */
    public void addNode(Node node) {
        nodes.add(node);

        System.out.println("[DEBUG] Nodes list: " + nodes.toString());
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
}
