package io.grimlock257.dnaos.client;

import io.grimlock257.dnaos.client.job.Job;
import io.grimlock257.dnaos.client.job.JobStatus;
import io.grimlock257.dnaos.client.managers.JobManager;
import io.grimlock257.dnaos.client.managers.MessageManager;
import io.grimlock257.dnaos.client.message.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Represent the Client in the Client project
 * This class represents the client and all it's functionality
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Client {
    // Constants storing indexes for information within a message
    private final int I_MESSAGE_TYPE = 0;
    private final int I_COMPLETE_JOB_NAME = 1;

    private boolean connected = false;
    private boolean hasSentRegister = false; // TODO: Better method of implementing this
    private boolean hasBeganUserInputThread = false; // TODO: Better method of implementing this

    // Information about the client
    private int port;

    // Information about the load balancer
    private String lbHost;
    private int lbPort;
    private InetAddress lbAddr;

    // The socket for the client to communicate through
    private DatagramSocket socket;

    // Managers that the client uses
    private MessageManager messageManager;
    private JobManager jobManager;

    /**
     * Create a new client instance
     *
     * @param port   The port for the client to communicate through
     * @param lbHost The IP address of the load balancer
     * @param lbPort The port of the load balancer
     */
    public Client(int port, String lbHost, int lbPort) {
        this.port = port;

        this.lbHost = lbHost;
        this.lbPort = lbPort;

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
            jobManager = JobManager.getInstance();

            // Have setup first?
            loop();
        } catch (BindException e) {
            if (e.getMessage().toLowerCase().contains("address already in use")) {
                System.err.println("[ERROR] Port " + port + " is already in use, please select another port via the command line arguments");
                System.err.println("[ERROR] Usage: java client <port> <load balancer host address> <load balancer port>");
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
     * @throws IOException When keyboard input can not be retrieved
     */
    private void loop() throws IOException {
        lbAddr = InetAddress.getByName(lbHost);

        while (true) {
            if (connected && !hasBeganUserInputThread) {
                // A new thread is created for each job to be ran
                Thread userInput = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Get input from the user forever
                        while (true) {
                            getUserInput();
                        }
                    }
                });

                userInput.start();

                hasBeganUserInputThread = true;
            } else if (!hasSentRegister) {
                System.out.println("Connecting to load balancer...");
                messageManager.send(MessageType.CLIENT_REGISTER.toString() + "," + InetAddress.getLocalHost().getHostAddress() + "," + this.port, lbAddr, lbPort);
                hasSentRegister = true;
            }

            // Process messages (if available)
            String nextMessage = messageManager.getNextMessage();

            if (nextMessage != null) {
                processMessage(nextMessage);
            }
        }
    }

    /**
     * Take in a message a string, analyse it and perform the appropriate action based on the contents
     *
     * @param message The message to analyse
     */
    private void processMessage(String message) {
        String[] args = message.split(",");

        // Nice formatting
        System.out.println("===============================================================================");

        // Perform appropriate action depending on the message type
        switch (getValidMessageType(args)) {
            case REGISTER_CONFIRM:
                System.out.println("[INFO] Received '" + message + "', processing...\n");
                System.out.println("[INFO] Successfully registered with the Load Balancer");
                connected = true;

                break;
            case COMPLETE_JOB:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String jobName = getValidStringArg(args, I_COMPLETE_JOB_NAME);

                if (jobName == null) {
                    System.out.println("[ERROR] Job was not altered, some of the supplied information was invalid");
                } else {
                    Job completedJob = jobManager.findByName(jobName);

                    jobManager.updateJobStatus(completedJob, JobStatus.COMPLETE);

                    System.out.println("[INFO] Job '" + completedJob.getName() + "' complete\n");
                    System.out.println("[INFO] Current job list:\n" + jobManager.toString());
                }

                break;
            case UNKNOWN:
            default:
                System.err.println("[ERROR] Received: '" + message + "', unknown argument");
        }
    }

    /**
     * Get input from the user search as the command they want to issue, and any required arguments
     */
    private void getUserInput() {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader keyboard = new BufferedReader(input);

        System.out.println("===============================================================================");

        // Create a new job
        try {
            // TODO: New job
            /*System.out.println("Enter job name:");
            System.out.print("> ");
            String jobName = keyboard.readLine(); // TODO: Validation // TODO: Duplicate check

            System.out.println("Enter job duration:");
            System.out.print("> ");
            int jobDuration = Integer.parseInt(keyboard.readLine());
            Job newJob = new Job(jobName, jobDuration);

            jobManager.addJob(newJob);

            messageManager.send(MessageType.NEW_JOB.toString() + "," + jobName + "," + jobDuration, lbAddr, lbPort);

            System.out.println("\n[INFO] New job added: " + newJob.toString() + "\n");

            System.out.println("[INFO] Current job list:\n" + jobManager.toString());*/

            // TODO: Shutdown LB + All nodes
            /*System.out.print("> ");
            keyboard.readLine(); // TODO: Validation // TODO: Duplicate check

            // TODO: Disconnect Client after sending LB_SHUTDOWN
            messageManager.send(MessageType.LB_SHUTDOWN.toString(), lbAddr, lbPort);*/

            // TODO: Shutdown specific node
            System.out.print("> ");
            String nodeToShutdown = keyboard.readLine(); // TODO: Validation // TODO: Duplicate check

            messageManager.send(MessageType.NODE_SHUTDOWN_SPECIFIC.toString() + "," + nodeToShutdown, lbAddr, lbPort);
        } catch (NumberFormatException e) {
            System.out.println("[INPUT ERROR] Please enter an integer only");
        } catch (Exception e) {
            e.printStackTrace();
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
