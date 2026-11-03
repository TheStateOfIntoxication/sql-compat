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
 * R002 —— MySQL 的 {@code DATE_FORMAT(col, '%Y-%m-%d')}，需要改写为 {@code TO_CHAR}。
 *
 * <p>注意函数名要换，格式串也要换（{@code %Y} → {@code YYYY}、{@code %i} → {@code MI}），
 * 这是迁移里最容易漏的一步。
 */
public final class DateFormatRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile("\\bdate_format\\s*\\(", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R002";
    }

    @Override
    public String name() {
        return "DATE_FORMAT 需改写为 TO_CHAR";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "MySQL 的 DATE_FORMAT(col, fmt) 在 Kingbase/PostgreSQL 上不存在。";
    }

    @Override
    public String suggestion() {
        return "DATE_FORMAT(col, '%Y-%m-%d') -> TO_CHAR(col, 'YYYY-MM-DD')";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String sql = context.sql();
        Matcher matcher = PATTERN.matcher(sql);
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            int end = Math.min(sql.length(), matcher.end() + 45);
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "DATE_FORMAT 是 MySQL 专有函数，且格式串语法也需一并改写。",
                    sql.substring(matcher.start(), end).replaceAll("\\s+", " ").trim(),
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
