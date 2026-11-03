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
 * R006 - MySQL {@code ON DUPLICATE KEY UPDATE}.
 *
 * <p>Kingbase / PostgreSQL want {@code ON CONFLICT (cols) DO UPDATE SET ...}, and the conflict
 * target columns are <b>mandatory</b>. Also {@code VALUES(col)} inside the update clause has no
 * meaning there — it must become {@code EXCLUDED.col}.
 */
public final class UpsertRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\bon\\s+duplicate\\s+key\\s+update\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUES_REF = Pattern.compile(
            "\\bvalues\\s*\\(\\s*([A-Za-z0-9_.]+)\\s*\\)", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R006";
    }

    @Override
    public String name() {
        return "ON DUPLICATE KEY UPDATE must become ON CONFLICT ... DO UPDATE";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "MySQL upsert syntax has no counterpart on Kingbase/PostgreSQL.";
    }

    @Override
    public String suggestion() {
        return "ON DUPLICATE KEY UPDATE col = VALUES(col) -> ON CONFLICT (key) DO UPDATE SET col = EXCLUDED.col";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String sql = context.sql();
        Matcher matcher = PATTERN.matcher(sql);
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            List<String> valuesRefs = new ArrayList<>();
            Matcher refMatcher = VALUES_REF.matcher(sql.substring(matcher.end()));
            while (refMatcher.find()) {
                valuesRefs.add(refMatcher.group(1));
            }
            String message = valuesRefs.isEmpty()
                    ? "ON DUPLICATE KEY UPDATE is MySQL-only; the conflict target columns are mandatory "
                      + "in ON CONFLICT and cannot be inferred."
                    : "ON DUPLICATE KEY UPDATE is MySQL-only; VALUES(" + String.join(", ", valuesRefs)
                      + ") must become EXCLUDED." + valuesRefs.getFirst().toLowerCase(Locale.ROOT)
                      + ", and the conflict target columns must be written explicitly.";
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    message,
                    "ON DUPLICATE KEY UPDATE",
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
