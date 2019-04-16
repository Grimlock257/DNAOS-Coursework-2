package io.grimlock257.dnaos.node;

import io.grimlock257.dnaos.node.job.Job;
import io.grimlock257.dnaos.node.job.JobStatus;
import io.grimlock257.dnaos.node.managers.JobManager;
import io.grimlock257.dnaos.node.managers.MessageManager;
import io.grimlock257.dnaos.node.message.MessageType;

import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Represent the Node in the Node project
 * This class represents a node and all it's functionality
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Node {
    // Constants storing indexes for information within a message
    private final int I_MESSAGE_TYPE = 0;
    private final int I_JOB_NAME = 1;
    private final int I_JOB_DURATION = 2;
    private final int I_CANCEL_REQUEST_JOB_NAME = 1;

    // How frequently to reattempt connection to the load balancer
    private final int RECONNECTION_TIME = 4 * 1000;

    private boolean connected = false;

    // Information about the node
    private String name;
    private int capacity;
    private int port;

    // Information about the load balancer
    private String lbHost;
    private int lbPort;
    private InetAddress lbAddr;

    // The socket for the node to communicate through
    private DatagramSocket socket;

    // Managers that the node uses
    private MessageManager messageManager;
    private JobManager jobManager;

    /**
     * Create a new node instance
     *
     * @param name     The name of the name
     * @param capacity The maximum amount of jobs the node can handle at a time
     * @param port     The port for the node to communicate through
     * @param lbHost   The IP address of the load balancer
     * @param lbPort   The port of the load balancer
     */
    public Node(String name, int capacity, int port, String lbHost, int lbPort) {
        this.name = name;
        this.capacity = capacity;
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

            lbAddr = InetAddress.getByName(lbHost);

            connect();
            loop();
        } catch (BindException e) {
            if (e.getMessage().toLowerCase().contains("address already in use")) {
                System.err.println("[ERROR] Port " + port + " is already in use, please select another port via the command line arguments");
                System.err.println("[ERROR] Usage: java node <name> <capacity> <port> <load balancer host address> <load balancer port>");
            } else {
                System.err.println("[ERROR] Unhandled BindException error thrown");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Unhandled Exception thrown");
            e.printStackTrace();
        } finally {
            try {
                messageManager.stop();

                socket.close();
            } catch (NullPointerException ignored) {
                // We'll get a NullPointException (as the finally clause always runs) if the BindException
                // was thrown - as this means the socket couldn't be created, so there is no socket object
            }
        }
    }

    /**
     * Set up the connection between the Node and the Load Balancer. Repeatedly send a register message
     * to the Load Balancer at a specified interval until a REGISTER_CONFIRM message is received
     */
    private void connect() {
        System.out.println("Connecting to load balancer...");

        // Create a Timer utilising a TimerTask to resend a register message at a specified time interval if not registered
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    // Send register message to the Load Balancer
                    messageManager.send(MessageType.NODE_REGISTER.toString() + "," + InetAddress.getLocalHost().getHostAddress() + "," + port + "," + name + "," + capacity, lbAddr, lbPort);
                } catch (Exception e) {
                    System.err.println("[ERROR] Unhandled Exception thrown");
                    e.printStackTrace();
                }
            }
        }, 0, RECONNECTION_TIME);

        // Keep retrieving next message until a REGISTER_CONFIRM is received, which sets boolean connected to true
        while (!connected) {
            // Process messages (if available)
            String nextMessage = messageManager.getNextMessage();

            if (nextMessage != null) {
                processMessage(nextMessage);
            }
        }

        // The while loop has ended which means we have successfully connected; terminate the timer
        timer.cancel();
    }

    /**
     * Check for incoming packets, and send any packets to be processed
     */
    private void loop() {
        while (true) {
            // Process messages (if available)
            String nextMessage = messageManager.getNextMessage();

            if (nextMessage != null) {
                processMessage(nextMessage);
            }

            // Process a job (if available)
            Job nextJob = jobManager.getNextJob();

            if (nextJob != null) {
                // A new thread is created for each job to be ran
                Thread jobProcessing = new Thread("job_processing_" + nextJob.getName().toLowerCase().replace(" ", "_")) {
                    @Override
                    public void run() {
                        // Process the job
                        if (processJob(nextJob)) {
                            // Send the complete job back to the Load Balancer
                            MessageManager.getInstance().send(MessageType.COMPLETE_JOB + "," + nextJob.getName(), lbAddr, lbPort);
                            JobManager.getInstance().updateJobStatus(nextJob, JobStatus.SENT);
                            System.out.println("");

                            System.out.println("[INFO] Job '" + nextJob.getName() + "' has been sent to the Load Balancer\n");
                            System.out.println("[INFO] Current job list:\n" + jobManager.toString());
                        }
                    }
                };

                jobProcessing.start();
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
            case REGISTER_FAILURE:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                if (!connected) {
                    System.out.println("[ERROR] Could not register with load balancer, duplicate node name or address / port combination present\n");
                    System.out.println("[INFO] Shutting down...");

                    System.exit(-1);
                } else {
                    System.out.println("[INFO] Ignoring as already registered with the load balancer...");
                }

                break;
            case NEW_JOB:
                if (!connected) {
                    System.err.println("[ERROR] Received '" + message + "', despite not being connected to a load balancer");
                    break;
                }

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
            case CANCEL_JOB_REQUEST:
                if (!connected) {
                    System.err.println("[ERROR] Received '" + message + "', despite not being connected to a load balancer");
                    break;
                }

                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String cancelJobName = getValidStringArg(args, I_CANCEL_REQUEST_JOB_NAME);

                if (cancelJobName == null) {
                    System.out.println("[ERROR] Job was not cancelled, some of the supplied information was invalid");
                } else {
                    Job cancelJob = jobManager.findByName(cancelJobName);

                    if (cancelJob == null) {
                        System.out.println("[ERROR] Job was not cancelled as no job with name '" + cancelJobName + "' was found");
                    } else {
                        System.out.println("[INFO] Previous job information for job '" + cancelJob.getName() + "':\n" + jobManager.jobToString(cancelJobName) + "\n");

                        System.out.println("[INFO] Alive threads before:");

                        // Iterate through the set of threads, printing the name of each
                        for (Thread thread : Thread.getAllStackTraces().keySet()) {
                            if (thread.isAlive()) {
                                System.out.println("Thread name: " + thread.getName());
                            }
                        }

                        // Iterate through the set of threads, looking for the thread that matches the cancelled job
                        for (Thread thread : Thread.getAllStackTraces().keySet()) {
                            if (thread.getName().equals("job_processing_" + cancelJob.getName().toLowerCase().replace(" ", "_"))) {
                                thread.interrupt();

                                System.out.println("\n[INFO] Job '" + cancelJob.getName() + "' has been cancelled");

                                break;
                            }
                        }

                        System.out.println("\n[INFO] Alive threads after:");

                        // Iterate through the set of threads, printing the name of each
                        for (Thread thread : Thread.getAllStackTraces().keySet()) {
                            if (thread.isAlive()) {
                                System.out.println("Thread name: " + thread.getName());
                            }
                        }

                        System.out.println("");
                        messageManager.send(MessageType.CANCEL_JOB_CONFIRM.toString() + "," + cancelJobName, lbAddr, lbPort);

                        jobManager.updateJobStatus(cancelJob, JobStatus.CANCELLED);

                        System.out.println("\n[INFO] Current job list:\n" + jobManager.toString());
                    }
                }

                break;
            case NODE_SHUTDOWN:
                if (!connected) {
                    System.err.println("[ERROR] Received '" + message + "', despite not being connected to a load balancer");
                    break;
                }

                System.out.println("[INFO] Received '" + message + "', processing...");
                System.out.println("[INFO] Shutting down...");
                System.exit(0);

                break;
            case UNKNOWN:
            default:
                if (!connected) {
                    System.err.println("[ERROR] Received '" + message + "', despite not being connected to a load balancer");
                    break;
                }

                System.err.println("[ERROR] Received: '" + message + "', unknown argument");
        }
    }

    /**
     * Process the passed in job
     *
     * @param job The job to be processed
     *
     * @return Whether or not the job processing was interrupted
     */
    private boolean processJob(Job job) {
        System.out.println("===============================================================================");
        System.out.println("[INFO] Began processing job '" + job.getName() + "' in thread '" + Thread.currentThread().getName() + "'...\n");
        System.out.println("[INFO] Current job list:\n" + jobManager.toString());

        // Try sleep for the job duration
        try {
            Thread.sleep(job.getDuration() * 1000);
        } catch (InterruptedException e) {
            return false;
        }

        System.out.println("===============================================================================");
        System.out.println("[INFO] Job '" + job.getName() + "' complete\n");
        System.out.println("[INFO] Previous job information for job '" + job.getName() + "':\n" + jobManager.jobToString(job.getName()) + "\n");

        // Update the job status to COMPLETE
        JobManager.getInstance().updateJobStatus(job, JobStatus.COMPLETE);

        System.out.println("[INFO] Current job list:\n" + jobManager.toString() + "\n");

        return true;
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
