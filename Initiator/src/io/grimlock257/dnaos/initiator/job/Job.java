package io.grimlock257.dnaos.initiator.job;

/**
 * Represents a Job within the Initiator project
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public class Job {
    private String name;
    private int duration;

    /**
     * Create a new job with the supplied name and duration
     *
     * @param name     The name of the job
     * @param duration The duration of the job
     */
    public Job(String name, int duration) {
        this.name = name;
        this.duration = duration;
    }

    /**
     * @return The name of the job
     */
    public String getName() {
        return this.name;
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
        return "Name: " + name + ", Duration: " + duration;
    }
}
