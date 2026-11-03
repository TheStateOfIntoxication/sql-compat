package com.sqlcompat.verify.model;

/**
 * Outcome of running one case against both databases.
 */
public enum Status {

    /** Same normalized rows (and same order when the case is ordered). */
    CONSISTENT,

    /** Both ran, but the results differ. This is the interesting one. */
    DIFFERENT,

    /** At least one side failed to execute. */
    ERROR
}
