package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.List;

/**
 * R010 - {@code LIMIT} without {@code ORDER BY}.
 *
 * <p>Not a syntax error — which is exactly why it is dangerous. Without an {@code ORDER BY} the
 * engine may return any rows, and MySQL and PostgreSQL genuinely pick different ones (different
 * access paths, different physical order). A page that looks correct on MySQL can silently show
 * different records after migration, and nothing in the logs will say so.
 *
 * <p>This rule is intentionally narrow: it only fires when the statement has a {@code LIMIT} and
 * no {@code ORDER BY} at all. A sub-query that orders internally still counts as "ordered" here,
 * which is a known simplification.
 */
public final class LimitWithoutOrderByRule implements DialectRule {

    @Override
    public String id() {
        return "R010";
    }

    @Override
    public String name() {
        return "LIMIT without ORDER BY returns engine-dependent rows";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MINOR;
    }

    @Override
    public String description() {
        return "Paging without ORDER BY has no defined row order and differs across engines.";
    }

    @Override
    public String suggestion() {
        return "add a deterministic ORDER BY (unique key) before LIMIT";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String lower = context.lowerCaseSql();
        if (!lower.contains("limit") || lower.contains("order by")) {
            return List.of();
        }
        return List.of(new Issue(
                id(),
                name(),
                defaultSeverity(),
                "This statement pages with LIMIT but has no ORDER BY. Row order is undefined and "
                        + "differs between MySQL and Kingbase/PostgreSQL — the same page can return "
                        + "different records, without any error.",
                "LIMIT without ORDER BY",
                suggestion(),
                context.location()));
    }
}
