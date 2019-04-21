package io.grimlock257.dnaos.initiator;

import io.grimlock257.dnaos.initiator.job.Job;
import io.grimlock257.dnaos.initiator.job.JobStatus;
import io.grimlock257.dnaos.initiator.managers.JobManager;
import io.grimlock257.dnaos.initiator.managers.MessageManager;
import io.grimlock257.dnaos.initiator.message.MessageTypeIn;
import io.grimlock257.dnaos.initiator.message.MessageTypeOut;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Represent the Initiator in the Initiator project
 * This class represents the initiator and all it's functionality
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Initiator {
    // Constants storing indexes for information within a message
    private final int I_MESSAGE_TYPE = 0;
    private final int I_COMPLETE_JOB_NAME = 1;
    private final int I_CANCELLED_JOB_NAME = 1;
    private final int I_SHUTDOWN_NODE_FAILURE_NAME = 1;
    private final int I_SHUTDOWN_NODE_SUCCESS_NAME = 1;
    private final int I_DATA_DUMP_NODE_NAME = 1;
    private final int I_DATA_DUMP_NODE_NAME_FAILURE = 1;

    // How frequently to reattempt connection to the load balancer
    private final int RECONNECTION_TIME = 4 * 1000;

    private boolean connected = false;

    // Information about the initiator
    private int port;

    // Information about the load balancer
    private String lbHost;
    private int lbPort;
    private InetAddress lbAddr;

    // The socket for the initiator to communicate through
    private DatagramSocket socket;

    // Managers that the initiator uses
    private MessageManager messageManager;
    private JobManager jobManager;

    // Store a reference to the keyboard
    private BufferedReader keyboard;

    /**
     * Create a new initiator instance
     *
     * @param port   The port for the initiator to communicate through
     * @param lbHost The IP address of the load balancer
     * @param lbPort The port of the load balancer
     */
    public Initiator(int port, String lbHost, int lbPort) {
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

            connect();
            loop();
        } catch (BindException e) {
            if (e.getMessage().toLowerCase().contains("address already in use")) {
                System.err.println("[ERROR] Port " + port + " is already in use, please select another port via the command line arguments");
                System.err.println("[ERROR] Usage: java initiator <port> <load balancer host address> <load balancer port>");
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
                    messageManager.send(MessageTypeOut.INITIATOR_REGISTER.toString() + "," + InetAddress.getLocalHost().getHostAddress() + "," + port, lbAddr, lbPort);
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
        // Create a thread to continuously get user input
        Thread userInput = new Thread("initiator_user_input") {
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
        System.out.println("\n===============================================================================");

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
                    System.out.println("[ERROR] Could not register with load balancer as it's already engaged with another initiator\n");
                    System.out.println("[INFO] Shutting down...");

                    System.exit(-1);
                } else {
                    System.out.println("[INFO] Ignoring as already registered with the load balancer...");
                }

                break;
            case COMPLETE_JOB:
                if (!connected) {
                    System.err.println("[ERROR] Received '" + message + "', despite not being connected to a load balancer");
                    break;
                }

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
            case CANCEL_JOB_CONFIRM:
                if (!connected) {
                    System.err.println("[ERROR] Received '" + message + "', despite not being connected to a load balancer");
                    break;
                }

                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String cancelledJobName = getValidStringArg(args, I_CANCELLED_JOB_NAME);

                if (cancelledJobName == null) {
                    System.out.println("[ERROR] Job was not altered, some of the supplied information was invalid");
                } else {
                    Job cancelledJob = jobManager.findByName(cancelledJobName);

                    jobManager.updateJobStatus(cancelledJob, JobStatus.CANCELLED);

                    System.out.println("[INFO] Job '" + cancelledJob.getName() + "' cancelled\n");
                    System.out.println("[INFO] Current job list:\n" + jobManager.toString());
                }

                break;
            case NODE_SHUTDOWN_SPECIFIC_FAILURE:
                if (!connected) {
                    System.err.println("[ERROR] Received '" + message + "', despite not being connected to a load balancer");
                    break;
                }

                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String shutdownFailedNodeName = getValidStringArg(args, I_SHUTDOWN_NODE_FAILURE_NAME);

                if (shutdownFailedNodeName == null) {
                    System.out.println("[ERROR] Some of the supplied information was invalid");
                } else {
                    System.out.println("[INFO] The requested shutdown of '" + shutdownFailedNodeName + "' could not be carried out as it was not found on the Load Balancer\n");
                }

                break;
            case NODE_SHUTDOWN_SPECIFIC_SUCCESS:
                if (!connected) {
                    System.err.println("[ERROR] Received '" + message + "', despite not being connected to a load balancer");
                    break;
                }

                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String shutdownSuccessNodeName = getValidStringArg(args, I_SHUTDOWN_NODE_SUCCESS_NAME);

                if (shutdownSuccessNodeName == null) {
                    System.out.println("[ERROR] Some of the supplied information was invalid");
                } else {
                    System.out.println("[INFO] The requested shutdown of '" + shutdownSuccessNodeName + "' was successful\n");
                }

                break;
            case DATA_DUMP_LOAD_BALANCER:
                // Use args[0] instead of the whole message as the message will be a data dump and not display nicely
                // in this context as it will need to be displayed separately

                if (!connected) {
                    System.err.println("[ERROR] Received an '" + args[0] + "', despite not being connected to a load balancer");
                    break;
                }

                System.out.println("[INFO] Received an '" + args[0] + "', processing...\n");
                System.out.println("[INFO] Received the following data dump from the load balancer for its registered client, nodes and jobs:\n\n-- Load Balancer Data Dump --");
                System.out.println(String.join(",", Arrays.copyOfRange(args, 1, args.length)));

                break;
            case DATA_DUMP_NODE_SUCCESS:
                // Use args[0] instead of the whole message as the message will be a data dump and not display nicely
                // in this context as it will need to be displayed separately

                if (!connected) {
                    System.err.println("[ERROR] Received an '" + args[0] + "', despite not being connected to a load balancer");
                    break;
                }

                System.out.println("[INFO] Received an '" + args[0] + "', processing...\n");

                String dataDumpNodeName = getValidStringArg(args, I_DATA_DUMP_NODE_NAME);

                if (dataDumpNodeName == null) {
                    System.out.println("[ERROR] Some of the supplied information was invalid");
                } else {
                    System.out.println("[INFO] Received the following data dump from the load balancer for node '" + dataDumpNodeName + "' job allocation history and thread list:\n\n-- Node '" + dataDumpNodeName + "' Data Dump --");
                    System.out.println(String.join(",", Arrays.copyOfRange(args, 2, args.length)));
                }

                break;
            case DATA_DUMP_NODE_FAILURE:
                if (!connected) {
                    System.err.println("[ERROR] Received '" + message + "', despite not being connected to a load balancer");
                    break;
                }

                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String dataDumpFailedNodeName = getValidStringArg(args, I_DATA_DUMP_NODE_NAME_FAILURE);

                if (dataDumpFailedNodeName == null) {
                    System.out.println("[ERROR] Some of the supplied information was invalid");
                } else {
                    System.out.println("[INFO] The requested data dump of '" + dataDumpFailedNodeName + "' could not be carried out as it was not found on the Load Balancer\n");
                }

                break;
            case UNKNOWN:
            default:
                if (!connected) {
                    System.err.println("[ERROR] Received '" + message + "', despite not being connected to a load balancer");
                    break;
                }

                System.err.println("[ERROR] Received: '" + message + "', unknown argument");
        }

        System.out.println("===============================================================================");
        System.out.print("Press enter to input...");
    }

    /**
     * Represent valid menu options that the initiator can send to the load balancer
     */
    private enum CommandOptions {
        NEW_JOB,
        CANCEL_JOB,
        DATA_DUMP_LOAD_BALANCER, // Show connected client, connected nodes and stored jobs
        DATA_DUMP_NODES, // For every connected node, show its details, completed jobs, allocated jobs and alive non-daemon threads
        DATA_DUMP_NODE_SPECIFIC, // For the specific node, show its details, completed jobs, allocated jobs and alive non-daemon threads
        SHUTDOWN_SPECIFIC,
        SHUTDOWN_ALL;

        // Cache the values array to avoid calling it every time
        public static final CommandOptions values[] = values();
    }

    /**
     * Get input from the user search as the command they want to issue, and any required arguments
     */
    private void getUserInput() {
        String displayOptions = null;

        while (displayOptions == null) {
            try {
                displayOptions = keyboard.readLine();
            } catch (IOException e) {
                System.err.println("[ERROR] IO Errro");
            }
        }

        System.out.println("===============================================================================");

        // Ask what type of command to carry out
        System.out.println("What command do you want to issue (Enter command number)?");
        for (CommandOptions option : EnumSet.allOf(CommandOptions.class)) {
            System.out.println(option.ordinal() + 1 + ") " + option.toString().replace("_", " "));
        }

        int menuSelection = getMenuInput(EnumSet.allOf(CommandOptions.class).size());

        // Convert their number selection back to the enum value
        CommandOptions selected = CommandOptions.values[menuSelection - 1];

        System.out.println("===============================================================================");

        // Provide the relevant input form depending on the menu option they selected
        switch (selected) {
            case NEW_JOB:
                System.out.print("Enter new job name: ");
                String jobName = getStringInput();

                System.out.print("Enter new job duration: ");
                int jobDuration = getIntegerInput();
                Job newJob = new Job(jobName, jobDuration);

                boolean hasJobAdded = jobManager.addJob(newJob);

                if (!hasJobAdded) {
                    System.out.println("[ERROR] Job was not added, the supplied information matched an existing job\n");
                } else {
                    messageManager.send(MessageTypeOut.NEW_JOB.toString() + "," + jobName + "," + jobDuration, lbAddr, lbPort);

                    System.out.println("\n[INFO] New job added: " + newJob.toString() + "\n");
                    System.out.println("[INFO] Current job list:\n" + jobManager.toString());
                }

                break;
            case CANCEL_JOB:
                System.out.print("Enter job name of which to cancel: ");
                String cancelJobName = getStringInput();

                if (cancelJobName == null) {
                    System.out.println("[ERROR] Cancel job request was not sent, some of the supplied information was invalid");
                } else {
                    Job cancelJob = jobManager.findByName(cancelJobName);

                    if (cancelJob == null) {
                        System.out.println("[ERROR] Cancel job request was not issued as no job with name '" + cancelJobName + "' was found");
                    } else if (jobManager.getJobStatus(cancelJobName) == JobStatus.COMPLETE) {
                        System.out.println("[ERROR] Cancel job request was not sent as the job is already complete");
                    } else {
                        jobManager.updateJobStatus(cancelJob, JobStatus.REQUESTED_CANCEL);

                        messageManager.send(MessageTypeOut.CANCEL_JOB_REQUEST.toString() + "," + cancelJobName, lbAddr, lbPort);

                        System.out.println("\n[INFO] Job cancel request has been issued");
                    }
                }

                break;
            case DATA_DUMP_LOAD_BALANCER:
                messageManager.send(MessageTypeOut.DATA_DUMP_LOAD_BALANCER.toString(), lbAddr, lbPort);
                System.out.println("");

                break;
            case DATA_DUMP_NODES:
                messageManager.send(MessageTypeOut.DATA_DUMP_NODES_REQUEST.toString(), lbAddr, lbPort);
                System.out.println("");

                break;
            case DATA_DUMP_NODE_SPECIFIC:
                System.out.print("Enter name of node to retrieve data dump from: ");

                String nodeToDataDump = getStringInput();

                messageManager.send(MessageTypeOut.DATA_DUMP_NODE_SPECIFIC_REQUEST.toString() + "," + nodeToDataDump, lbAddr, lbPort);
                System.out.println("");

                break;
            case SHUTDOWN_SPECIFIC:
                System.out.print("Enter node name to shutdown: ");

                String nodeToShutdown = getStringInput();

                messageManager.send(MessageTypeOut.NODE_SHUTDOWN_SPECIFIC.toString() + "," + nodeToShutdown, lbAddr, lbPort);
                System.out.println("");

                break;
            case SHUTDOWN_ALL:
                System.out.print("Are you sure you want to shutdown this initiator, the load balancer and all nodes? (Y/N): ");

                String selection = getStringInput().toLowerCase();

                while (!selection.equals("y") && !selection.equals("n")) {
                    System.out.println("[INPUT ERROR] Please enter either Y or N");
                    System.out.print("> ");

                    selection = getStringInput().toLowerCase();
                }

                if (selection.equals("y")) {
                    messageManager.send(MessageTypeOut.LB_SHUTDOWN.toString(), lbAddr, lbPort);

                    System.out.println("[INFO] Shutting down...");
                    System.exit(0);
                } else {
                    System.out.println("[INFO] Shutdown cancelled");
                }

                break;
            default:
                System.err.println("[ERROR] Something went wrong... unknown option '" + selected.toString() + "'");
        }

        System.out.println("===============================================================================");
        System.out.print("Press enter to input...");
    }

    /**
     * Get validated menu input from a user, between 1 and specified maximum
     *
     * @param maxMenuOption The maximum integer allowed
     *
     * @return The validated menu option
     */
    private int getMenuInput(int maxMenuOption) {
        int menuSelection = -1;

        while (menuSelection < 1 || menuSelection > maxMenuOption) {
            try {
                System.out.print("> ");
                menuSelection = Integer.parseInt(keyboard.readLine());

                if (menuSelection < 1 || menuSelection > EnumSet.allOf(CommandOptions.class).size()) {
                    System.out.println("[INPUT ERROR] Please enter integer between 1 and " + maxMenuOption);
                } else {
                    break;
                }
            } catch (NumberFormatException e) {
                System.out.println("[INPUT ERROR] Please enter an integer only");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return menuSelection;
    }

    /**
     * Get validated integer input from the user (i.e integer input that is 0 or greater)
     *
     * @return The validated integer input
     */
    private int getIntegerInput() {
        int userInput;

        while (true) {
            try {
                userInput = Integer.parseInt(keyboard.readLine());

                if (userInput < 0) {
                    System.out.println("[INPUT ERROR] Please enter an integer that is 0 or greater");
                } else {
                    break;
                }
            } catch (NumberFormatException e) {
                System.out.println("[INPUT ERROR] Please enter an integer only");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return userInput;
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
}
