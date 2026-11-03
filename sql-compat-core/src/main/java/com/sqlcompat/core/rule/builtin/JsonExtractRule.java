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
 * R013 - MySQL {@code JSON_EXTRACT(col, '$.key')}.
 *
 * <p>Kingbase / PostgreSQL prefer the operators {@code ->} (returns jsonb) and
 * {@code ->>} (returns text). The practical trap is picking the wrong one: {@code ->}
 * keeps quotes, so a join or equality against a plain string silently fails.
 */
public final class JsonExtractRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile("\\bjson_extract\\s*\\(", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R013";
    }

    @Override
    public String name() {
        return "JSON_EXTRACT must become -> or ->>";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }

    @Override
    public String description() {
        return "MySQL JSON_EXTRACT is not available on Kingbase/PostgreSQL.";
    }

    @Override
    public String suggestion() {
        return "JSON_EXTRACT(col, '$.k') -> col ->> 'k'  (use -> only when you need jsonb)";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String sql = context.sql();
        Matcher matcher = PATTERN.matcher(sql);
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            int end = Math.min(sql.length(), matcher.end() + 50);
            String snippet = sql.substring(matcher.start(), end).replaceAll("\\s+", " ").trim();
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "JSON_EXTRACT is MySQL-only. Pick ->> for text comparisons, -> for jsonb.",
                    snippet,
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
