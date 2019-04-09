package io.grimlock257.dnaos.node;

import io.grimlock257.dnaos.node.job.Job;
import io.grimlock257.dnaos.node.job.JobStatus;
import io.grimlock257.dnaos.node.managers.JobManager;
import io.grimlock257.dnaos.node.managers.MessageManager;
import io.grimlock257.dnaos.node.message.MessageType;

import java.io.IOException;
import java.net.BindException;
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
    private final int I_JOB_NAME = 1;
    private final int I_JOB_DURATION = 2;

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

            messageManager = MessageManager.getInstance();
            messageManager.init(socket);
            jobManager = JobManager.getInstance();

            // System.out.println("[INFO] addr: " + addr.toString());

            // try send register, once received confirmation of register, begin the main loop?
            loop();
        } catch (BindException e) {
            if (e.getMessage().toLowerCase().contains("address already in use")) {
                System.err.println("[ERROR] Port " + nodePort + " is already in use, please select another port via the command line arguments");
                System.err.println("[ERROR] Usage: java node <name> <capacity> <port> <load balancer host address> <load balancer port>");
            } else {
                System.err.println("[ERROR]");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("[ERROR]");
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (NullPointerException ignored) {
            }
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

            // TODO: Capacity check? Shouldn't be required as Load Balancer shouldn't send more jobs than capacity
            // Process a job (if available)
            Job nextJob = jobManager.getNextJob();

            if (nextJob != null) {
                // A new thread is created for each job to be ran
                Thread jobProcessing = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Process the job
                        processJob(nextJob);

                        // Send the complete job back to the Load Balancer
                        try {
                            MessageManager.getInstance().send(MessageType.COMPLETE_JOB + "," + nextJob.getName(), addr, lbPort);
                            JobManager.getInstance().updateJobStatus(nextJob, JobStatus.SENT);
                        } catch (IOException e) {
                            System.err.println("[ERROR] An IO error occurred sending the complete job back to the Load Balancer");
                            e.printStackTrace();
                        }
                    }
                });

                jobProcessing.start();
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
                System.out.println("===============================================================================");
                System.out.println("[INFO] processMessage received '" + message + "'");
                connected = true;
                break;
            case NEW_JOB:
                System.out.println("===============================================================================");
                System.out.println("[INFO] processMessage received '" + message + "'");

                String jobName = getValidStringArg(args, I_JOB_NAME);
                int jobDuration = getValidIntArg(args, I_JOB_DURATION);

                jobManager.addJob(new Job(jobName, jobDuration));

                break;
            default:
                System.out.println("===============================================================================");
                System.err.println("[ERROR] processMessage received: '" + message + "' (unknown argument)");
        }
    }

    /**
     * Process the passed in job
     *
     * @param job The job to be processed
     */
    private void processJob(Job job) {
        System.out.println("===============================================================================");
        System.out.println("[INFO] Processing job '" + job.getName() + "'");

        // Try sleep for the job duration
        try {
            Thread.sleep(job.getDuration() * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Update the job status to COMPLETE and send the job back to the Load Balancer
        JobManager.getInstance().updateJobStatus(job, JobStatus.COMPLETE);

        System.out.println("===============================================================================");
        System.out.println("[INFO] Job '" + job.getName() + "' complete");
        System.out.println(JobManager.getInstance().toString());
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
