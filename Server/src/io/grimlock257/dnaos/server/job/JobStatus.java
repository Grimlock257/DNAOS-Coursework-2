package io.grimlock257.dnaos.server.job;

/**
 * Enum of valid statuses that a job can be in from the Server point of view
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public enum JobStatus {
    UNALLOCATED,
    ALLOCATED,
    COMPLETE,
    SENT
}
