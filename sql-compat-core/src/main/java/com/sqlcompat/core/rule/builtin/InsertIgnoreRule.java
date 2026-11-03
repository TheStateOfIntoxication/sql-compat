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
 * R005 - MySQL {@code INSERT IGNORE INTO}.
 *
 * <p>Kingbase / PostgreSQL need {@code INSERT ... ON CONFLICT DO NOTHING}. Watch out for the
 * semantic gap: MySQL's INSERT IGNORE also downgrades data errors (truncation, NOT NULL) to
 * warnings, while ON CONFLICT only handles conflicts. Behaviour is not identical.
 */
public final class InsertIgnoreRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\binsert\\s+ignore\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R005";
    }

    @Override
    public String name() {
        return "INSERT IGNORE must become ON CONFLICT DO NOTHING";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "INSERT IGNORE is MySQL-only syntax.";
    }

    @Override
    public String suggestion() {
        return "INSERT IGNORE INTO t (..) VALUES (..) -> INSERT INTO t (..) VALUES (..) ON CONFLICT DO NOTHING";
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
                    "INSERT IGNORE is MySQL-only. Note it also swallows data errors, "
                            + "which ON CONFLICT does not — review the intent before rewriting.",
                    "INSERT IGNORE",
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
