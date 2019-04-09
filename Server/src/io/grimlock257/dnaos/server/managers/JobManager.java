package io.grimlock257.dnaos.server.managers;

import io.grimlock257.dnaos.server.job.Job;
import io.grimlock257.dnaos.server.job.JobAlloc;
import io.grimlock257.dnaos.server.job.JobStatus;
import io.grimlock257.dnaos.server.node.Node;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Job Manager for Server project
 * This class handles the storage of jobs and the allocation of jobs to nodes
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class JobManager {
    private static JobManager instance = null;

    private LinkedHashMap<Job, JobAlloc> jobs;

    /**
     * Job constructor
     */
    private JobManager() {
        this.jobs = new LinkedHashMap<>();
    }

    /**
     * Get the instance of the JobManager singleton
     *
     * @return The instance of the JobManager
     */
    public static JobManager getInstance() {
        if (instance == null) {
            instance = new JobManager();
        }

        return instance;
    }

    /**
     * Add a job to the jobs LinkedHashMap
     *
     * @param job The job to add to the jobs LinkedHashMap
     */
    public void addJob(Job job) {
        this.jobs.put(job, new JobAlloc(null, JobStatus.UNALLOCATED));

        System.out.println("[DEBUG] Jobs list: \n" + this.toString());
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
        // Iterate through the jobs LinkedHashMap to find the supplied job
        for (Map.Entry<Job, JobAlloc> jobDetails : jobs.entrySet()) {
            if (jobDetails.getKey() == job && jobDetails.getValue().getJobStatus() == JobStatus.BEING_ALLOCATED) {
                jobDetails.getValue().setJobStatus(JobStatus.ALLOCATED);
                jobDetails.getValue().setNode(node);

                System.out.println("[INFO] Job '" + job.getName() + "' been allocated to " + node.getName());
            } else if (jobDetails.getKey() == job && jobDetails.getValue().getJobStatus() == JobStatus.ALLOCATED) {
                System.out.println("[ERROR] Job '" + job.getName() + "' has already been allocated");
            } else if (jobDetails.getKey() == job && jobDetails.getValue().getJobStatus() == JobStatus.UNALLOCATED) {
                System.out.println("[ERROR] Job '" + job.getName() + "' is not ready to be allocated yet");
            }
        }
    }

    /**
     * Update the JobStatus of a job
     *
     * @param job       The job that has the JobStatus to be updated
     * @param newStatus The new status of the job
     */
    public void updateJobStatus(Job job, JobStatus newStatus) {
        this.jobs.get(job).setJobStatus(newStatus);
    }

    /**
     * Fetch the next unallocated job from the jobs LinkedHashMap.
     *
     * @return The next unallocated job as a Job object
     */
    public Job getNextJob() {
        // Iterate through the jobs LinkedHashMap to find the next job with status 'UNALLOCATED', once found
        // set the status to 'BEING_ALLOCATED' (to prevent two threads from trying to allocate the job).
        for (Map.Entry<Job, JobAlloc> jobDetails : jobs.entrySet()) {
            if (jobDetails.getValue().getJobStatus() == JobStatus.UNALLOCATED) {
                jobDetails.getValue().setJobStatus(JobStatus.BEING_ALLOCATED);

                return jobDetails.getKey();
            }
        }

        return null;
    }

    /**
     * Fetch all the jobs that are currently allocated to a Node
     *
     * @param node The node whose jobs to find
     * @return The LinkedHashMap of the Jobs and JobStatus belonging to the specified Node
     */
    // TODO: Untested
    public LinkedHashMap<Job, JobAlloc> getNodeJobs(Node node) {
        return null;
    }

    /**
     * Get the number of jobs that are currently allocated and in progress with a Node
     *
     * @param node The node whose active jobs to tally
     * @return The number of jobs allocated to the specified Node
     */
    public int getAmountOfActiveNodeJobs(Node node) {
        int amountOfJobs = 0;

        // Iterate through the jobs LinkedHashMap and see if the job had an allocated node, and if so, if that
        // node is the same as the supplied node
        for (Map.Entry<Job, JobAlloc> jobDetails : jobs.entrySet()) {
            // Check: Node is not null && parameter node == iteration node && JobStatus of iteration job is ALLOCATED
            if (jobDetails.getValue().getNode() != null && jobDetails.getValue().getNode().equals(node) && jobDetails.getValue().getJobStatus() == JobStatus.ALLOCATED) {
                amountOfJobs++;
            }
        }

        return amountOfJobs;
    }

    /**
     * Find the specified job object in the jobs LinkedHashMap using the supplied name
     *
     * @param jobName The name of the job to locate in the jobs LinkedHashMap
     * @return The job object matching the name, or null if not found
     */
    public Job findByName(String jobName) {
        for (Map.Entry<Job, JobAlloc> jobDetails : jobs.entrySet()) {
            if (jobDetails.getKey().getName().equals(jobName)) {
                return jobDetails.getKey();
            }
        }

        return null;
    }

    /**
     * Used to display the jobs LinkedHashMap in a nice, readable format
     *
     * @return The formatted string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (Map.Entry<Job, JobAlloc> jobDetails : jobs.entrySet()) {
            i++;

            sb.append(jobDetails.getKey().toString());
            sb.append(" --- ");
            sb.append("Allocation Information: ");
            sb.append(jobDetails.getValue().toString());

            if (i != jobs.size())
                sb.append("\n");
        }

        return sb.toString();
    }
}
