package io.grimlock257.dnaos.loadbalancer.managers;

import io.grimlock257.dnaos.loadbalancer.job.Job;
import io.grimlock257.dnaos.loadbalancer.job.JobAlloc;
import io.grimlock257.dnaos.loadbalancer.job.JobStatus;
import io.grimlock257.dnaos.loadbalancer.node.Node;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Job Manager for Load Balancer project
 * This class handles the storage of jobs and the allocation of jobs to nodes
 *
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
            if (jobDetails.getKey() == job && jobDetails.getValue().getJobStatus() == JobStatus.UNALLOCATED) {
                jobDetails.getValue().setJobStatus(JobStatus.ALLOCATED);
                jobDetails.getValue().setNode(node);

                System.out.println("[INFO] Job '" + job.getName() + "' been allocated to '" + node.getName() + "'\n");
            } else if (jobDetails.getKey() == job && jobDetails.getValue().getJobStatus() == JobStatus.ALLOCATED) {
                System.out.println("[ERROR] Job '" + job.getName() + "' has already been allocated\n");
            }
        }
    }

    /**
     * Deallocate any jobs associated with the supplied node
     *
     * @param node The node to deallocate jobs from
     */
    public void deallocateJobs(Node node) {
        // Iterate through the jobs LinkedHashMap to find jobs allocated to the supplied node
        for (Map.Entry<Job, JobAlloc> jobDetails : jobs.entrySet()) {
            if (jobDetails.getValue().getJobStatus() != JobStatus.SENT && jobDetails.getValue().getNode() != null && jobDetails.getValue().getNode().equals(node)) {
                jobDetails.getValue().setJobStatus(JobStatus.UNALLOCATED);
                jobDetails.getValue().setNode(null);

                System.out.println("[INFO] Job '" + jobDetails.getKey().getName() + "' been deallocated from node '" + node.getName() + "'");
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
        // Iterate through the jobs LinkedHashMap to find the next job with status 'UNALLOCATED'
        for (Map.Entry<Job, JobAlloc> jobDetails : jobs.entrySet()) {
            if (jobDetails.getValue().getJobStatus() == JobStatus.UNALLOCATED) {
                return jobDetails.getKey();
            }
        }

        return null;
    }

    /**
     * Get the number of jobs that are currently allocated and in progress with a Node
     *
     * @param node The node whose active jobs to tally
     *
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
     * Find allocated node given a job name
     *
     * @param jobName The job to find which node is allocated
     *
     * @return The allocated node, or null if not found
     */
    public Node getJobNode(String jobName) {
        for (Map.Entry<Job, JobAlloc> jobDetails : jobs.entrySet()) {
            if (jobDetails.getKey().getName().equalsIgnoreCase(jobName)) {
                return jobDetails.getValue().getNode();
            }
        }

        return null;
    }

    /**
     * Find the specified job object in the jobs LinkedHashMap using the supplied name
     *
     * @param jobName The name of the job to locate in the jobs LinkedHashMap
     *
     * @return The job object matching the name, or null if not found
     */
    public Job getByName(String jobName) {
        for (Map.Entry<Job, JobAlloc> jobDetails : jobs.entrySet()) {
            if (jobDetails.getKey().getName().equalsIgnoreCase(jobName)) {
                return jobDetails.getKey();
            }
        }

        return null;
    }

    /**
     * Formats a job as a string with it's allocation information
     *
     * @param jobName The job for which to represent in a string format
     *
     * @return The formatted string
     */
    public String jobToString(String jobName) {
        StringBuilder sb = new StringBuilder();

        // Iterate through the jobs LinkedHashMap, until we find the job matching the supplied name, once found
        // append all information to a string to return to caller
        for (Map.Entry<Job, JobAlloc> jobDetails : jobs.entrySet()) {
            if (jobDetails.getKey().getName().equals(jobName)) {
                sb.append(jobDetails.getKey().toString());
                sb.append(" --- ");
                sb.append("Allocation Information: ");
                sb.append(jobDetails.getValue().toString());

                break;
            }
        }

        return sb.toString();
    }

    /**
     * Get the JobStatus of the specified job
     *
     * @param job The job for which to find the current status
     *
     * @return The JobStatus of the supplied job, null if job is not found
     */
    public JobStatus getJobStatus(Job job) {
        for (Map.Entry<Job, JobAlloc> jobDetails : jobs.entrySet()) {
            if (jobDetails.getKey().equals(job)) {
                return jobDetails.getValue().getJobStatus();
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
        for (Map.Entry<Job, JobAlloc> jobDetails : jobs.entrySet()) {
            i++;

            sb.append(jobDetails.getKey().toString());
            sb.append(" --- ");
            sb.append("Allocation Information: ");
            sb.append(jobDetails.getValue().toString());

            // If we haven't reached the end of the list, add a new line
            if (i != jobs.size())
                sb.append("\n");
        }

        return sb.toString();
    }
}
