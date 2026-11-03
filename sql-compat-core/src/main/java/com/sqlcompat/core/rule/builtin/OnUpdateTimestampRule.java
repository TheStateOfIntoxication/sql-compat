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
 * R015 - MySQL {@code DATETIME ... ON UPDATE CURRENT_TIMESTAMP}.
 *
 * <p>Kingbase / PostgreSQL have no column-level "on update" clause. The behaviour must move to
 * a trigger or to the application layer, and doing it in the application is usually the better
 * answer for a MyBatis code base — but it has to be a decision, because after migration the
 * column simply stops updating and nobody notices for months.
 */
public final class OnUpdateTimestampRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\bon\\s+update\\s+(?:current_timestamp(?:\\(\\s*\\d*\\s*\\))?|now\\s*\\(\\s*\\))",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R015";
    }

    @Override
    public String name() {
        return "ON UPDATE CURRENT_TIMESTAMP has no column-level equivalent";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }

    @Override
    public String description() {
        return "Column-level ON UPDATE CURRENT_TIMESTAMP is MySQL-only; use a trigger or app code.";
    }

    @Override
    public String suggestion() {
        return "drop ON UPDATE CURRENT_TIMESTAMP; set updated_at in the application "
                + "or create a BEFORE UPDATE trigger";
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
                    "After migration this column silently stops updating — the statement keeps "
                            + "succeeding, which is why it is easy to miss.",
                    matcher.group(),
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
