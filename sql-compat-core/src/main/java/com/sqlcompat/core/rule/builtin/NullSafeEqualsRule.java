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
 * R016 - MySQL null-safe equality operator {@code <=>}.
 *
 * <p>MySQL uses {@code a <=> b} to compare values where NULL equals NULL. Kingbase / PostgreSQL
 * use the SQL standard {@code a IS NOT DISTINCT FROM b}. There is no operator form, so this fails
 * at parse time — but the expression is easy to miss inside a long {@code <if>} block.
 */
public final class NullSafeEqualsRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile("<=>");

    @Override
    public String id() {
        return "R016";
    }

    @Override
    public String name() {
        return "<=> must become IS NOT DISTINCT FROM";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "MySQL null-safe equality <=> has no operator equivalent on Kingbase/PostgreSQL.";
    }

    @Override
    public String suggestion() {
        return "a <=> b -> a IS NOT DISTINCT FROM b";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String sql = context.sql();
        Matcher matcher = PATTERN.matcher(sql);
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            int start = Math.max(0, matcher.start() - 30);
            int end = Math.min(sql.length(), matcher.end() + 30);
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "The MySQL null-safe equality operator does not exist on Kingbase/PostgreSQL.",
                    sql.substring(start, end).replaceAll("\\s+", " ").trim(),
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
