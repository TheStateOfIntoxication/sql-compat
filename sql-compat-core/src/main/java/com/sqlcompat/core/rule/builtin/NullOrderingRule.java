package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.List;
import java.util.regex.Pattern;

/**
 * R017 - {@code ORDER BY col DESC} combined with {@code LIMIT}: NULL placement differs.
 *
 * <p>MySQL sorts NULLs first on ASC and last on DESC. PostgreSQL defaults to the opposite
 * (NULLs are treated as larger than any value, so they come last on ASC and first on DESC).
 * For a plain listing nobody notices; for a paged list it changes <b>which rows land on page 1</b>.
 *
 * <p>Scope is deliberately limited to statements that also use {@code LIMIT}. Firing on every
 * {@code ORDER BY ... DESC} would bury the report in noise for a problem that only matters when
 * the result is truncated.
 */
public final class NullOrderingRule implements DialectRule {

    private static final Pattern ORDER_BY_DESC = Pattern.compile(
            "\\border\\s+by\\b[\\s\\S]{0,300}?\\bdesc\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R017";
    }

    @Override
    public String name() {
        return "ORDER BY DESC + LIMIT: NULLs land on a different page";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MINOR;
    }

    @Override
    public String description() {
        return "MySQL and PostgreSQL place NULLs differently in DESC order.";
    }

    @Override
    public String suggestion() {
        return "make it explicit: ORDER BY col DESC NULLS LAST (or NULLS FIRST)";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String lower = context.lowerCaseSql();
        if (!lower.contains("limit") || !lower.contains("order by")) {
            return List.of();
        }
        var matcher = ORDER_BY_DESC.matcher(context.sql());
        if (!matcher.find()) {
            return List.of();
        }
        return List.of(new Issue(
                id(),
                name(),
                defaultSeverity(),
                "DESC ordering with LIMIT: MySQL puts NULLs last, PostgreSQL puts them first. "
                        + "Rows on the first page can differ after migration.",
                matcher.group().replaceAll("\\s+", " ").trim(),
                suggestion(),
                context.location()));
    }
}
