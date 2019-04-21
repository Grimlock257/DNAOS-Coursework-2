package io.grimlock257.dnaos.node.message;

/**
 * Enum of valid incoming types of message from the Node point of view
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public enum MessageTypeIn {
    NEW_JOB,
    NODE_SHUTDOWN,
    REGISTER_CONFIRM,
    REGISTER_FAILURE,
    DATA_DUMP_NODE,
    CANCEL_JOB_REQUEST,
    IS_ALIVE,
    UNKNOWN
}
