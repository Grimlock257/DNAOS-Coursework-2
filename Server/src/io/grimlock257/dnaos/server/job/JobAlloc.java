package io.grimlock257.dnaos.server.job;

import io.grimlock257.dnaos.server.node.Node;

/**
 * Data structure to store the node allocated to a job and the status of the job
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class JobAlloc {
    private Node node;
    private JobStatus jobStatus;

    /**
     * JobAlloc constructor
     *
     * @param node      The node allocated to the job
     * @param jobStatus The status of the job
     */
    public JobAlloc(Node node, JobStatus jobStatus) {
        this.node = node;
        this.jobStatus = jobStatus;
    }

    /**
     * @return The allocated node
     */
    public Node getNode() {
        return node;
    }

    /**
     * Set the node that is allocated to the job
     *
     * @param node The node that is allocated
     */
    public void setNode(Node node) {
        this.node = node;
    }

    /**
     * @return The JobStatus of the job
     */
    public JobStatus getJobStatus() {
        return jobStatus;
    }

    /**
     * Set the JobStatus associated with the job
     *
     * @param jobStatus
     */
    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    /**
     * @return The JobAlloc formatted as a string of properties
     */
    @Override
    public String toString() {
        return "Node: " + (node != null ? node.getName() : "NULL") + ", JobStatus: " + jobStatus.toString();
    }
}
