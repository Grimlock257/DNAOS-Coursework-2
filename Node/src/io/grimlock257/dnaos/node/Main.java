package io.grimlock257.dnaos.node;

/**
 * Main class of Node project
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Main {
    /**
     * Entry port for the program.
     * <p>
     * Takes in command line arguments and uses them to initialise a nonstatic instance of the Node
     *
     * @param args The command line arguments supplied
     */
    public static void main(String[] args) {
        if (args.length == 5) {
            // Get parameters from the supplied command line arguments
            String name = args[0];
            int capacity = Integer.parseInt(args[1]);
            int port = Integer.parseInt(args[2]);
            String lbHost = args[3];
            int lbPort = Integer.parseInt(args[4]);

            new Node(name, capacity, port, lbHost, lbPort);
        } else {
            System.err.println("Invalid arguments supplied! Usage: <name> <capacity> <port> <load balancer host address> <load balancer port>");
        }
    }
}