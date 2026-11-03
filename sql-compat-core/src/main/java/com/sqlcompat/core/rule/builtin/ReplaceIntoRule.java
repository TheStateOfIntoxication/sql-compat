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
 * R019 - MySQL {@code REPLACE INTO t ...}.
 *
 * <p>Kingbase / PostgreSQL have no REPLACE INTO. It is not a simple synonym for upsert either:
 * MySQL REPLACE deletes the conflicting row and inserts a new one, so triggers fire twice and
 * the surrogate key changes. Rewriting to ON CONFLICT changes that behaviour on purpose — which
 * is usually what you want, but it must be a decision, not an accident.
 */
public final class ReplaceIntoRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\breplace\\s+into\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R019";
    }

    @Override
    public String name() {
        return "REPLACE INTO must become ON CONFLICT (or MERGE)";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }

    @Override
    public String description() {
        return "REPLACE INTO is MySQL-only and behaves as delete-then-insert.";
    }

    @Override
    public String suggestion() {
        return "REPLACE INTO t (..) VALUES (..) -> INSERT INTO t (..) VALUES (..) "
                + "ON CONFLICT (key) DO UPDATE SET ...";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        Matcher matcher = PATTERN.matcher(context.sql());
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "REPLACE INTO does not exist on Kingbase/PostgreSQL, and its delete-then-insert "
                            + "semantics differ from ON CONFLICT (triggers, surrogate keys).",
                    "REPLACE INTO",
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
