package io.grimlock257.dnaos.server;

import io.grimlock257.dnaos.server.job.Job;
import io.grimlock257.dnaos.server.job.JobStatus;
import io.grimlock257.dnaos.server.managers.JobManager;
import io.grimlock257.dnaos.server.managers.MessageManager;
import io.grimlock257.dnaos.server.managers.NodeManager;
import io.grimlock257.dnaos.server.message.MessageType;
import io.grimlock257.dnaos.server.node.Node;

import java.io.IOException;
import java.net.BindException;
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
    private final int I_JOB_NAME = 1;
    private final int I_JOB_DURATION = 2;
    private final int I_COMPLETE_JOB_NAME = 1;
    private final int I_CANCEL_REQUEST_JOB_NAME = 1;
    private final int I_CANCELLED_JOB_NAME = 1;
    private final int I_SHUTDOWN_NODE_NAME = 1;

    // TODO: Temp - need Client representation class
    // Information about the connected client
    private String clientIP;
    private int clientPort;
    private InetAddress clientAddr;

    // Information about the load balancer
    private int port = 0;

    // The socket for the load balancer to communicate though
    private DatagramSocket socket;

    // Managers that the load balancer uses
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
            messageManager.init(socket);
            nodeManager = NodeManager.getInstance();
            jobManager = JobManager.getInstance();

            // Have setup first?
            loop();
            // TODO: MessageManager.receive thread is still running - causes continuous socket closed exceptions
        } catch (BindException e) {
            if (e.getMessage().toLowerCase().contains("address already in use")) {
                System.err.println("[ERROR] Port " + port + " is already in use, please select another port via the command line arguments");
                System.err.println("[ERROR] Usage: java loadbalancer <port>");
            } else {
                System.err.println("[ERROR] Unhandled BindException error thrown");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Unhandled Exception thrown");
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (NullPointerException ignored) {
                // We'll get a NullPointException (as the finally clause always runs) if the BindException
                // was thrown - as this means the socket couldn't be created, so there is no socket object
            }
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

            if (nextMessage != null) {
                processMessage(nextMessage);
            }

            // Allocate a job (if available)
            // TODO: Thread this?
            Node freestNode = nodeManager.getFreestNode();

            if (freestNode != null) {
                Job nextJob = jobManager.getNextJob();

                if (nextJob != null) {
                    System.out.println("===============================================================================");
                    System.out.println("[INFO] Allocating job '" + nextJob.getName() + "'...\n");

                    jobManager.allocateJob(nextJob, freestNode);
                    System.out.println("[INFO] Current job list:\n" + jobManager.toString());

                    messageManager.send(MessageType.NEW_JOB.toString() + "," + nextJob.getName() + "," + nextJob.getDuration(), freestNode.getAddr(), freestNode.getPort());

                    System.out.println("\n[INFO] Node '" + freestNode.getName() + "' utilization is now " + freestNode.calcUsage() + "% (max capacity is " + freestNode.getCapacity() + ")");
                }
            }
        }
    }

    /**
     * Take in a message a string, analyse it and perform the appropriate action based on the contents
     *
     * @param message The message to analyse
     *
     * @throws IOException When InetAddress cannot be resolved from the supplied IP address
     */
    private void processMessage(String message) throws IOException {
        String[] args = message.split(",");

        // Nice formatting
        System.out.println("===============================================================================");

        // Perform appropriate action depending on the message type
        switch (getValidMessageType(args)) {
            case LB_SHUTDOWN:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                System.out.println("[INFO] Sending shutdown message to nodes...\n");
                nodeManager.shutdownAllNodes();

                System.out.println("[INFO] Shutting down...");
                System.exit(0);

                break;
            case NODE_SHUTDOWN_SPECIFIC:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String shutdownNodeName = getValidStringArg(args, I_SHUTDOWN_NODE_NAME);

                if (shutdownNodeName == null) {
                    System.out.println("[ERROR] Node was not shutdown, some of the supplied information was invalid");
                } else {
                    Node shutdownNode = nodeManager.findByName(shutdownNodeName);

                    if (shutdownNode == null) {
                        System.out.println("[ERROR] Node was not shutdown as no node with name '" + shutdownNodeName + "' was found");
                    } else {
                        nodeManager.shutdownNode(shutdownNode);

                        System.out.println("[INFO] The following node has been removed: " + shutdownNode.toString() + "\n");
                        System.out.println("[INFO] Current nodes:\n" + nodeManager.toString());
                    }
                }

                break;
            case CLIENT_REGISTER:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                clientIP = getValidStringArg(args, I_CLIENT_IP);
                clientPort = getValidIntArg(args, I_CLIENT_PORT);

                if (clientIP == null || clientPort == -1) {
                    System.out.println("[ERROR] Client was not added, some of the supplied information was invalid");
                } else {
                    clientAddr = InetAddress.getByName(clientIP);

                    System.out.println("[INFO] New client added: IP: " + clientIP + ", Port: " + clientPort);

                    messageManager.send(MessageType.REGISTER_CONFIRM.toString(), clientAddr, clientPort);
                }

                break;
            case NODE_REGISTER:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String nodeIP = getValidStringArg(args, I_NODE_IP);
                int nodePort = getValidIntArg(args, I_NODE_PORT);
                String nodeName = getValidStringArg(args, I_NODE_NAME);
                int nodeCap = getValidIntArg(args, I_NODE_CAP);

                if (nodeIP == null || nodePort == -1 || nodeName == null || nodeCap == -1) {
                    System.out.println("[ERROR] Node was not added, some of the supplied information was invalid");
                } else {
                    InetAddress nodeAddr = InetAddress.getByName(nodeIP);

                    Node newNode = new Node(nodePort, nodeAddr, nodeCap, nodeName);
                    nodeManager.addNode(newNode);

                    System.out.println("[INFO] New node added: " + newNode.toString() + "\n");

                    messageManager.send(MessageType.REGISTER_CONFIRM.toString(), nodeAddr, nodePort);

                    System.out.println("[INFO] Current nodes:\n" + nodeManager.toString());
                }

                break;
            case NEW_JOB:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String jobName = getValidStringArg(args, I_JOB_NAME);
                int jobDuration = getValidIntArg(args, I_JOB_DURATION);

                if (jobName == null || jobDuration == -1) {
                    System.out.println("[ERROR] Job was not added, some of the supplied information was invalid");
                } else {
                    Job newJob = new Job(jobName, jobDuration);

                    jobManager.addJob(newJob);

                    System.out.println("[INFO] New job added: " + newJob.toString() + "\n");
                    System.out.println("[INFO] Current job list:\n" + jobManager.toString());
                }

                break;
            case COMPLETE_JOB:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String completedJobName = getValidStringArg(args, I_COMPLETE_JOB_NAME);

                if (completedJobName == null) {
                    System.out.println("[ERROR] Job was not altered, some of the supplied information was invalid");
                } else {
                    Job completedJob = jobManager.findByName(completedJobName);

                    if (completedJob == null) {
                        System.out.println("[ERROR] Job was not marked as complete or sent to the client as no job with name '" + completedJobName + "' was found");
                    } else {
                        messageManager.send(MessageType.COMPLETE_JOB.toString() + "," + completedJobName, clientAddr, clientPort);

                        jobManager.updateJobStatus(completedJob, JobStatus.SENT);

                        System.out.println("[INFO] Job '" + completedJob.getName() + "' is complete and sent to the client\n");
                        System.out.println("[INFO] Current job list:\n" + jobManager.toString());
                    }
                }

                break;
            case CANCEL_JOB_REQUEST:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String cancelJobName = getValidStringArg(args, I_CANCEL_REQUEST_JOB_NAME);

                if (cancelJobName == null) {
                    System.out.println("[ERROR] Job cancel request was not issued, some of the supplied information was invalid");
                } else {
                    Job cancelJob = jobManager.findByName(cancelJobName);

                    if (cancelJob == null) {
                        System.out.println("[ERROR] Job cancel request was not issued as no job with name '" + cancelJobName + "' was found");
                    } else {
                        Node jobNode = jobManager.getNodeByJob(cancelJob.getName());

                        if (jobNode == null) {
                            System.out.println("[ERROR] The node allocated to the job could not be found");
                        } else {
                            messageManager.send(MessageType.CANCEL_JOB_REQUEST.toString() + "," + cancelJobName, jobNode.getAddr(), jobNode.getPort());

                            jobManager.updateJobStatus(cancelJob, JobStatus.REQUESTED_CANCEL);

                            System.out.println("[INFO] Job '" + cancelJob.getName() + "' has been requested to be cancelled\n");
                            System.out.println("[INFO] Current job list:\n" + jobManager.toString());
                        }
                    }
                }

                break;
            case CANCEL_JOB_CONFIRM:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String cancelledJobName = getValidStringArg(args, I_CANCELLED_JOB_NAME);

                if (cancelledJobName == null) {
                    System.out.println("[ERROR] Job was not altered, some of the supplied information was invalid");
                } else {
                    Job cancelledJob = jobManager.findByName(cancelledJobName);

                    if (cancelledJob == null) {
                        System.out.println("[ERROR] Job was not marked as cancelled or sent to the client as no job with name '" + cancelledJobName + "' was found");
                    } else {
                        messageManager.send(MessageType.CANCEL_JOB_CONFIRM.toString() + "," + cancelledJobName, clientAddr, clientPort);

                        jobManager.updateJobStatus(cancelledJob, JobStatus.CANCELLED);

                        System.out.println("[INFO] Job '" + cancelledJob.getName() + "' cancelled\n");
                        System.out.println("[INFO] Current job list:\n" + jobManager.toString());
                    }
                }

                break;
            case UNKNOWN:
            default:
                System.err.println("[ERROR] Received: '" + message + "', unknown argument");
        }
    }

    /**
     * Validate the MessageType of the message
     *
     * @param args The message broken up into elements based on commas
     *
     * @return The MessageType (UNKNOWN is non valid)
     */
    private MessageType getValidMessageType(String[] args) {
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
     * Validate a string argument within the message at the specified position
     *
     * @param args The message broken up into elements based on commas
     * @param pos  The element to validate
     *
     * @return The trimmed string or "" if invalid or null
     */
    private String getValidStringArg(String[] args, int pos) {
        if (args.length > pos) {
            return (args[pos] != null || !args[pos].trim().equals("")) ? args[pos].trim() : null;
        } else {
            return null;
        }
    }

    /**
     * Validate an integer argument with the message at the specified position
     *
     * @param args The message broken up into elements based on commas
     * @param pos  The element to validate
     *
     * @return The parsed integer or -1 if invalid or null
     */
    private int getValidIntArg(String[] args, int pos) {
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
