package io.grimlock257.dnaos.server.message;

/**
 * Enum of valid types of message from the Server point of view
 * <p>
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public enum MessageType {
    CLIENT_REGISTER,
    NODE_REGISTER,
    NODE_RESIGN,
    NEW_JOB,
    COMPLETE_JOB,
    LB_SHUTDOWN,
    NODE_SHUTDOWN,
    REGISTER_CONFIRM,
    UNKNOWN
}
