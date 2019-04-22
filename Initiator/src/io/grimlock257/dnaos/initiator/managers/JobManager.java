package io.grimlock257.dnaos.initiator.managers;

import io.grimlock257.dnaos.initiator.job.Job;
import io.grimlock257.dnaos.initiator.job.JobStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Job Manager for Initiator project
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
     * Add a job to the jobs LinkedHashMap providing a job of the same name doesn't already exist
     *
     * @param job The job to add to the jobs LinkedHashMap
     *
     * @return Whether or not the addition was successful, false is matching name
     */
    public boolean addJob(Job job) {
        for (Map.Entry<Job, JobStatus> searchJob : jobs.entrySet()) {
            if (searchJob.getKey().getName().equals(job.getName())) {
                return false;
            }
        }

        this.jobs.put(job, JobStatus.QUEUED);

        return true;
    }

    /**
     * Get the status of a job with the specified name
     *
     * @param jobName The job to find the status off
     *
     * @return The status of the job
     */
    public JobStatus getJobStatus(String jobName) {
        for (Map.Entry<Job, JobStatus> jobDetails : jobs.entrySet()) {
            if (jobDetails.getKey().getName().equalsIgnoreCase(jobName)) {
                return jobDetails.getValue();
            }
        }

        return null;
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
     * Find the specified job object in the jobs LinkedHashMap using the supplied name
     *
     * @param jobName The name of the job to locate in the jobs LinkedHashMap
     *
     * @return The job object matching the name, or null if not found
     */
    public Job findByName(String jobName) {
        for (Map.Entry<Job, JobStatus> jobDetails : jobs.entrySet()) {
            if (jobDetails.getKey().getName().equalsIgnoreCase(jobName)) {
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
