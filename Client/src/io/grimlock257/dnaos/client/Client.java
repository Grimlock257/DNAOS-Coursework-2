package io.grimlock257.dnaos.client;

import io.grimlock257.dnaos.client.job.Job;
import io.grimlock257.dnaos.client.managers.JobManager;
import io.grimlock257.dnaos.client.managers.MessageManager;
import io.grimlock257.dnaos.client.message.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Represent the Client in the Client project
 * This class represents the client and all it's functionality
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Client {
    // Constants storing indexes for information within a message
    private final int I_MESSAGE_TYPE = 0;
    private final int I_JOB_DURATION = 1;

    private boolean connected = false;
    private boolean hasSentRegister = false; // TODO: Better method of implementing this

    private int clientPort;
    private String lbHost;
    private int lbPort;
    private DatagramSocket socket;

    private MessageManager messageManager;
    private JobManager jobManager;

    /**
     * Create a new client instance
     *
     * @param clientPort The port for the client to communicate through
     * @param lbHost     The IP address of the load balancer
     * @param lbPort     The port of the load balancer
     */
    public Client(int clientPort, String lbHost, int lbPort) {
        this.clientPort = clientPort;
        this.lbHost = lbHost;
        this.lbPort = lbPort;

        start();
    }

    /**
     * Try to open the DatagramSocket, if successful create the managers and begin the main loop
     */
    private void start() {
        try {
            socket = new DatagramSocket(clientPort);

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
        // Temporarily get input from the keyboard until the initiator/client program is complete
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader keyboard = new BufferedReader(input);
        InetAddress addr = InetAddress.getByName(lbHost);

        while (true) {
            if (connected) {
                System.out.print("> ");

                try {
                    int jobDuration = Integer.parseInt(keyboard.readLine()); // TODO: Causing block
                    jobManager.addJob(new Job(jobDuration));

                    messageManager.send(MessageType.NEW_JOB.toString() + "," + jobDuration, addr, lbPort);
                } catch (NumberFormatException e) {
                    System.out.println("[INPUT ERROR] Please enter an integer only");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (!hasSentRegister) {
                System.out.println("Connecting to load balancer...");
                messageManager.send(MessageType.CLIENT_REGISTER.toString() + "," + InetAddress.getLocalHost().getHostAddress() + "," + this.clientPort, addr, lbPort);
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
            case COMPLETE_JOB:
                System.out.println("[INFO] processMessage received 'COMPLETE_JOB'");

                // TODO: Retrieve the Job that has been completed from the message and update JobStatus of the Job within the JobManager

                break;
            default:
                System.err.println("[ERROR] processMessage received: '" + message + "' (unknown argument)");
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
