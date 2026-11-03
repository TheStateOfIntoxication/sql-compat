package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * R001 - MySQL {@code IFNULL(a, b)} has no counterpart in Kingbase / PostgreSQL.
 *
 * <p>Rewrite with the SQL standard {@code COALESCE(a, b)}.
 */
public final class IfnullRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile("\\bifnull\\s*\\(", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R001";
    }

    @Override
    public String name() {
        return "IFNULL has no Kingbase equivalent";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "MySQL-only IFNULL(a, b) is not available on Kingbase/PostgreSQL.";
    }

    @Override
    public String suggestion() {
        return "IFNULL(a, b) -> COALESCE(a, b)";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String sql = context.sql();
        Matcher matcher = PATTERN.matcher(sql);
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            int start = Math.max(0, matcher.start() - 10);
            int end = Math.min(sql.length(), matcher.end() + 40);
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "IFNULL is MySQL-only and will fail to execute on Kingbase/PostgreSQL.",
                    sql.substring(start, end).replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT),
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
