package io.grimlock257.dnaos.server;

/**
 * Main class of Server project
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Main {
    /**
     * Entry port for the program.
     * <p>
     * Takes in command line arguments and uses them to initialise a nonstatic instance of the Load Balancer
     *
     * @param args The command line arguments supplied
     */
    public static void main(String[] args) {
        if (args.length == 1) {
            // Get parameters from the supplied command line arguments
            int port = Integer.parseInt(args[0]);

            System.out.println("[INFO] LoadBalancer online");
            new LoadBalancer(port);
        } else {
            System.err.println("Invalid arguments supplied! Usage: <port>");
        }
    }
}