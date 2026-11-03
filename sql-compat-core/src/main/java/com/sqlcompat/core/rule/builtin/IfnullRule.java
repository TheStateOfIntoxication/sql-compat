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
 * R001 —— MySQL 的 {@code IFNULL(a, b)} 在 Kingbase / PostgreSQL 中没有对应函数。
 *
 * <p>改写为 SQL 标准的 {@code COALESCE(a, b)}。
 */
public final class IfnullRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile("\\bifnull\\s*\\(", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R001";
    }

    @Override
    public String name() {
        return "IFNULL 在 Kingbase 中无对应函数";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "MySQL 专有的 IFNULL(a, b) 在 Kingbase/PostgreSQL 上不存在。";
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
                    "IFNULL 是 MySQL 专有函数，在 Kingbase/PostgreSQL 上执行会直接报错。",
                    sql.substring(start, end).replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT),
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
