package io.grimlock257.dnaos.client;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Main class of Client project
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Main {
    /**
     * Entry port for the program.
     *
     * Takes in command line arguments and uses them to initialise a nonstatic instance of the Client
     *
     * @param args The command line arguments supplied
     */
    public static void main(String[] args) {
        if (args.length == 3) {
            // Get parameters from the supplied command line arguments
            int port = Integer.parseInt(args[0]);
            String lbHost = args[1];
            int lbPort = Integer.parseInt(args[2]);

            InetAddress addr = null;
            try {
                addr = InetAddress.getByName("localhost");
            } catch (UnknownHostException e) {
                System.err.println("[ERROR] UnknownHostException thrown, exiting...");
                e.printStackTrace();
                System.exit(1);
            }

            // Display information about the Client
            System.out.println("[INFO] Client online");
            System.out.println("[INFO] Client details:");
            System.out.println("[INFO] - IP: " + addr);
            System.out.println("[INFO] - Port: " + port);
            System.out.println("[INFO] - Load Balancer IP: " + lbHost);
            System.out.println("[INFO] - Load Balancer Port: " + lbPort);
            System.out.println("===============================================================================");

            Client client = new Client(port, lbHost, lbPort);
            client.start();
        } else {
            System.err.println("[ERROR] Invalid arguments supplied! Usage: java client <port> <load balancer host address> <load balancer port>");
        }
    }
}