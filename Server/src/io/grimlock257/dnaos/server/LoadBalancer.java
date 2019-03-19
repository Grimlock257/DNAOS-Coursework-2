package io.grimlock257.dnaos.server;

import io.grimlock257.dnaos.server.managers.MessageManager;
import io.grimlock257.dnaos.server.message.MessageType;

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
    private int port = 0;
    private DatagramSocket socket;

    private MessageManager messageManager;

    public LoadBalancer(int port) {
        this.port = port;

        start();
    }

    /**
     * Try to open the DatagramSocket, if successful begin the main loop
     */
    private void start() {
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(0);
            messageManager = new MessageManager(socket);

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
        // System.out.println("[DEBUG] Received message: " + message);
        String[] args = message.split(",");

        // These messages are just for testing at the moment
        switch (getValidMessageType(args)) {
            case LB_SHUTDOWN:
                System.out.println("[INFO] processMessage received 'LB_SHUTDOWN'");
                System.exit(0);
                break;
            case NODE_REGISTER:
                System.out.println("[INFO] processMessage received 'NODE_REGISTER'");
                InetAddress nodeAddr = InetAddress.getByName("localhost");
                messageManager.send(MessageType.REGISTER_CONFRIM.toString(), nodeAddr, 5000);
                break;
            default:
                System.out.println("[ERROR] processMessage received: '" + message + "' (unknown argument)");
        }
    }

    public MessageType getValidMessageType(String[] args) {
        if (args.length > 0 && args[0] != null) {
            try {
                return MessageType.valueOf(args[0].trim());
            } catch (IllegalArgumentException e) {
                return MessageType.UNKNOWN;
            }
        } else {
            return MessageType.UNKNOWN;
        }
    }

    // TODO: toUpperCase()? UPDATE
    public String getValidArg(String[] args, int pos) {
        if (args.length > pos) {
            return (args[pos] != null) ? args[pos].toUpperCase().trim() : "";
        } else {
            return "";
        }
    }
}
