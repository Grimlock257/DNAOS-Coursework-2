package io.grimlock257.dnaos.node;

import io.grimlock257.dnaos.node.job.Job;
import io.grimlock257.dnaos.node.job.JobStatus;
import io.grimlock257.dnaos.node.managers.JobManager;
import io.grimlock257.dnaos.node.managers.MessageManager;
import io.grimlock257.dnaos.node.message.MessageTypeOut;

import java.net.InetAddress;

public class JobProcessRunnable implements Runnable {

    private Job job;
    private InetAddress lbAddr;
    private int lbPort;

    public JobProcessRunnable(Job job, InetAddress lbAddr, int lbPort) {
        this.job = job;
        this.lbAddr = lbAddr;
        this.lbPort = lbPort;
    }

    @Override
    public void run() {
        // Process the job
        if (processJob(job)) {
            // Send the complete job back to the Load Balancer
            MessageManager.getInstance().send(MessageTypeOut.COMPLETE_JOB + "," + job.getName(), lbAddr, lbPort);
            JobManager.getInstance().updateJobStatus(job, JobStatus.SENT);
            System.out.println("");

            System.out.println("[INFO] Job '" + job.getName() + "' has been sent to the Load Balancer\n");
            System.out.println("[INFO] Current job list:\n" + JobManager.getInstance().toString());
        }
    }

    /**
     * Process the passed in job
     *
     * @param job The job to be processed
     *
     * @return Whether or not the job processing was interrupted
     */
    private boolean processJob(Job job) {
        System.out.println("===============================================================================");
        System.out.println("[INFO] Began processing job '" + job.getName() + "' in thread '" + Thread.currentThread().getName() + "'...\n");
        System.out.println("[INFO] Current job list:\n" + JobManager.getInstance().toString());

        // Try sleep for the job duration
        try {
            Thread.sleep(job.getDuration() * 1000);
        } catch (InterruptedException e) {
            return false;
        }

        System.out.println("===============================================================================");
        System.out.println("[INFO] Job '" + job.getName() + "' complete\n");
        System.out.println("[INFO] Previous job information for job '" + job.getName() + "':\n" + JobManager.getInstance().jobToString(job.getName()) + "\n");

        // Update the job status to COMPLETE
        JobManager.getInstance().updateJobStatus(job, JobStatus.COMPLETE);

        System.out.println("[INFO] Current job list:\n" + JobManager.getInstance().toString() + "\n");

        return true;
    }
}
