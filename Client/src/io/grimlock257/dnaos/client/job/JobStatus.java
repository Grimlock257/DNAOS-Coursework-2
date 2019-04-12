package io.grimlock257.dnaos.client.job;

/**
 * Enum of valid statuses that a job can be in from the Client point of view
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
