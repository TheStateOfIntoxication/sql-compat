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
 * R014 —— 当作布尔用的 MySQL {@code TINYINT(1)}。
 *
 * <p>Kingbase / PostgreSQL 有真正的 {@code BOOLEAN}。迁移陷阱在于显示宽度：
 * {@code TINYINT(4)} 是数字，{@code TINYINT(1)} 是事实上的布尔；一旦粗放地
 * {@code TINYINT → SMALLINT}，数据还在，但 JDBC {@code getBoolean()} 与 ORM 的布尔映射会失效。
 */
public final class TinyIntBooleanRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\btinyint\\s*\\(\\s*1\\s*\\)", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R014";
    }

    @Override
    public String name() {
        return "TINYINT(1) 承载布尔语义";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MINOR;
    }

    @Override
    public String description() {
        return "TINYINT(1) 是 MySQL 事实上的布尔，应映射为 BOOLEAN 而不是 SMALLINT。";
    }

    @Override
    public String suggestion() {
        return "flag TINYINT(1) -> flag BOOLEAN（不要映射成 SMALLINT）";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        Matcher matcher = PATTERN.matcher(context.sql());
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "TINYINT(1) 被当作布尔使用。映射成 SMALLINT 虽然能存数据，"
                            + "但会破坏 JDBC getBoolean() / ORM 的布尔映射。",
                    matcher.group(),
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
