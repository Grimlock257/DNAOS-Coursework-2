package io.grimlock257.dnaos.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

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

            // Display information about the Load Balancer
            System.out.println("[INFO] LoadBalancer online");
            System.out.println("[INFO] LoadBalancer details:");
            try {
                System.out.println("[INFO] - IP: " + InetAddress.getByName("localhost"));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            System.out.println("[INFO] - Port: " + port);
            System.out.println("===============================================================================\n\n");

            new LoadBalancer(port);
        } else {
            System.err.println("Invalid arguments supplied! Usage: <port>");
        }
    }
}