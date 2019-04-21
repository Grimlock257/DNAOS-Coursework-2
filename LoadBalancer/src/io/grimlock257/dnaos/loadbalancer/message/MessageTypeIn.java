package io.grimlock257.dnaos.loadbalancer.message;

/**
 * Enum of valid incoming types of message from the Load Balancer point of view
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public enum MessageTypeIn {
    INITIATOR_REGISTER,
    NODE_REGISTER,
    NODE_RESIGN,
    NODE_SHUTDOWN_SPECIFIC,
    LB_SHUTDOWN,
    NEW_JOB,
    COMPLETE_JOB,
    CANCEL_JOB_REQUEST,
    CANCEL_JOB_CONFIRM,
    DATA_DUMP_LOAD_BALANCER,
    DATA_DUMP_NODE,
    DATA_DUMP_NODES_REQUEST,
    DATA_DUMP_NODE_SPECIFIC_REQUEST,
    UNKNOWN
}
