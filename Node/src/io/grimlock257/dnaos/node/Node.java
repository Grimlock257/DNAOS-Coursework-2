package io.grimlock257.dnaos.node;

import io.grimlock257.dnaos.node.managers.MessageManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Represent the Node in the Node project
 * This class represents a node and all it's functionality
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Node {
    private boolean connected = false;

    private int nodePort;
    private String lbHost;
    private int lbPort;
    private DatagramSocket socket;

    private MessageManager messageManager;

    /**
     * Create a new node instance
     * @param nodePort The port for the node to communicate through
     * @param lbPort The port of the load balancer
     */
    public Node(int nodePort, String lbHost, int lbPort) {
        this.nodePort = nodePort;
        this.lbHost = lbHost;
        this.lbPort = lbPort;

        start();
    }

    /**
     * Try to open the DatagramSocket, if successful begin the main loop
     */
    private void start() {
        try {
            socket = new DatagramSocket(nodePort);
            messageManager = new MessageManager(socket);

            // System.out.println("[INFO] addr: " + addr.toString());

            // try send register, once received confirmation of register, begin the main loop?
            loop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Check for incoming packets, and send any packets to be processed
     *
     * @throws IOException When keyboard input can not be retrieved
     */
    private void loop() throws IOException {
        // Temporarily get input from the keyboard until the initiator/client program is complete
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader keyboard = new BufferedReader(input);
        InetAddress addr = InetAddress.getByName(lbHost);

        while (true) {
            if (connected) {
                System.out.print("> ");
                String message = keyboard.readLine();

                messageManager.send(message, addr, lbPort);
            } else {
                System.out.println("Connecting to load balancer...");
                messageManager.send("REGISTER", addr, lbPort);
            }

            processMessage(messageManager.receive());
        }
    }

    public void processMessage(String message) throws IOException {
        // System.out.println("[DEBUG] Received message: " + message);
        String[] args = message.split(",");

        // These messages are just for testing at the moment
        switch (getValidArg(args, 0)) {
            case "CONFIRM":
                System.out.println("[INFO] processMessage received 'CONFIRM'");
                connected = true;
                break;
            default:
                System.out.println("[ERROR] processMessage received: '" + message + "' (unknown argument)");
        }
    }

    // TODO: toUpperCase()?
    public String getValidArg(String[] args, int pos) {
        if (args.length > pos) {
            return (args[pos] != null) ? args[pos].toUpperCase().trim() : "";
        } else {
            return "";
        }
    }
}
