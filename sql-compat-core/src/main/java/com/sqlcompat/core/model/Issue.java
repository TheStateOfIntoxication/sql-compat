package com.sqlcompat.core.model;

import java.util.Objects;

/**
 * A single compatibility finding produced by a rule.
 *
 * @param ruleId     stable rule id, e.g. {@code R001}
 * @param ruleName   human readable rule name
 * @param severity   how bad it is
 * @param message    what the problem is
 * @param snippet    the offending fragment from the statement
 * @param suggestion how to rewrite it for the target dialect
 * @param location   where in the source it lives
 */
public record Issue(
        String ruleId,
        String ruleName,
        Severity severity,
        String message,
        String snippet,
        String suggestion,
        SourceLocation location) {

    public Issue {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(severity, "severity");
        if (location == null) {
            location = SourceLocation.UNKNOWN;
        }
    }
}
