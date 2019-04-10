package io.grimlock257.dnaos.node.managers;

import io.grimlock257.dnaos.node.job.Job;
import io.grimlock257.dnaos.node.job.JobStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Job Manager for Node project
 * This class handles the storage of jobs and their status
 *
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
     * Update the JobStatus of a job
     *
     * @param job       The job that has the JobStatus to be updated
     * @param newStatus The new status of the job
     */
    public void updateJobStatus(Job job, JobStatus newStatus) {
        this.jobs.replace(job, newStatus);
    }

    /**
     * Fetch the next queued job from the jobs LinkedHashMap.
     *
     * @return The next queued job as a Job object
     */
    public Job getNextJob() {
        // Iterate through the jobs LinkedHashMap to find the next job with status 'jobs', once found
        // set the status to 'IN_PROGRESS'
        for (Map.Entry<Job, JobStatus> jobDetails : jobs.entrySet()) {
            if (jobDetails.getValue() == JobStatus.QUEUED) {
                jobDetails.setValue(JobStatus.IN_PROGRESS);

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

        // Iterate through the jobs LinkedHashMap, appending all the information about it
        int i = 0;
        for (Map.Entry<Job, JobStatus> jobDetails : jobs.entrySet()) {
            i++;

            sb.append(jobDetails.getKey().toString());
            sb.append(", ");
            sb.append("Status: ");
            sb.append(jobDetails.getValue().toString());

            // If we haven't reached the end of the list, add a new line
            if (i != jobs.size())
                sb.append("\n");
        }

        return sb.toString();
    }
}
