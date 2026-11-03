package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * R020 - MySQL date arithmetic and date parsing functions.
 *
 * <p>{@code DATE_ADD} / {@code DATE_SUB} / {@code STR_TO_DATE} / {@code UNIX_TIMESTAMP} do not
 * exist on Kingbase / PostgreSQL. They are the most common leftovers in reporting SQL, and they
 * are easy to miss because the statements around them are otherwise portable.
 */
public final class DateFunctionRule implements DialectRule {

    private static final Map<String, String> REWRITES = new LinkedHashMap<>(Map.of(
            "date_add", "DATE_ADD(col, INTERVAL 1 DAY) -> col + INTERVAL '1 day'",
            "date_sub", "DATE_SUB(col, INTERVAL 1 DAY) -> col - INTERVAL '1 day'",
            "str_to_date", "STR_TO_DATE(s, '%Y-%m-%d') -> TO_DATE(s, 'YYYY-MM-DD')",
            "unix_timestamp", "UNIX_TIMESTAMP() -> EXTRACT(EPOCH FROM now())"
    ));

    private static final Pattern PATTERN = Pattern.compile(
            "\\b(date_add|date_sub|str_to_date|unix_timestamp)\\s*\\(",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R020";
    }

    @Override
    public String name() {
        return "MySQL date functions have no equivalent";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "DATE_ADD / DATE_SUB / STR_TO_DATE / UNIX_TIMESTAMP are MySQL-only.";
    }

    @Override
    public String suggestion() {
        return String.join("; ", REWRITES.values());
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String sql = context.sql();
        Matcher matcher = PATTERN.matcher(sql);
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            String function = matcher.group(1).toLowerCase(java.util.Locale.ROOT);
            int end = Math.min(sql.length(), matcher.end() + 50);
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    function.toUpperCase(java.util.Locale.ROOT) + " is MySQL-only; the interval "
                            + "and format-string syntax differs on Kingbase/PostgreSQL.",
                    sql.substring(matcher.start(), end).replaceAll("\\s+", " ").trim(),
                    REWRITES.get(function),
                    context.location()));
        }
        return issues;
    }
}
