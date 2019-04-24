package io.grimlock257.dnaos.initiator.message;

/**
 * Enum of valid incoming types of message from the Initiator point of view
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public enum MessageTypeIn {
    REGISTER_CONFIRM,
    REGISTER_FAILURE,
    LOAD_BALANCER_SHUTDOWN,
    COMPLETE_JOB,
    NODE_SHUTDOWN_SPECIFIC_FAILURE,
    NODE_SHUTDOWN_SPECIFIC_SUCCESS,
    DATA_DUMP_LOAD_BALANCER,
    DATA_DUMP_NODE_FAILURE,
    DATA_DUMP_NODE_SUCCESS,
    CANCEL_JOB_CONFIRM,
    UNKNOWN
}
