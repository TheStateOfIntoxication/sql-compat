package com.sqlcompat.core.model;

/**
 * Issue severity. Used by the CLI {@code --fail-on} option to break CI builds.
 */
public enum Severity {

    /** Definitely breaks on the target dialect. Blocks migration. */
    BLOCKER(3),

    /** Compiles but behaves differently, or silently changes semantics. */
    MAJOR(2),

    /** Works, but is on the deprecated / discouraged side. */
    MINOR(1),

    /** Purely informational, e.g. "this statement parsed with fallback". */
    INFO(0);

    private final int weight;

    Severity(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    public boolean atLeast(Severity other) {
        return this.weight >= other.weight;
    }
}
