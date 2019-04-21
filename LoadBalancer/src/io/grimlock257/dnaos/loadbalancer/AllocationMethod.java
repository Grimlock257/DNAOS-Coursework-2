package io.grimlock257.dnaos.loadbalancer;

/**
 * Enum of valid allocation algorithms for the Load Balancer to use when allocating jobs
 *
 * Adam Watson
 * Year 2 - Computer Systems Engineering
 * Distributed Network Architecture & Operating Systems Module CW-2
 */
public enum AllocationMethod {
    WEIGHTED,
    NON_WEIGHTED;

    // Cache the values array to avoid calling it every time
    public static final AllocationMethod values[] = values();
}
