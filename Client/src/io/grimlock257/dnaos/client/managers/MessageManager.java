package io.grimlock257.dnaos.client.managers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Message Manager for Client project
 * This class handles the sending and receiving of messages via UDP packets
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class MessageManager {
    private DatagramSocket socket;

    private LinkedList<HashMap<String, Boolean>> messages;
    private final Object messageLock = new Object();

    /**
     * MessageManager constructor
     *
     * @param socket The socket to use when sending and receiving UDP packets
     */
    public MessageManager(DatagramSocket socket) {
        this.socket = socket;
        this.messages = new LinkedList<>();

        this.receive();
    }

    /**
     * Send a message as a UDP packet
     *
     * @param message The message to be sent
     * @param address The address to send the packet to (the load balancer)
     * @param port    The port to sent the packet to (the load balancer)
     * @throws IOException When keyboard input can not be retrieved
     */
    public void send(String message, InetAddress address, int port) throws IOException {
        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, address, port);
        socket.send(packet);
    }

    /**
     * Creates a thread that receives a message from an incoming UDP packet
     * If a packet is received, add the contents of the packet to the messages LinkedList
     */
    private void receive() {
        // Create a new thread to receive the incoming packet so that Node isn't blocked completely while waiting for a response
        Thread receive = new Thread("client_receive_thread") {
            public void run() {
                while (true) {
                    byte[] buffer = new byte[2048];

                    try {
                        socket.receive(new DatagramPacket(buffer, buffer.length));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    String message = new String(buffer);

                    if (message.length() > 0) {
                        HashMap<String, Boolean> newMsg = new HashMap<String, Boolean>() {{
                            put(message, false);
                        }};

                        addMessage(newMsg);
                    }
                }
            }
        };

        receive.start();
    }

    /**
     * Add a message in the form of a HashMap<message, hasRead> to the messages LinkedList
     *
     * @param message The message to add to the messages LinkedList
     */
    private void addMessage(HashMap<String, Boolean> message) {
        // Lock the messagesLock so that only one thread may access the messages LinkedList at any one time
        synchronized (messageLock) {
            messages.add(message);

            System.err.println("ADD: " + this.getMessages().toString());
        }
    }

    /**
     * Get the LinkedList of messages
     *
     * @return The messages LinkedList containing all the messages currently stored
     */
    private LinkedList<HashMap<String, Boolean>> getMessages() {
        // Lock the messagesLock so that only one thread may access the messages LinkedList at any one time
        synchronized (messageLock) {
            return messages;
        }
    }

    /**
     * Fetch the next unread message from the messages LinkedList and mark it as read.
     *
     * @return The next unread message as a String
     */
    public String getNextMessage() {
        try {
            // Lock the messagesLock so that only one thread may access the messages LinkedList at any one time
            synchronized (messageLock) {
                for (int i = 0; i < getMessages().size(); i++) {
                    HashMap<String, Boolean> message = getMessages().get(i);

                    // Check if the message has been read or not
                    if (!message.get(message.keySet().toArray()[0])) {
                        // Update the corresponding HashMap
                        getMessages().set(i, new HashMap<String, Boolean>() {{
                            put(message.keySet().toArray()[0].toString(), true);
                        }});

                        System.err.println("ALT: " + this.getMessages().toString());

                        return message.keySet().toArray()[0].toString();
                    }
                }
            }
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }

        return "";
    }
}
