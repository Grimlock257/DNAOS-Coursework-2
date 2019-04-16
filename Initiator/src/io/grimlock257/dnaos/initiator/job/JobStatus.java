package io.grimlock257.dnaos.initiator.job;

/**
 * Enum of valid statuses that a job can be in from the Initiator point of view
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public enum JobStatus {
    QUEUED,
    COMPLETE,
    REQUESTED_CANCEL,
    CANCELLED
}
