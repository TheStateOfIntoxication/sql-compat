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
 * R012 - MySQL {@code col REGEXP 'pattern'} / {@code RLIKE}.
 *
 * <p>Kingbase / PostgreSQL use the {@code ~} operator (case-sensitive) or {@code ~*}
 * (case-insensitive).
 */
public final class RegexpOperatorRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile("\\b(?:regexp|rlike)\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R012";
    }

    @Override
    public String name() {
        return "REGEXP / RLIKE must become the ~ operator";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }

    @Override
    public String description() {
        return "MySQL REGEXP / RLIKE operators do not exist on Kingbase/PostgreSQL.";
    }

    @Override
    public String suggestion() {
        return "col REGEXP 'p' -> col ~ 'p'   (case-insensitive: col ~* 'p')";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String sql = context.sql();
        Matcher matcher = PATTERN.matcher(sql);
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            int start = Math.max(0, matcher.start() - 25);
            int end = Math.min(sql.length(), matcher.end() + 25);
            String snippet = sql.substring(start, end).replaceAll("\\s+", " ").trim();
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "MySQL regex operator found: use ~ (or ~* for case-insensitive) on Kingbase/PostgreSQL.",
                    snippet,
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
