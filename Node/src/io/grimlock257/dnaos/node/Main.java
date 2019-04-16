package io.grimlock257.dnaos.node;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Main class of Node project
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Main {
    /**
     * Entry port for the program.
     *
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

            InetAddress addr = null;
            try {
                addr = InetAddress.getByName("localhost");
            } catch (UnknownHostException e) {
                System.err.println("[ERROR] UnknownHostException thrown, exiting...");
                e.printStackTrace();
                System.exit(1);
            }

            // Display information about the Node
            System.out.println("[INFO] Node online");
            System.out.println("[INFO] Node details:");
            System.out.println("[INFO] - Name: " + name);
            System.out.println("[INFO] - Capacity: " + capacity);
            System.out.println("[INFO] - IP: " + addr);
            System.out.println("[INFO] - Port: " + port);
            System.out.println("[INFO] - Load Balancer IP: " + lbHost);
            System.out.println("[INFO] - Load Balancer Port: " + lbPort);
            System.out.println("===============================================================================");

            Node node = new Node(name, capacity, port, lbHost, lbPort);
            node.start();
        } else {
            System.err.println("[ERROR] Invalid arguments supplied! Usage: java node <name> <capacity> <port> <load balancer host address> <load balancer port>");
        }
    }
}