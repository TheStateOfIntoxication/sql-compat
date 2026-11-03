package com.sqlcompat.core.model;

import java.util.List;

/**
 * Result of scanning one statement.
 *
 * @param statement  the statement that was scanned
 * @param issues     findings; empty when the statement is clean
 * @param parseError non-null when the statement could not be parsed into an AST
 */
public record ScanResult(SqlStatement statement, List<Issue> issues, String parseError) {

    public ScanResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean clean() {
        return issues.isEmpty();
    }
}
