package com.sqlcompat.core.model;

import com.sqlcompat.core.util.J8;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 单条语句的扫描结果。
 */
public final class ScanResult {

    private final SqlStatement statement;
    private final List<Issue> issues;
    private final String parseError;

    public ScanResult(SqlStatement statement, List<Issue> issues, String parseError) {
        this.statement = statement;
        this.issues = issues == null
                ? J8.<Issue>list()
                : Collections.unmodifiableList(new ArrayList<>(issues));
        this.parseError = parseError;
    }

    public SqlStatement statement() {
        return statement;
    }

    public List<Issue> issues() {
        return issues;
    }

    /** 无法解析成 AST 时的错误信息，正常为 {@code null}。 */
    public String parseError() {
        return parseError;
    }

    public boolean clean() {
        return issues.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScanResult)) {
            return false;
        }
        ScanResult that = (ScanResult) o;
        return Objects.equals(statement, that.statement)
                && issues.equals(that.issues)
                && Objects.equals(parseError, that.parseError);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statement, issues, parseError);
    }

    @Override
    public String toString() {
        return "ScanResult[" + statement.qualifiedId() + ", " + issues.size() + " issues]";
    }
}
