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
 * R004 - MySQL paging syntax {@code LIMIT offset, size}.
 *
 * <p>Kingbase / PostgreSQL only understand {@code LIMIT size OFFSET offset}: the comma form
 * is a straight syntax error. This is the single most common blocker when a MyBatis paging
 * plugin is configured for MySQL.
 *
 * <p>The pattern matches placeholders too ({@code LIMIT #{offset}, #{pageSize}}) because
 * MyBatis statements are checked before parameter substitution.
 */
public final class PagingSyntaxRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\blimit\\s+([^\\s,;()]+)\\s*,\\s*([^\\s,;()]+)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R004";
    }

    @Override
    public String name() {
        return "LIMIT offset, size must become LIMIT size OFFSET offset";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "MySQL allows the comma form on LIMIT; Kingbase/PostgreSQL do not.";
    }

    @Override
    public String suggestion() {
        return "LIMIT 10, 20 -> LIMIT 20 OFFSET 10   (note the arguments swap)";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        Matcher matcher = PATTERN.matcher(context.sql());
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            String offset = matcher.group(1);
            String size = matcher.group(2);
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "The comma form of LIMIT is a syntax error on Kingbase/PostgreSQL.",
                    "LIMIT " + offset + ", " + size,
                    "LIMIT " + size + " OFFSET " + offset,
                    context.location()));
        }
        return issues;
    }
}
