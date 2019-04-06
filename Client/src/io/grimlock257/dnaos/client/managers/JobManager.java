package io.grimlock257.dnaos.client.managers;

import io.grimlock257.dnaos.client.job.Job;
import io.grimlock257.dnaos.client.job.JobStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Job Manager for Client project
 * This class handles the storage of jobs and their status
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class JobManager {
    private static JobManager instance = null;

    private LinkedHashMap<Job, JobStatus> jobs;

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
        this.jobs.put(job, JobStatus.QUEUED);

        System.out.println("[DEBUG] (JobManager:addJob()) Jobs list: \n" + this.toString());
    }

    /**
     * Remove a job from the jobs LinkedHashMap
     *
     * @param job The job to remove from the jobs LinkedHashMap
     */
    // TODO: Untested
    public void removeJob(Job job) {
        this.jobs.remove(job);

        System.out.println("[DEBUG] (JobManager:removeJob()) Jobs list: \n" + this.toString());
    }

    /**
     * Update the JobStatus of a job
     *
     * @param job       The job that has the JobStatus to be updated
     * @param newStatus The new status of the job
     */
    // TODO: Untested
    public void updateJobStatus(Job job, JobStatus newStatus) {
        this.jobs.replace(job, newStatus);
    }

    /**
     * Fetch the next unallocated job from the jobs LinkedHashMap.
     *
     * @return The next unallocated job as a Job
     */
    // TODO: Mark as BEING_ALLOCATED to prevent two threads from trying to allocate?
    // TODO: Untested
    public Job getNextJob() {
        return null;
    }

    /**
     * Used to display the jobs LinkedHashMap in a nice, readable format
     *
     * @return The formatted string
     */
    // TODO: Untested
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (Map.Entry<Job, JobStatus> jobDetails : jobs.entrySet()) {
            i++;

            sb.append(jobDetails.getKey().toString());
            sb.append(", ");
            sb.append("Status: ");
            sb.append(jobDetails.getValue().toString());

            if (i != jobs.size())
                sb.append("\n");
        }

        return sb.toString();
    }
}
