package io.grimlock257.dnaos.loadbalancer.node;

import io.grimlock257.dnaos.loadbalancer.managers.JobManager;

import java.net.InetAddress;

/**
 * Represent the Node in the Load Balancer project
 * This class represents a nodes properties for use within the Load Balancer project
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Node {
    private int port;
    private InetAddress addr;

    private int capacity;
    private String name;

    /**
     * Node constructor
     *
     * @param port     The port for which to communicate with the node
     * @param addr     The address of the node (IP Address)
     * @param capacity The maximum capacity of the node
     * @param name     The name of the node
     */
    public Node(int port, InetAddress addr, int capacity, String name) {
        this.port = port;
        this.addr = addr;
        this.capacity = capacity;
        this.name = name;
    }

    /**
     * @return The port used by the load balancer for communication with the node
     */
    public int getPort() {
        return port;
    }

    /**
     * @return The address of the node (IP Address)
     */
    public InetAddress getAddr() {
        return addr;
    }

    /**
     * @return The maximum capacity of the node
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * @return The name of the node
     */
    public String getName() {
        return name;
    }

    /**
     * Calculates the usage of the node, based on the capacity of the node and the currently allocated jobs
     *
     * @return The calculated percentage usage of the node
     */
    public double calcUsage() {
        return (JobManager.getInstance().getAmountOfActiveNodeJobs(this) / (double) capacity) * 100;
    }

    /**
     * @return The node formatted as a string of its properties
     */
    @Override
    public String toString() {
        return "Name: " + name + ", Capacity: " + capacity + ", Address: " + addr + ", Port: " + port + ", Usage: " + calcUsage() + "%";
    }
}
