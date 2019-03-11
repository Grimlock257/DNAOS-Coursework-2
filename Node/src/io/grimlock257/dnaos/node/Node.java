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
    private int port;
    private DatagramSocket socket;

    private MessageManager messageManager;

    public Node(int port) {
        this.port = port;

        start();
    }

    /**
     * Try to open the DatagramSocket, if successful begin the main loop
     */
    private void start() {
        try {
            socket = new DatagramSocket(5000);
            messageManager = new MessageManager(socket);

            // System.out.println("[INFO] addr: " + addr.toString());

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
        InetAddress addr = InetAddress.getByName("localhost");

        while (true) {
            System.out.print("> ");
            String message = keyboard.readLine();

            messageManager.send(message, addr, 4000);
        }
    }
}
