package io.grimlock257.dnaos.server;

import io.grimlock257.dnaos.server.job.Job;
import io.grimlock257.dnaos.server.managers.JobManager;
import io.grimlock257.dnaos.server.managers.MessageManager;
import io.grimlock257.dnaos.server.managers.NodeManager;
import io.grimlock257.dnaos.server.message.MessageType;
import io.grimlock257.dnaos.server.node.Node;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * This class represents the load balancer and all it's functionality
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class LoadBalancer {
    // Constants storing indexes for information within a message
    private final int I_MESSAGE_TYPE = 0;
    private final int I_CLIENT_IP = 1;
    private final int I_CLIENT_PORT = 2;
    private final int I_NODE_IP = 1;
    private final int I_NODE_PORT = 2;
    private final int I_NODE_NAME = 3;
    private final int I_NODE_CAP = 4;
    private final int I_JOB_DURATION = 1;

    // TODO: Temp - need Client representation class
    private String clientIP;
    private int clientPort;

    private int port = 0;
    private DatagramSocket socket;

    private MessageManager messageManager;
    private NodeManager nodeManager;
    private JobManager jobManager;

    /**
     * Create a new load balancer instance
     *
     * @param port The port for the LoadBalancer to operate on
     */
    public LoadBalancer(int port) {
        this.port = port;

        start();
    }

    /**
     * Try to open the DatagramSocket, if successful create the managers and begin the main loop
     */
    private void start() {
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(0);

            messageManager = MessageManager.getInstance();
            MessageManager.getInstance().init(socket);
            nodeManager = NodeManager.getInstance();
            jobManager = JobManager.getInstance();

            loop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Try / Catch?
            socket.close();
        }
    }

    /**
     * Check for incoming packets, and send any packets to be processed
     *
     * @throws IOException When a packet cannot be successfully retrieved on the socket
     */
    private void loop() throws IOException {
        System.out.println("Listening for messages...");

        while (true) {
            // Process messages (if available)
            String nextMessage = messageManager.getNextMessage();

            if (nextMessage != "") {
                processMessage(nextMessage);
            }

            // Allocate a job (if available)
            // TODO: Thread this?
            Node freestNode = nodeManager.getFreestNode();

            if (freestNode != null) {
                Job nextJob = jobManager.getNextJob();

                if (nextJob != null) {
                    System.out.println("===============================================================================");
                    System.out.println("[INFO] Job allocation taking place for '" + nextJob.toString() + "' to node '" + freestNode.getName() + "'");

                    jobManager.allocateJob(nextJob, freestNode);

                    messageManager.send(MessageType.NEW_JOB.toString() + "," + nextJob.getDuration(), freestNode.getAddr(), freestNode.getPort());

                    System.out.println("[INFO] Node utilization is now " + freestNode.calcUsage() + "% (max capacity is " + freestNode.getCapacity() + ")");
                }
            }
        }
    }

    /**
     * Take in a message a string, analyse it and perform the appropriate action based on the contents
     *
     * @param message The message to analyse
     * @throws IOException
     */
    // Message structure: command, args0, args1, ..., args2
    // E.g: REGISTER, 192.168.1.15
    // TODO: Enum or Message packaging containing Message subclasses (i.e MessageRegister, MessageResign etc.)
    // Messages: Node Register, Node Resign, New Job (client -> lb), New Job (lb -> node), Complete job (node -> lb), Complete Job (lb -> client), LB shutdown, Node shutdown
    public void processMessage(String message) throws IOException {
        // System.out.println("[DEBUG] Received message: " + message);
        String[] args = message.split(",");

        // These messages are just for testing at the moment
        switch (getValidMessageType(args)) {
            case LB_SHUTDOWN:
                System.out.println("===============================================================================");
                System.out.println("[INFO] processMessage received '" + message + "'");
                System.exit(0);
                break;
            case CLIENT_REGISTER:
                System.out.println("===============================================================================");
                System.out.println("[INFO] processMessage received '" + message + "'");

                clientIP = getValidStringArg(args, I_CLIENT_IP);
                clientPort = getValidIntArg(args, I_CLIENT_PORT);
                InetAddress clientAddr = InetAddress.getByName(clientIP);

                messageManager.send(MessageType.REGISTER_CONFIRM.toString(), clientAddr, clientPort);
                break;
            case NODE_REGISTER:
                System.out.println("===============================================================================");
                System.out.println("[INFO] processMessage received '" + message + "'");

                String nodeIP = getValidStringArg(args, I_NODE_IP);
                int nodePort = getValidIntArg(args, I_NODE_PORT);
                String nodeName = getValidStringArg(args, I_NODE_NAME);
                int nodeCap = getValidIntArg(args, I_NODE_CAP);
                InetAddress nodeAddr = InetAddress.getByName(nodeIP);

                nodeManager.addNode(new Node(nodePort, nodeAddr, nodeCap, nodeName));

                messageManager.send(MessageType.REGISTER_CONFIRM.toString(), nodeAddr, nodePort);

                // TODO: Remove - temporary testing
                // messageManager.send(MessageType.NEW_JOB.toString() + ",10", nodeAddr, nodePort);
                // messageManager.send(MessageType.NEW_JOB.toString() + ",10", nodeAddr, nodePort);
                break;
            case NEW_JOB:
                System.out.println("===============================================================================");
                System.out.println("[INFO] processMessage received '" + message + "'");

                int jobDuration = getValidIntArg(args, I_JOB_DURATION);

                jobManager.addJob(new Job(jobDuration));

                break;
            case COMPLETE_JOB:
                System.out.println("===============================================================================");
                System.out.println("[INFO] processMessage received '" + message + "'");
                break;
            default:
                System.out.println("===============================================================================");
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
