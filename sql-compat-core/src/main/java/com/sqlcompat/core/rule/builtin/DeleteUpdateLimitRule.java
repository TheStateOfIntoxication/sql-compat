package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.List;
import java.util.regex.Pattern;

/**
 * R003 - MySQL allows {@code DELETE ... LIMIT n} and {@code UPDATE ... LIMIT n}.
 * Kingbase / PostgreSQL do not: use a sub-select on {@code ctid} or a key range instead.
 */
public final class DeleteUpdateLimitRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\b(delete|update)\\b[\\s\\S]{0,2000}?\\blimit\\b",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R003";
    }

    @Override
    public String name() {
        return "DELETE/UPDATE ... LIMIT is not supported";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "MySQL supports LIMIT on DELETE/UPDATE; Kingbase/PostgreSQL do not.";
    }

    @Override
    public String suggestion() {
        return "DELETE FROM t WHERE ctid IN (SELECT ctid FROM t WHERE ... LIMIT n)";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        var matcher = PATTERN.matcher(context.sql());
        if (!matcher.find()) {
            return List.of();
        }
        String keyword = matcher.group(1).toLowerCase();
        return List.of(new Issue(
                id(),
                name(),
                defaultSeverity(),
                "LIMIT is not allowed on " + keyword.toUpperCase() + " in Kingbase/PostgreSQL.",
                keyword + " ... limit",
                suggestion(),
                context.location()));
    }
}
