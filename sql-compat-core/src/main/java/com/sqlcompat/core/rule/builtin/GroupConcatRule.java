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
 * R008 - MySQL {@code GROUP_CONCAT(col ORDER BY x SEPARATOR ',')}.
 *
 * <p>Kingbase / PostgreSQL use {@code STRING_AGG(col, ',')}, and the ordering must move
 * into the aggregate's own {@code ORDER BY} clause.
 */
public final class GroupConcatRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile("\\bgroup_concat\\s*\\(", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R008";
    }

    @Override
    public String name() {
        return "GROUP_CONCAT must become STRING_AGG";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "MySQL GROUP_CONCAT has no equivalent name on Kingbase/PostgreSQL.";
    }

    @Override
    public String suggestion() {
        return "GROUP_CONCAT(col ORDER BY x SEPARATOR ',') -> STRING_AGG(col, ',' ORDER BY x)";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String sql = context.sql();
        Matcher matcher = PATTERN.matcher(sql);
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            int end = Math.min(sql.length(), matcher.end() + 60);
            String snippet = sql.substring(matcher.start(), end).replaceAll("\\s+", " ").trim();
            // The separator is a keyword in MySQL but a positional argument in PostgreSQL.
            boolean hasSeparator = snippet.toLowerCase().contains("separator");
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    hasSeparator
                            ? "GROUP_CONCAT is MySQL-only; SEPARATOR ',' becomes the 2nd STRING_AGG argument."
                            : "GROUP_CONCAT is MySQL-only; STRING_AGG requires an explicit delimiter (default ',').",
                    snippet,
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
