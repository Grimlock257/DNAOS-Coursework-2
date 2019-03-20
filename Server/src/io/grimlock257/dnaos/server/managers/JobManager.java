package io.grimlock257.dnaos.server.managers;

import io.grimlock257.dnaos.server.job.Job;
import io.grimlock257.dnaos.server.job.JobAlloc;
import io.grimlock257.dnaos.server.job.JobStatus;
import io.grimlock257.dnaos.server.node.Node;

import java.util.LinkedHashMap;

/**
 * Job Manager for Server project
 * This class handles the storage of jobs and the allocation of jobs to nodes
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class JobManager {
    private LinkedHashMap<Job, JobAlloc> jobs;

    /**
     * Job constructor
     */
    public JobManager() {
        this.jobs = new LinkedHashMap<>();
    }

    /**
     * Add a job to the jobs LinkedHashMap
     *
     * @param job The job to add to the jobs LinkedHashMap
     */
    public void addJob(Job job) {
        this.jobs.put(job, new JobAlloc(null, JobStatus.UNALLOCATED));

        System.out.println("[DEBUG] Jobs list: " + jobs.toString());
    }

    /**
     * Remove a job from the jobs LinkedHashMap
     *
     * @param job The job to remove from the jobs LinkedHashMap
     */
    // TODO: Untested
    public void removeJob(Job job) {
        this.jobs.remove(job);
    }

    /**
     * Allocate a specified job to a specific node
     *
     * @param job  The job that is being allocated
     * @param node The node which is being allocated the job
     */
    public void allocateJob(Job job, Node node) {

    }

    /**
     * Update the JobStatus of a job
     *
     * @param job       The job that has the JobStatus to be updated
     * @param newStatus The new status of the job
     */
    // TODO: Untested
    public void updateJobStatus(Job job, JobStatus newStatus) {
        this.jobs.get(job).setJobStatus(newStatus);
        //this.jobs.replace(job, newStatus);
    }

    /**
     * Fetch the next unallocated job from the jobs LinkedHashMap.
     *
     * @return The next unallocated job as a Job
     */
    // TODO: Mark as BEING_ALLOCATED to prevent two threads from trying to allocate?
    public Job getNextJob() {
        return null;
    }

    /**
     * Fetch all the jobs that are currently allocated to a Node
     *
     * @param node The node whose jobs to find
     * @return The LinkedHashMap of the Jobs and JobStatus belonging to the specified Node
     */
    public LinkedHashMap<Job, JobAlloc> getNodeJobs(Node node) {
        return null;
    }
}
