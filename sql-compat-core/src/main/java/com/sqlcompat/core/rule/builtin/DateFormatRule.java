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
 * R002 - MySQL {@code DATE_FORMAT(col, '%Y-%m-%d')} must be rewritten with {@code TO_CHAR}.
 */
public final class DateFormatRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile("\\bdate_format\\s*\\(", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R002";
    }

    @Override
    public String name() {
        return "DATE_FORMAT must become TO_CHAR";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "MySQL DATE_FORMAT(col, fmt) does not exist on Kingbase/PostgreSQL.";
    }

    @Override
    public String suggestion() {
        return "DATE_FORMAT(col, '%Y-%m-%d') -> TO_CHAR(col, 'YYYY-MM-DD')";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String sql = context.sql();
        Matcher matcher = PATTERN.matcher(sql);
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            int end = Math.min(sql.length(), matcher.end() + 45);
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "DATE_FORMAT is MySQL-only; the format string also needs a syntax rewrite.",
                    sql.substring(matcher.start(), end).replaceAll("\\s+", " ").trim(),
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
