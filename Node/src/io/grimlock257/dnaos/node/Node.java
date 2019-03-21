package io.grimlock257.dnaos.node;

import io.grimlock257.dnaos.node.job.Job;
import io.grimlock257.dnaos.node.managers.JobManager;
import io.grimlock257.dnaos.node.managers.MessageManager;
import io.grimlock257.dnaos.node.message.MessageType;

import java.io.IOException;
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
    // Constants storing indexes for information within a message
    private final int I_MESSAGE_TYPE = 0;
    private final int I_JOB_DURATION = 1;

    private boolean connected = false;
    private boolean hasSentRegister = false; // TODO: Better method of implementing this

    private String name;
    private int capacity;
    private int nodePort;
    private String lbHost;
    private int lbPort;
    private DatagramSocket socket;

    private MessageManager messageManager;
    private JobManager jobManager;

    /**
     * Create a new node instance
     *
     * @param name     The name of the name
     * @param capacity The maximum amount of jobs the node can handle at a time
     * @param nodePort The port for the node to communicate through
     * @param lbHost   The IP address of the load balancer
     * @param lbPort   The port of the load balancer
     */
    public Node(String name, int capacity, int nodePort, String lbHost, int lbPort) {
        this.name = name;
        this.capacity = capacity;
        this.nodePort = nodePort;
        this.lbHost = lbHost;
        this.lbPort = lbPort;

        start();
    }

    /**
     * Try to open the DatagramSocket, if successful create the managers and begin the main loop
     */
    private void start() {
        try {
            socket = new DatagramSocket(nodePort);

            messageManager = new MessageManager(socket);
            jobManager = new JobManager();

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
        InetAddress addr = InetAddress.getByName(lbHost);

        while (true) {
            if (!hasSentRegister) {
                System.out.println("Connecting to load balancer...");
                messageManager.send(MessageType.NODE_REGISTER.toString() + "," + InetAddress.getLocalHost().getHostAddress() + "," + this.nodePort + "," + this.name + "," + this.capacity, addr, lbPort);
                hasSentRegister = true;
            }

            String nextMessage = messageManager.getNextMessage();

            // TODO: Related to TODO: Causing block - thread the processMessage();
            if (nextMessage != "") {
                processMessage(nextMessage);
            }
        }
    }

    /**
     * Take in a message a string, analyse it and perform the appropriate action based on the contents
     *
     * @param message The message to analyse
     * @throws IOException
     */
    public void processMessage(String message) throws IOException {
        // System.out.println("[DEBUG] Received message: " + message);
        String[] args = message.split(",");

        // These messages are just for testing at the moment
        switch (getValidMessageType(args)) {
            case REGISTER_CONFIRM:
                System.out.println("[INFO] processMessage received 'REGISTER_CONFIRM'");
                connected = true;
                break;
            case NEW_JOB:
                System.out.println("[INFO] processMessage received 'NEW_JOB'");

                int jobDuration = getValidIntArg(args, I_JOB_DURATION);

                jobManager.addJob(new Job(jobDuration));

                break;
            default:
                System.out.println("[ERROR] processMessage received: '" + message + "' (unknown argument)");
        }
    }

    /**
     * Validate the MessageType of the message
     *
     * @param args The message broken up into elements based on commas
     * @return The MessageType (UNKNOWN is non valid)
     */
    public MessageType getValidMessageType(String[] args) {
        if (args.length > 0 && args[I_MESSAGE_TYPE] != null) {
            try {
                return MessageType.valueOf(args[I_MESSAGE_TYPE].trim());
            } catch (IllegalArgumentException e) {
                return MessageType.UNKNOWN;
            }
        } else {
            return MessageType.UNKNOWN;
        }
    }

    /**
     * Validate a string argument within the message at the specified pos
     *
     * @param args The message broken up into elements based on commas
     * @param pos  The element to validate
     * @return The trimmed string or "" if invalid or null
     */
    // TODO: toUpperCase()? UPDATE
    public String getValidStringArg(String[] args, int pos) {
        if (args.length > pos) {
            return (args[pos] != null) ? args[pos].trim() : "";
        } else {
            return "";
        }
    }

    /**
     * Validate an integer argument with the message at the specified pos
     *
     * @param args The message broken up into elements based on commas
     * @param pos  The element to validate
     * @return The parsed integer or -1 if invalid or null
     */
    // TODO: Handle -1 outputs from this method
    public int getValidIntArg(String[] args, int pos) {
        if (args.length > pos && args[pos] != null) {
            try {
                return Integer.parseInt(args[pos].trim());
            } catch (NumberFormatException e) {
                return -1;
            }
        } else {
            return -1;
        }
    }
}
