package io.grimlock257.dnaos.server.managers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Message Manager for Load Balancer project
 * This class handles the sending and receiving of messages via UDP packets
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class MessageManager {
    private static MessageManager instance = null;

    private DatagramSocket socket;

    private LinkedList<HashMap<String, Boolean>> messages;
    private final Object messageLock = new Object();

    /**
     * MessageManager constructor
     */
    private MessageManager() {
        this.messages = new LinkedList<>();

        this.receive();
    }

    /**
     * Get the instance of the MessageManager singleton
     *
     * @return The instance of the MessageManager
     */
    public static MessageManager getInstance() {
        if (instance == null) {
            instance = new MessageManager();
        }

        return instance;
    }

    /**
     * Setup the socket for the MessageManager to use
     *
     * @param socket The socket to use when sending and receiving UDP packets
     */
    public void init(DatagramSocket socket) {
        this.socket = socket;
    }

    /**
     * Send a message as a UDP packet
     *
     * @param message The message to be sent
     * @param address The address to send the packet to
     * @param port    The port to sent the packet to
     */
    public void send(String message, InetAddress address, int port) {
        try {
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("[ERROR] The packet could not be sent due to IOException");
        }
    }

    /**
     * Creates a thread that receives messages as incoming UDP packets
     * If a packet is received, add the contents of the packet to the messages LinkedList
     */
    private void receive() {
        // Create a new thread to receive the incoming packet so that Load Balancer isn't blocked completely while waiting for a message
        Thread receive = new Thread("load_balancer_receive_thread") {
            public void run() {
                while (true) {
                    // Byte buffer to store the message
                    byte[] buffer = new byte[2048];

                    // Try receive the packet from the socket into the byte array
                    try {
                        socket.receive(new DatagramPacket(buffer, buffer.length));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Store the message into the LinkedList of messages, assuming the message length isn't 0
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

                    return message.keySet().toArray()[0].toString();
                }
            }
        }

        return null;
    }
}
