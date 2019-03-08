package io.grimlock257.dnaos.node;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Main class of Node project
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Main {
    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        try {
            InputStreamReader input = new InputStreamReader(System.in);
            BufferedReader keyboard = new BufferedReader(input);

            DatagramSocket socket = new DatagramSocket(5000);
            InetAddress addr = InetAddress.getByName("localhost");

            System.out.println("[INFO] addr: " + addr.toString());

            System.out.println("Enter message:");
            while (true) {
                System.out.print("> ");

                String message = keyboard.readLine();
                DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, addr, 4000);
                socket.send(packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}