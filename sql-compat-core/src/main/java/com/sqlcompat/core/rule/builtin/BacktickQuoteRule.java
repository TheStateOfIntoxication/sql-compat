package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.List;
import java.util.regex.Pattern;

/**
 * R009 - MySQL backtick-quoted identifiers.
 *
 * <p>Kingbase / PostgreSQL use double quotes (and fold unquoted identifiers to lower case,
 * unlike MySQL on Windows where the default is case-insensitive). A statement that keeps
 * backticks is a syntax error.
 *
 * <p>Known limitation: backticks inside a string literal also match. That is a false positive
 * by design trade-off — it is rarer than the real thing, and the report shows the snippet so a
 * human can dismiss it in one glance.
 */
public final class BacktickQuoteRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile("`[^`\\r\\n]{1,64}`");

    @Override
    public String id() {
        return "R009";
    }

    @Override
    public String name() {
        return "Backtick-quoted identifiers must use double quotes";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }

    @Override
    public String description() {
        return "MySQL quotes identifiers with backticks; Kingbase/PostgreSQL use double quotes.";
    }

    @Override
    public String suggestion() {
        return "`col` -> \"col\"   (or drop the quotes when the identifier is lower case)";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        var matcher = PATTERN.matcher(context.sql());
        List<Issue> issues = new java.util.ArrayList<>();
        while (matcher.find()) {
            String snippet = matcher.group();
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "Backtick quoting is MySQL-only syntax.",
                    snippet,
                    "`" + snippet.substring(1, snippet.length() - 1) + "` -> \""
                            + snippet.substring(1, snippet.length() - 1) + "\"",
                    context.location()));
        }
        return issues;
    }
}
