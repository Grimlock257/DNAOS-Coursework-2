package io.grimlock257.dnaos.node.managers;

import io.grimlock257.dnaos.node.job.Job;
import io.grimlock257.dnaos.node.job.JobStatus;

import java.util.LinkedHashMap;

public class JobManager {
    private LinkedHashMap<Job, JobStatus> jobs;

    public JobManager() {
        this.jobs = new LinkedHashMap<>();
    }

    public void addJob(Job job) {
        this.jobs.put(job, JobStatus.QUEUED);

        System.out.println("[DEBUG] Jobs list: " + jobs.toString());
    }

    // TODO: Untested
    public void removeJob(Job job) {
        this.jobs.remove(job);
    }

    public void updateJobStatus(Job job, JobStatus newStatus) {
        this.jobs.replace(job, newStatus);
    }

    public void getNextJob() {

    }
}
