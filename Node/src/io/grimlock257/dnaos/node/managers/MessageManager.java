package io.grimlock257.dnaos.node.managers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Message Manager for Node project
 * This class handles the sending and receiving of messages via UDP packets
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class MessageManager {
    private DatagramSocket socket;

    private ArrayList<String> messages;
    private boolean newMessage = false;

    /**
     * MessageManager constructor
     *
     * @param socket The socket to use when sending and receiving UDP packets
     */
    public MessageManager(DatagramSocket socket) {
        this.socket = socket;
        this.messages = new ArrayList<String>();
    }

    /**
     * Send a message as a UDP packet
     *
     * @param message The message to be sent
     * @param address The address to send the packet to
     * @param port    The port to sent the packet to
     * @throws IOException When keyboard input can not be retrieved
     */
    public void send(String message, InetAddress address, int port) throws IOException {
        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, address, port);
        socket.send(packet);
    }

    /**
     * Receives a message from the UDP packet
     *
     * @return The received message as a string
     */
    public String receive() throws IOException {
        // Create a new thread to receive the incoming packet so that Node isn't blocked completely while waiting for a response
        Thread receive = new Thread("node_receive_thread") {
            public void run() {
                while (true) {
                    byte[] buffer = new byte[2048];

                    try {
                        socket.receive(new DatagramPacket(buffer, buffer.length));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // new String(buffer);

                    String message = new String(buffer);

                    if (message.length() > 0) {
                        messages.add(message);
                        newMessage = true;
                    }

                    System.out.println(messages.toString());
                }
            }
        };

        receive.start();

        if (newMessage) {
            newMessage = false;
            return messages.get(messages.size() - 1);
        } else {
            return "";
        }
    }
}
