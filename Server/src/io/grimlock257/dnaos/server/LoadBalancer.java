package io.grimlock257.dnaos.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class LoadBalancer {
    private DatagramSocket socket;
    private int port = 0;

    public LoadBalancer(int port) {
        this.port = port;

        start();
    }

    /**
     *  Open the DatagramSocket and check for incoming packets forever
     */
    private void start() {
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(0);

            while (true) {
                byte[] buffer = new byte[2048];

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                processMessage(new String(buffer));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Try / Catch?
            socket.close();
        }
    }

    // Message structure: command, args0, args1, ..., args2
    // E.g: REGISTER, 192.168.1.15
    // TODO: Enum or Message packaging containing Message subclasses (i.e MessageRegister, MessageResign etc.)
    // Messages: Node Register, Node Resign, New Job (client -> lb), New Job (lb -> node), Complete job (node -> lb), Complete Job (lb -> client), LB shutdown, Node shutdown
    public void processMessage(String message) {
        System.out.println("[DEBUG] Received message: " + message);
        String[] args = message.split(",");

        // These messages are just for testing at the moment
        switch (getValidArg(args, 0)) {
            case "QUIT":
                System.out.println("[INFO] processMessage received 'QUIT'");
                System.exit(0);
            case "REGISTER":
                System.out.println("[INFO] processMessage received 'REGISTER'");
                break;
            default:
                System.out.println("[ERROR] processMessage received: '" + message + "' (unknown argument)");
        }
    }

    // TODO: toUpperCase()?
    public String getValidArg(String[] args, int pos) {
        if(args.length > pos) {
            return (args[pos] != null) ? args[pos].toUpperCase().trim() : "";
        } else {
            return "";
        }
    }
}
