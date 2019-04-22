package io.grimlock257.dnaos.node;

import io.grimlock257.dnaos.node.job.Job;
import io.grimlock257.dnaos.node.job.JobStatus;
import io.grimlock257.dnaos.node.managers.JobManager;
import io.grimlock257.dnaos.node.managers.MessageManager;
import io.grimlock257.dnaos.node.message.MessageTypeIn;
import io.grimlock257.dnaos.node.message.MessageTypeOut;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private String ip;

    // Information about the load balancer
    private String lbHost;
    private int lbPort;
    private InetAddress lbAddr;

    // The socket for the node to communicate through
    private DatagramSocket socket;

    // Managers that the node uses
    private MessageManager messageManager;
    private JobManager jobManager;

    // Store a reference to the keyboard
    private BufferedReader keyboard;

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
    }

    /**
     * Try to open the DatagramSocket, if successful create the managers, connect and begin the main loop
     */
    public void start() {
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(0);

            messageManager = MessageManager.getInstance();
            messageManager.init(socket);
            jobManager = JobManager.getInstance();

            keyboard = new BufferedReader(new InputStreamReader(System.in));

            lbAddr = InetAddress.getByName(lbHost);
            ip = InetAddress.getLocalHost().getHostAddress();

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
                    messageManager.send(MessageTypeOut.NODE_REGISTER.toString() + "," + ip + "," + port + "," + name + "," + capacity, lbAddr, lbPort);
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

        // Nice formatting and display prompt on how to shutdown
        System.out.println("===============================================================================");
        System.out.println("Press enter at any time to shutdown node...");
    }

    /**
     * Check for incoming packets, and send any packets to be processed
     */
    private void loop() {
        // Create a thread to continuously get user input
        Thread userInput = new Thread("node_user_input") {
            @Override
            public void run() {
                while (true) {
                    getUserInput();
                }
            }
        };

        userInput.start();

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
                            MessageManager.getInstance().send(MessageTypeOut.COMPLETE_JOB + "," + nextJob.getName(), lbAddr, lbPort);
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

                        System.out.println("[INFO] Alive threads (non-daemon) before:");

                        // Iterate through the set of threads, printing the name of each
                        for (Thread thread : Thread.getAllStackTraces().keySet()) {
                            if (thread.isAlive() && !thread.isDaemon()) {
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

                        System.out.println("\n[INFO] Alive threads (non-daemon) after:");

                        // Iterate through the set of threads, printing the name of each
                        for (Thread thread : Thread.getAllStackTraces().keySet()) {
                            if (thread.isAlive() && !thread.isDaemon()) {
                                System.out.println("Thread name: " + thread.getName());
                            }
                        }

                        System.out.println("");
                        messageManager.send(MessageTypeOut.CANCEL_JOB_CONFIRM.toString() + "," + cancelJobName, lbAddr, lbPort);

                        jobManager.updateJobStatus(cancelJob, JobStatus.CANCELLED);

                        System.out.println("\n[INFO] Current job list:\n" + jobManager.toString());
                    }
                }

                break;
            case DATA_DUMP_NODE:
                if (!connected) {
                    System.err.println("[ERROR] Received '" + message + "', despite not being connected to a load balancer");
                    break;
                }

                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String dataDump = createDataDump();

                messageManager.send(MessageTypeOut.DATA_DUMP_NODE.toString() + "," + name + "," + dataDump, lbAddr, lbPort);

                System.out.println("\n[INFO] Load Balancer has been sent the data dump");

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
            case IS_ALIVE:
                if (!connected) {
                    System.err.println("[ERROR] Received '" + message + "', despite not being connected to a load balancer");
                    break;
                }

                System.out.println("[INFO] Received '" + message + "', processing...");
                System.out.println("[INFO] Sending is alive message back to Load Balancer...");
                messageManager.send(MessageTypeOut.IS_ALIVE_CONFIRM.toString() + "," + name, lbAddr, lbPort);

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
     * Get input from the user search as the command they want to issue, and any required arguments
     */
    private void getUserInput() {
        String displayOptions = null;

        // Wait for the user to press enter before displaying the menu
        while (displayOptions == null) {
            try {
                displayOptions = keyboard.readLine();
            } catch (IOException e) {
                System.err.println("[ERROR] IO Error");
            }
        }

        // Ask the user if they want to shutdown
        System.out.println("===============================================================================");
        System.out.print("Are you sure you want to shutdown this node? (Y/N): ");

        // Get selection from the user
        String selection = getStringInput().toLowerCase();

        while (!selection.equals("y") && !selection.equals("n")) {
            System.out.println("[INPUT ERROR] Please enter either Y or N");
            System.out.print("> ");

            selection = getStringInput().toLowerCase();
        }

        // If Y then send resignation message to Load Balancer and shutdown
        if (selection.equals("y")) {
            messageManager.send(MessageTypeOut.NODE_RESIGN.toString() + "," + name, lbAddr, lbPort);

            System.out.println("[INFO] Shutting down...");
            System.exit(0);
        } else {
            System.out.println("[INFO] Shutdown cancelled");
        }
    }

    /**
     * Create a data dump of the node. Shows node details, completed (sent to load balancer)
     * jobs and currently in progress jobs
     *
     * @return Node data dump in String format
     */
    private String createDataDump() {
        StringBuilder sbResult = new StringBuilder(); // Store node information
        StringBuilder sbSentJobs = new StringBuilder(); // Store sent jobs from node
        StringBuilder sbAllocatedJobs = new StringBuilder(); // Store allocated jobs to node
        StringBuilder sbAliveThreads = new StringBuilder(); // Store alive, non-daemon threads

        // Add the headings to each StringBuilder
        sbResult.append("[INFO] Node: ").append(this.toString()).append("\n\n");
        sbSentJobs.append("[INFO] Jobs completed by node '").append(this.name).append("':\n");
        sbAllocatedJobs.append("[INFO] Jobs allocated to node '").append(this.name).append("':\n");
        sbAliveThreads.append("[INFO] Alive (non-daemon) threads in node '").append(this.name).append("':\n");

        // LinkedHashMap of associated jobs to the current node
        LinkedHashMap<Job, JobStatus> nodeJobs = JobManager.getInstance().getJobs();

        // Iterate through the associated jobs and add the job information to the relevant StringBuilder
        for (Map.Entry<Job, JobStatus> nodeJobDetails : nodeJobs.entrySet()) {
            StringBuilder sbReference = (nodeJobDetails.getValue() == JobStatus.SENT) ? sbSentJobs : sbAllocatedJobs;

            sbReference.append(nodeJobDetails.getKey().toString());
            sbReference.append(" --- ");
            sbReference.append("Job Status: ");
            sbReference.append(nodeJobDetails.getValue().toString());
            sbReference.append("\n");
        }

        // Iterate through the set of threads, appending name of each non-daemon thread
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.isAlive() && !thread.isDaemon()) {
                sbAliveThreads.append("Thread name: ").append(thread.getName()).append("\n");
            }
        }

        // Add the two StringBuilders for each JobStatus to the main StringBuilder for Node information
        sbResult.append(sbSentJobs.toString()).append("\n");
        sbResult.append(sbAllocatedJobs.toString()).append("\n");
        sbResult.append(sbAliveThreads.toString());

        return sbResult.toString();
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
     * Get validated string input from the user (i.e input with a length greater than 0)
     *
     * @return The validated string
     */
    private String getStringInput() {
        String userInput = "";

        while (userInput.length() < 1) {
            try {
                userInput = keyboard.readLine();

                if (userInput.length() < 1) {
                    System.out.println("[INPUT ERROR] Please enter a string with a length greater than 1");
                } else {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return userInput;
    }

    /**
     * Validate the MessageTypeIn of the message
     *
     * @param args The message broken up into elements based on commas
     *
     * @return The MessageTypeIn (UNKNOWN is non valid)
     */
    private MessageTypeIn getValidMessageType(String[] args) {
        if (args.length > 0 && args[I_MESSAGE_TYPE] != null) {
            try {
                return MessageTypeIn.valueOf(args[I_MESSAGE_TYPE].trim());
            } catch (IllegalArgumentException e) {
                return MessageTypeIn.UNKNOWN;
            }
        } else {
            return MessageTypeIn.UNKNOWN;
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

    /**
     * @return The node formatted as a string of its properties
     */
    @Override
    public String toString() {
        return "Name: " + name + ", Capacity: " + capacity + ", Address: " + ip + ", Port: " + port + ", Usage: " + String.format("%.2f", (jobManager.getAmountOfActiveJobs() / (double) capacity) * 100) + "%";
    }
}
