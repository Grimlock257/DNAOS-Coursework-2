package io.grimlock257.dnaos.node;

/**
 * Main class of Node project
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Main {
    public static void main(String[] args) {
        new Main();
    }

    /**
     * Create nonstatic instance of main which creates a new nonstatic Node with the specified port to be active on
     */
    public Main() {
        new Node("Node 1", 5, 5001, "localhost", 4000);
    }
}