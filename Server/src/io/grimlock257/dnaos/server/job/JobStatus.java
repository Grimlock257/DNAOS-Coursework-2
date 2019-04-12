package io.grimlock257.dnaos.server.job;

/**
 * Enum of valid statuses that a job can be in from the Load Balancer point of view
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public enum JobStatus {
    UNALLOCATED,
    BEING_ALLOCATED,
    ALLOCATED,
    REQUESTED_CANCEL,
    CANCELLED,
    SENT
}
