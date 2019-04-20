package io.grimlock257.dnaos.loadbalancer.message;

/**
 * Enum of valid types of message from the Load Balancer point of view
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public enum MessageType {
    INITIATOR_REGISTER,
    NODE_REGISTER,
    NODE_RESIGN,
    NEW_JOB,
    COMPLETE_JOB,
    LB_SHUTDOWN,
    NODE_SHUTDOWN,
    NODE_SHUTDOWN_SPECIFIC,
    NODE_SHUTDOWN_SPECIFIC_FAILURE,
    NODE_SHUTDOWN_SPECIFIC_SUCCESS,
    DATA_DUMP_LOAD_BALANCER,
    DATA_DUMP_NODES_REQUEST,
    DATA_DUMP_NODE,
    DATA_DUMP_NODE_SPECIFIC_REQUEST,
    DATA_DUMP_NODE_FAILURE,
    DATA_DUMP_NODE_SUCCESS,
    REGISTER_CONFIRM,
    REGISTER_FAILURE,
    CANCEL_JOB_REQUEST,
    CANCEL_JOB_CONFIRM,
    UNKNOWN
}
