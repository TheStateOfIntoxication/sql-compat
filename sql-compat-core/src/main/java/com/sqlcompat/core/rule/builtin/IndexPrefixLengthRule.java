package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * R018 - MySQL prefix-length indexes, e.g. {@code KEY idx_name (name(10))}.
 *
 * <p>Kingbase / PostgreSQL cannot index a prefix of a column. The usual replacement is an
 * expression index on {@code left(col, 10)} — and that index is only used when the query uses
 * the same expression, so the query needs to change too. This is one of the cases where the
 * DDL change alone is not enough.
 */
public final class IndexPrefixLengthRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\b(?:key|index)\\s+`?(\\w+)`?\\s*\\(\\s*`?(\\w+)`?\\s*\\(\\s*(\\d+)\\s*\\)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R018";
    }

    @Override
    public String name() {
        return "Index prefix length is not supported";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }

    @Override
    public String description() {
        return "MySQL indexes can cover a column prefix; Kingbase/PostgreSQL cannot.";
    }

    @Override
    public String suggestion() {
        return "KEY idx (col(10)) -> CREATE INDEX idx ON t (left(col, 10))  "
                + "(and the query must use left(col, 10) too)";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        Matcher matcher = PATTERN.matcher(context.sql());
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            String indexName = matcher.group(1);
            String column = matcher.group(2);
            String length = matcher.group(3);
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "Prefix-length index on \"" + column + "\" has no direct equivalent; "
                            + "an expression index only helps if the queries use the same expression.",
                    matcher.group().replaceAll("\\s+", " ").trim(),
                    "CREATE INDEX " + indexName + " ON <table> (left(" + column + ", " + length + "))",
                    context.location()));
        }
        return issues;
    }
}
