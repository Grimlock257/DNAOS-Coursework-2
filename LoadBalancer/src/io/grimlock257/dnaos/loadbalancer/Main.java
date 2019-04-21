package io.grimlock257.dnaos.loadbalancer;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Main class of Load Balancer project
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Main {
    /**
     * Entry port for the program.
     *
     * Takes in command line arguments and uses them to initialise a nonstatic instance of the Load Balancer
     *
     * @param args The command line arguments supplied
     */
    public static void main(String[] args) {
        if (args.length == 2) {
            // Get parameters from the supplied command line arguments
            int port = Integer.parseInt(args[0]);

            AllocationMethod allocationMethod = null;
            try {
                allocationMethod = AllocationMethod.valueOf(args[1].trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("[ERROR] Invalid allocation method entered. Enter either WEIGHTED or NON_WEIGHTED, exiting...");
                System.exit(1);
            }

            InetAddress addr = null;
            try {
                addr = InetAddress.getByName("localhost");
            } catch (UnknownHostException e) {
                System.err.println("[ERROR] UnknownHostException thrown, exiting...");
                e.printStackTrace();
                System.exit(1);
            }

            // Display information about the Load Balancer
            System.out.println("[INFO] Load Balancer online");
            System.out.println("[INFO] Load Balancer details:");
            System.out.println("[INFO] - IP: " + addr);
            System.out.println("[INFO] - Port: " + port);
            System.out.println("[INFO] - Allocation Method: " + allocationMethod.toString());
            System.out.println("===============================================================================");

            LoadBalancer loadBalancer = new LoadBalancer(port, allocationMethod);
            loadBalancer.start();
        } else {
            System.err.println("[ERROR] Invalid arguments supplied! Usage: java loadbalancer <port> <WEIGHTED | NON_WEIGHTED>");
        }
    }
}