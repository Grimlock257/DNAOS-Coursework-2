package io.grimlock257.dnaos.client.message;

/**
 * Enum of valid types of message from the Client point of view
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public enum MessageType {
    CLIENT_REGISTER,
    REGISTER_CONFIRM,
    REGISTER_FAILURE,
    NEW_JOB,
    COMPLETE_JOB,
    LB_SHUTDOWN,
    NODE_SHUTDOWN_SPECIFIC,
    CANCEL_JOB_REQUEST,
    CANCEL_JOB_CONFIRM,
    UNKNOWN
}
