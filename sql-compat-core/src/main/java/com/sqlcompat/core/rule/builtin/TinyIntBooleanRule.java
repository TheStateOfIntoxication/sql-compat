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
 * R014 - MySQL {@code TINYINT(1)} used as a boolean.
 *
 * <p>Kingbase / PostgreSQL have a real {@code BOOLEAN}. The migration trap is the display width:
 * {@code TINYINT(4)} is a number, {@code TINYINT(1)} is a de-facto boolean, and a careless
 * {@code TINYINT → SMALLINT} conversion silently turns a boolean into an integer that no longer
 * round-trips through JDBC {@code getBoolean()}.
 */
public final class TinyIntBooleanRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\btinyint\\s*\\(\\s*1\\s*\\)", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R014";
    }

    @Override
    public String name() {
        return "TINYINT(1) carries boolean semantics";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MINOR;
    }

    @Override
    public String description() {
        return "TINYINT(1) is MySQL's de-facto boolean; map it to BOOLEAN, not SMALLINT.";
    }

    @Override
    public String suggestion() {
        return "flag TINYINT(1) -> flag BOOLEAN   (do not map it to SMALLINT)";
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
                    "TINYINT(1) is used as a boolean. Mapping it to SMALLINT keeps the data but "
                            + "breaks JDBC getBoolean() / ORM boolean mapping.",
                    matcher.group(),
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
