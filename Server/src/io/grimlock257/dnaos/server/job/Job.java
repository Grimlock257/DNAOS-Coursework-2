package io.grimlock257.dnaos.server.job;

/**
 * Represents a Job within the Server package
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Job {
    private int duration;

    /**
     * Job constructor
     *
     * @param duration The duration of the job
     */
    public Job(int duration) {
        this.duration = duration;
    }

    /**
     * @return The duration of the job
     */
    public int getDuration() {
        return this.duration;
    }

    /**
     * @return The job formatted as a string of properties
     */
    @Override
    public String toString() {
        return "Duration: " + duration;
    }
}
