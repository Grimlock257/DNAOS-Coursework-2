package io.grimlock257.dnaos.loadbalancer;

import io.grimlock257.dnaos.loadbalancer.job.Job;
import io.grimlock257.dnaos.loadbalancer.job.JobStatus;
import io.grimlock257.dnaos.loadbalancer.managers.JobManager;
import io.grimlock257.dnaos.loadbalancer.managers.MessageManager;
import io.grimlock257.dnaos.loadbalancer.managers.NodeManager;
import io.grimlock257.dnaos.loadbalancer.message.MessageTypeIn;
import io.grimlock257.dnaos.loadbalancer.message.MessageTypeOut;
import io.grimlock257.dnaos.loadbalancer.node.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * This class represents the Load Balancer and all it's functionality
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class LoadBalancer {
    // Constants storing indexes for information within a message
    private final int I_MESSAGE_TYPE = 0;
    private final int I_INITIATOR_IP = 1;
    private final int I_INITIATOR_PORT = 2;
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
    private final int I_DATA_DUMP_NODE_NAME = 1;
    private final int I_IS_ALIVE_NODE_NAME = 1;
    private final int I_NODE_RESIGN_NAME = 1;

    // Information about the connected initiator
    private String initiatorIP;
    private int initiatorPort;
    private InetAddress initiatorAddr;

    private boolean initiatorConnected = false;

    // Information about the load balancer
    private int port = 0;

    // The socket for the load balancer to communicate though
    private DatagramSocket socket;

    // Managers that the load balancer uses
    private MessageManager messageManager;
    private NodeManager nodeManager;
    private JobManager jobManager;

    private AllocationMethod allocationMethod;

    // Store a reference to the keyboard
    private BufferedReader keyboard;

    /**
     * Create a new load balancer instance
     *
     * @param port             The port for the Load Balancer to operate on
     * @param allocationMethod The allocation method to use by the Load Balancer when allocating Jobs to Nodes
     */
    public LoadBalancer(int port, AllocationMethod allocationMethod) {
        this.port = port;
        this.allocationMethod = allocationMethod;
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
            nodeManager = NodeManager.getInstance();
            nodeManager.setAllocationMethod(allocationMethod);
            jobManager = JobManager.getInstance();

            keyboard = new BufferedReader(new InputStreamReader(System.in));

            loop();
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
                messageManager.stop();

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
        // Create a thread to continuously get user input
        Thread userInput = new Thread("load_balancer_user_input") {
            @Override
            public void run() {
                while (true) {
                    getUserInput();
                }
            }
        };

        userInput.start();

        System.out.println("Press enter at any time to shutdown load balancer...");
        System.out.println("===============================================================================");

        System.out.println("Listening for messages...");

        while (true) {
            // Process messages (if available)
            String nextMessage = messageManager.getNextMessage();

            if (nextMessage != null) {
                processMessage(nextMessage);
            }

            // Allocate a job (if available)
            Job nextJob = jobManager.getNextJob();

            if (nextJob != null) {
                Node freestNode = nodeManager.getFreestNode();

                if (freestNode != null) {
                    System.out.println("===============================================================================");
                    System.out.println("[INFO] Allocating job '" + nextJob.getName() + "'...\n");
                    System.out.println("[INFO] Current nodes:\n" + nodeManager.toString() + "\n");
                    System.out.println("[INFO] Previous job list:\n" + jobManager.toString() + "\n");

                    jobManager.allocateJob(nextJob, freestNode);
                    System.out.println("[INFO] Current job list:\n" + jobManager.toString() + "\n");

                    messageManager.send(MessageTypeOut.NEW_JOB.toString() + "," + nextJob.getName() + "," + nextJob.getDuration(), freestNode.getAddr(), freestNode.getPort());
                    System.out.println("");

                    System.out.println("[INFO] Node '" + freestNode.getName() + "' utilization is now " + String.format("%.2f", freestNode.calcUsage()) + "% (max capacity is " + freestNode.getCapacity() + ")");
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
                    Node shutdownNode = nodeManager.getByName(shutdownNodeName);

                    if (shutdownNode == null) {
                        System.out.println("[ERROR] Node was not shutdown as no node with name '" + shutdownNodeName + "' was found\n");

                        messageManager.send(MessageTypeOut.NODE_SHUTDOWN_SPECIFIC_FAILURE.toString() + "," + shutdownNodeName, initiatorAddr, initiatorPort);

                        System.out.println("\n[INFO] Initiator has been notified of failure to shutdown the node");
                    } else {
                        nodeManager.shutdownNode(shutdownNode);

                        System.out.println("\n[INFO] The following node has been removed:\n" + shutdownNode.toString() + "\n");

                        messageManager.send(MessageTypeOut.NODE_SHUTDOWN_SPECIFIC_SUCCESS.toString() + "," + shutdownNodeName, initiatorAddr, initiatorPort);

                        System.out.println("\n[INFO] Initiator has been notified of successful node shutdown");
                        System.out.println("\n[INFO] Current nodes:\n" + nodeManager.toString());
                    }
                }

                break;
            case INITIATOR_REGISTER:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String newInitiatorIP = getValidStringArg(args, I_INITIATOR_IP);
                int newInitiatorPort = getValidIntArg(args, I_INITIATOR_PORT);
                InetAddress newInitiatorAddr = InetAddress.getByName(newInitiatorIP);

                if (newInitiatorIP == null || newInitiatorPort == -1) {
                    System.out.println("[ERROR] Initiator was not added, some of the supplied information was invalid");
                } else {
                    if (initiatorConnected) {
                        System.out.println("[ERROR] Initiator was not added, already connected to a initiator\n");

                        messageManager.send(MessageTypeOut.REGISTER_FAILURE.toString(), newInitiatorAddr, newInitiatorPort);
                    } else {
                        initiatorIP = newInitiatorIP;
                        initiatorPort = newInitiatorPort;
                        initiatorAddr = newInitiatorAddr;

                        System.out.println("[INFO] New initiator added: IP: " + initiatorIP + ", Port: " + initiatorPort + "\n");

                        messageManager.send(MessageTypeOut.REGISTER_CONFIRM.toString(), initiatorAddr, initiatorPort);

                        initiatorConnected = true;
                    }
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
                    boolean hasNodeAdded = nodeManager.addNode(newNode);

                    if (!hasNodeAdded) {
                        System.out.println("[ERROR] Node was not added, some of the supplied information matched an existing node\n");

                        messageManager.send(MessageTypeOut.REGISTER_FAILURE.toString(), nodeAddr, nodePort);
                    } else {
                        System.out.println("[INFO] New node added: " + newNode.toString() + "\n");

                        messageManager.send(MessageTypeOut.REGISTER_CONFIRM.toString(), nodeAddr, nodePort);
                        System.out.println("");

                        System.out.println("[INFO] Current nodes:\n" + nodeManager.toString());
                    }
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
                    Job completedJob = jobManager.getByName(completedJobName);

                    if (completedJob == null) {
                        System.out.println("[ERROR] Job was not marked as complete or sent to the initiator as no job with name '" + completedJobName + "' was found");
                    } else {
                        Node completedJobNode = jobManager.getJobNode(completedJobName);
                        System.out.println("[INFO] Previous job information for job '" + completedJob.getName() + "':\n" + jobManager.jobToString(completedJobName) + "\n");
                        System.out.println("[INFO] Previous node information for node '" + completedJobNode.getName() + "':\n" + completedJobNode.toString() + "\n");

                        messageManager.send(MessageTypeOut.COMPLETE_JOB.toString() + "," + completedJobName, initiatorAddr, initiatorPort);
                        System.out.println("");

                        jobManager.updateJobStatus(completedJob, JobStatus.SENT);

                        System.out.println("[INFO] Job '" + completedJob.getName() + "' is complete and sent to the initiator\n");
                        System.out.println("[INFO] Current job list:\n" + jobManager.toString() + "\n");
                        System.out.println("[INFO] Current nodes:\n" + nodeManager.toString());

                        nodeManager.resetIsAliveTimer(completedJobNode.getName());
                        System.out.println("\n[INFO] Is alive timer reset for node '" + completedJobNode.getName() + "'");
                    }
                }

                break;
            case CANCEL_JOB_REQUEST:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String cancelJobName = getValidStringArg(args, I_CANCEL_REQUEST_JOB_NAME);

                if (cancelJobName == null) {
                    System.out.println("[ERROR] Job cancel request was not issued, some of the supplied information was invalid");
                } else {
                    Job cancelJob = jobManager.getByName(cancelJobName);

                    if (cancelJob == null) {
                        System.out.println("[ERROR] Job cancel request was not issued as no job with name '" + cancelJobName + "' was found");
                    } else {
                        JobStatus jobStatus = jobManager.getJobStatus(cancelJob);

                        if (jobStatus == JobStatus.ALLOCATED) {
                            Node jobNode = jobManager.getJobNode(cancelJob.getName());

                            if (jobNode == null) {
                                System.out.println("[ERROR] The node allocated to the job could not be found");
                            } else {
                                System.out.println("[INFO] Previous job information for job '" + cancelJob.getName() + "':\n" + jobManager.jobToString(cancelJobName) + "\n");

                                messageManager.send(MessageTypeOut.CANCEL_JOB_REQUEST.toString() + "," + cancelJobName, jobNode.getAddr(), jobNode.getPort());
                                System.out.println("");

                                jobManager.updateJobStatus(cancelJob, JobStatus.REQUESTED_CANCEL);

                                System.out.println("[INFO] Job '" + cancelJob.getName() + "' has been requested to be cancelled\n");
                                System.out.println("[INFO] Current job list:\n" + jobManager.toString());
                            }
                        } else {
                            System.out.println("[INFO] Previous job information for job '" + cancelJob.getName() + "':\n" + jobManager.jobToString(cancelJobName) + "\n");

                            messageManager.send(MessageTypeOut.CANCEL_JOB_CONFIRM.toString() + "," + cancelJobName, initiatorAddr, initiatorPort);
                            System.out.println("");

                            jobManager.updateJobStatus(cancelJob, JobStatus.CANCELLED);

                            System.out.println("[INFO] Job '" + cancelJob.getName() + "' cancelled\n");
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
                    Job cancelledJob = jobManager.getByName(cancelledJobName);

                    if (cancelledJob == null) {
                        System.out.println("[ERROR] Job was not marked as cancelled or sent to the initiator as no job with name '" + cancelledJobName + "' was found");
                    } else {
                        System.out.println("[INFO] Previous job information for job '" + cancelledJob.getName() + "':\n" + jobManager.jobToString(cancelledJobName) + "\n");

                        messageManager.send(MessageTypeOut.CANCEL_JOB_CONFIRM.toString() + "," + cancelledJobName, initiatorAddr, initiatorPort);
                        System.out.println("");

                        jobManager.updateJobStatus(cancelledJob, JobStatus.CANCELLED);

                        System.out.println("[INFO] Job '" + cancelledJob.getName() + "' cancelled\n");
                        System.out.println("[INFO] Current job list:\n" + jobManager.toString());

                        String cancelledJobNodeName = jobManager.getJobNode(cancelledJobName).getName();
                        nodeManager.resetIsAliveTimer(cancelledJobNodeName);
                        System.out.println("\n[INFO] Is alive timer reset for node '" + cancelledJobNodeName + "'");
                    }
                }

                break;
            case DATA_DUMP_LOAD_BALANCER:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String lbDataDump = ("\n" +
                        "[INFO] Current client:\n" + "Address: ") + initiatorAddr + ", Port: " + initiatorPort + "\n\n" +
                        "[INFO] Current nodes:\n" + nodeManager.toString() + "\n\n" +
                        "[INFO] Current job list:\n" + jobManager.toString();

                messageManager.send(MessageTypeOut.DATA_DUMP_LOAD_BALANCER.toString() + "," + lbDataDump, initiatorAddr, initiatorPort);
                System.out.println("");

                break;
            case DATA_DUMP_NODES_REQUEST:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                System.out.println("[INFO] Issuing data dump request for all connected nodes...\n");
                nodeManager.issueDataDumps();
                System.out.println("[INFO] All data dump requests have been issued");

                break;
            case DATA_DUMP_NODE_SPECIFIC_REQUEST:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String nodeToDataDump = getValidStringArg(args, I_SHUTDOWN_NODE_NAME);

                if (nodeToDataDump == null) {
                    System.out.println("[ERROR] Node data dump was not requested, some of the supplied information was invalid");
                } else {
                    Node dataDumpNode = nodeManager.getByName(nodeToDataDump);

                    if (dataDumpNode == null) {
                        System.out.println("[ERROR] Node data dump was not requested as no node with name '" + nodeToDataDump + "' was found\n");

                        messageManager.send(MessageTypeOut.DATA_DUMP_NODE_FAILURE.toString() + "," + nodeToDataDump, initiatorAddr, initiatorPort);

                        System.out.println("\n[INFO] Initiator has been notified of failure to retrieve node data dump");
                    } else {
                        messageManager.send(MessageTypeOut.DATA_DUMP_NODE.toString(), dataDumpNode.getAddr(), dataDumpNode.getPort());
                        System.out.println("\n[INFO] Data dump request sent to node '" + dataDumpNode.getName() + "'");
                    }
                }

                break;
            case DATA_DUMP_NODE:
                // Use args[0] instead of the whole message as the message will be a debug dump and not display nicely
                // in this context as it will need to be displayed separately

                String dataDumpNodeName = getValidStringArg(args, I_DATA_DUMP_NODE_NAME);
                String specificNodeDataDump = String.join(",", Arrays.copyOfRange(args, 2, args.length));

                if (dataDumpNodeName == null) {
                    System.out.println("[ERROR] Node data dump not accepted, some of the supplied information was invalid");
                    messageManager.send(MessageTypeOut.DATA_DUMP_NODE_FAILURE.toString() + "," + dataDumpNodeName, initiatorAddr, initiatorPort);
                } else {
                    System.out.println("[INFO] Received an '" + args[0] + "', processing...\n");
                    System.out.println("[INFO] Received data dump from node '" + dataDumpNodeName + "' showing job history and thread list");
                    System.out.println("[INFO] Forwarding to Initiator...\n");

                    messageManager.send(MessageTypeOut.DATA_DUMP_NODE_SUCCESS.toString() + "," + dataDumpNodeName + "," + specificNodeDataDump, initiatorAddr, initiatorPort);
                    System.out.println("\n[INFO] Initiator has been sent retrieved data dump");

                    nodeManager.resetIsAliveTimer(dataDumpNodeName);
                    System.out.println("\n[INFO] Is alive timer reset for node '" + dataDumpNodeName + "'");
                }

                break;
            case IS_ALIVE_CONFIRM:
                System.out.println("[INFO] Received '" + message + "', processing...");

                String isAliveNodeName = getValidStringArg(args, I_IS_ALIVE_NODE_NAME);

                if (isAliveNodeName == null) {
                    System.out.println("[ERROR] Some of the supplied information was invalid");
                } else {
                    Node isAliveNode = nodeManager.getByName(isAliveNodeName);

                    if (isAliveNode == null) {
                        System.out.println("[ERROR] Node alive timer could not be reset as no node with name '" + isAliveNodeName + "' was found\n");
                    } else {
                        System.out.println("[INFO] Received alive message from node '" + isAliveNodeName + "'");
                        nodeManager.resetIsAliveTimer(isAliveNodeName);
                        System.out.println("[INFO] Is alive timer reset for node '" + isAliveNodeName + "'");
                    }
                }

                break;
            case NODE_RESIGN:
                System.out.println("[INFO] Received '" + message + "', processing...\n");

                String resignNodeName = getValidStringArg(args, I_NODE_RESIGN_NAME);

                if (resignNodeName == null) {
                    System.out.println("[ERROR] Some of the supplied information was invalid");
                } else {
                    Node resignNode = nodeManager.getByName(resignNodeName);

                    if (resignNode == null) {
                        System.out.println("[ERROR] Node details could not be removed as no node with name '" + resignNodeName + "' was found\n");
                    } else {
                        System.out.println("[INFO] Received resignation from node '" + resignNodeName + "'");
                        nodeManager.shutdownViaResign(resignNode);

                        System.out.println("\n[INFO] Node '" + resignNodeName + "' has been removed from the Load Balancer");
                    }
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
            if (initiatorConnected) {
                messageManager.send(MessageTypeOut.LOAD_BALANCER_SHUTDOWN.toString(), initiatorAddr, initiatorPort);
            }

            nodeManager.shutdownAllNodes();

            System.out.println("[INFO] Shutting down...");
            System.exit(0);
        } else {
            System.out.println("[INFO] Shutdown cancelled");
        }
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
