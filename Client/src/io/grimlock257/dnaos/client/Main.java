package io.grimlock257.dnaos.client;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Main class of Client project
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Main {
    /**
     * Entry port for the program.
     * <p>
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

            System.out.println("[INFO] Client online");
            System.out.println("[INFO] Client details:");
            try {
                System.out.println("[INFO] - IP: " + InetAddress.getByName("localhost"));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            System.out.println("[INFO] - Port: " + port);
            System.out.println("[INFO] - Load Balancer IP: " + lbHost);
            System.out.println("[INFO] - Load Balancer Port: " + lbPort);
            System.out.println("===============================================================================\n\n");


            new Client(port, lbHost, lbPort);
        } else {
            System.err.println("Invalid arguments supplied! Usage: <port> <load balancer host address> <load balancer port>");
        }
    }
}