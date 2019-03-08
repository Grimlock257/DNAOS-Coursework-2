package io.grimlock257.dnaos.server;

/**
 * Main class of Server project
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Main {
    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        System.out.println("[INFO] LoadBalancer online");

        new LoadBalancer(4000);
    }
}