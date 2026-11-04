package com.sqlcompat.core.model;

import java.util.Objects;

/**
 * 规则产出的单条兼容性发现。
 *
 * <p>访问器保持 {@code ruleId()} 这种形式而不是 {@code getRuleId()}：与原先 record 的
 * 调用点完全一致，规则实现和报告渲染都不用改。
 */
public final class Issue {

    private final String ruleId;
    private final String ruleName;
    private final Severity severity;
    private final String message;
    private final String snippet;
    private final String suggestion;
    private final SourceLocation location;

    public Issue(String ruleId,
                 String ruleName,
                 Severity severity,
                 String message,
                 String snippet,
                 String suggestion,
                 SourceLocation location) {
        this.ruleId = Objects.requireNonNull(ruleId, "ruleId");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.ruleName = ruleName;
        this.message = message;
        this.snippet = snippet;
        this.suggestion = suggestion;
        this.location = location == null ? SourceLocation.UNKNOWN : location;
    }

    public String ruleId() {
        return ruleId;
    }

    public String ruleName() {
        return ruleName;
    }

    public Severity severity() {
        return severity;
    }

    public String message() {
        return message;
    }

    public String snippet() {
        return snippet;
    }

    public String suggestion() {
        return suggestion;
    }

    public SourceLocation location() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Issue)) {
            return false;
        }
        Issue that = (Issue) o;
        return ruleId.equals(that.ruleId)
                && severity == that.severity
                && Objects.equals(ruleName, that.ruleName)
                && Objects.equals(message, that.message)
                && Objects.equals(snippet, that.snippet)
                && Objects.equals(suggestion, that.suggestion)
                && Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId, ruleName, severity, message, snippet, suggestion, location);
    }

    @Override
    public String toString() {
        return "Issue[" + ruleId + " " + severity + " " + location + "]";
    }
}
