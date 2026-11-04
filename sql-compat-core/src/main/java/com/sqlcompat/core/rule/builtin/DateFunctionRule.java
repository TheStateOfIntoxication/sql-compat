package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.ArrayList;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.sqlcompat.core.util.J8;

/**
 * R020 —— MySQL 的日期运算与日期解析函数。
 *
 * <p>{@code DATE_ADD} / {@code DATE_SUB} / {@code STR_TO_DATE} / {@code UNIX_TIMESTAMP}
 * 在 Kingbase / PostgreSQL 上都不存在。它们是报表 SQL 里最常见的遗留，而且周围的语句往往都可移植，
 * 所以特别容易漏。
 */
public final class DateFunctionRule implements DialectRule {

    private static final Map<String, String> REWRITES = J8.stringMap(
            "date_add", "DATE_ADD(col, INTERVAL 1 DAY) -> col + INTERVAL '1 day'",
            "date_sub", "DATE_SUB(col, INTERVAL 1 DAY) -> col - INTERVAL '1 day'",
            "str_to_date", "STR_TO_DATE(s, '%Y-%m-%d') -> TO_DATE(s, 'YYYY-MM-DD')",
            "unix_timestamp", "UNIX_TIMESTAMP() -> EXTRACT(EPOCH FROM now())");

    private static final Pattern PATTERN = Pattern.compile(
            "\\b(date_add|date_sub|str_to_date|unix_timestamp)\\s*\\(",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R020";
    }

    @Override
    public String name() {
        return "MySQL 日期函数没有对应实现";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "DATE_ADD / DATE_SUB / STR_TO_DATE / UNIX_TIMESTAMP 是 MySQL 专有函数。";
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
            String function = matcher.group(1).toLowerCase(Locale.ROOT);
            int end = Math.min(sql.length(), matcher.end() + 50);
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    function.toUpperCase(Locale.ROOT) + " 是 MySQL 专有；区间与格式串语法在 "
                            + "Kingbase/PostgreSQL 上都不同。",
                    sql.substring(matcher.start(), end).replaceAll("\\s+", " ").trim(),
                    REWRITES.get(function),
                    context.location()));
        }
        return issues;
    }
}
