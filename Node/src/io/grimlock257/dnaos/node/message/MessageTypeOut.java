package io.grimlock257.dnaos.node.message;

/**
 * Enum of valid outgoing types of message from the Node point of view
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public enum MessageTypeOut {
    NODE_REGISTER,
    NODE_RESIGN,
    COMPLETE_JOB,
    DATA_DUMP_NODE,
    CANCEL_JOB_CONFIRM,
    IS_ALIVE_CONFIRM
}
