package io.grimlock257.dnaos.server;

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
    private final int I_NODE_IP = 1;
    private final int I_NODE_PORT = 2;
    private final int I_NODE_NAME = 3;
    private final int I_NODE_CAP = 4;

    private int port = 0;
    private DatagramSocket socket;

    private MessageManager messageManager;
    private NodeManager nodeManager;

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

            messageManager = new MessageManager(socket);
            nodeManager = new NodeManager();

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
        while (true) {
            String nextMessage = messageManager.getNextMessage();

            if (nextMessage != "") {
                processMessage(nextMessage);
            }
        }
    }

    // Message structure: command, args0, args1, ..., args2
    // E.g: REGISTER, 192.168.1.15
    // TODO: Enum or Message packaging containing Message subclasses (i.e MessageRegister, MessageResign etc.)
    // Messages: Node Register, Node Resign, New Job (client -> lb), New Job (lb -> node), Complete job (node -> lb), Complete Job (lb -> client), LB shutdown, Node shutdown
    public void processMessage(String message) throws IOException {
        System.out.println("[DEBUG] Received message: " + message);
        String[] args = message.split(",");

        // These messages are just for testing at the moment
        switch (getValidMessageType(args)) {
            case LB_SHUTDOWN:
                System.out.println("[INFO] processMessage received 'LB_SHUTDOWN'");
                System.exit(0);
                break;
            case NODE_REGISTER:
                System.out.println("[INFO] processMessage received 'NODE_REGISTER'");

                String nodeIP = getValidStringArg(args, I_NODE_IP);
                int nodePort = getValidIntArg(args, I_NODE_PORT);
                String nodeName = getValidStringArg(args, I_NODE_NAME);
                int nodeCap = getValidIntArg(args, I_NODE_CAP);
                InetAddress nodeAddr = InetAddress.getByName(nodeIP);

                nodeManager.addNode(new Node(nodePort, nodeAddr, nodeCap, nodeName));

                messageManager.send(MessageType.REGISTER_CONFRIM.toString(), nodeAddr, nodePort);
                break;
            default:
                System.out.println("[ERROR] processMessage received: '" + message + "' (unknown argument)");
        }
    }

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

    // TODO: toUpperCase()? UPDATE
    public String getValidStringArg(String[] args, int pos) {
        if (args.length > pos) {
            return (args[pos] != null) ? args[pos].trim() : "";
        } else {
            return "";
        }
    }

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
