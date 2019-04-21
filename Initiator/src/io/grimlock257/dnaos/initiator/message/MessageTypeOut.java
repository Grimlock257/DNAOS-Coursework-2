package io.grimlock257.dnaos.initiator.message;

/**
 * Enum of valid outgoing types of message from the Initiator point of view
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public enum MessageTypeOut {
    INITIATOR_REGISTER,
    NEW_JOB,
    LB_SHUTDOWN,
    NODE_SHUTDOWN_SPECIFIC,
    DATA_DUMP_LOAD_BALANCER,
    DATA_DUMP_NODES_REQUEST,
    DATA_DUMP_NODE_SPECIFIC_REQUEST,
    CANCEL_JOB_REQUEST
}
