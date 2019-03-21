package io.grimlock257.dnaos.client;

/**
 * Main class of Client project
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
        new Client(4999, "localhost", 4000);
    }
}