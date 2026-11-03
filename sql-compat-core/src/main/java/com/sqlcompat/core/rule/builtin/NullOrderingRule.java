package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.List;
import java.util.regex.Pattern;

/**
 * R017 —— {@code ORDER BY col DESC} 叠加 {@code LIMIT} 时 NULL 的位置不同。
 *
 * <p>MySQL 认为 NULL 最小（ASC 在前、DESC 在后）；PostgreSQL 认为 NULL 最大
 * （ASC 在后、DESC 在前）。普通列表没人注意，但分页时它决定**第一页是哪几条**。
 *
 * <p>范围刻意限制在同时带 {@code LIMIT} 的语句：对每个 {@code ORDER BY ... DESC} 都报会让报告
 * 变成噪音，而没被截断的结果里这个差异并不构成实际问题。
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
        return "ORDER BY DESC + LIMIT：NULL 落在不同页";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MINOR;
    }

    @Override
    public String description() {
        return "MySQL 与 PostgreSQL 在 DESC 排序下对 NULL 的处理方向相反。";
    }

    @Override
    public String suggestion() {
        return "显式写出：ORDER BY col DESC NULLS LAST（或 NULLS FIRST）";
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
                "DESC 排序叠加 LIMIT：MySQL 把 NULL 排在最后，PostgreSQL 排在最前。"
                        + "迁移后第一页的记录可能换人。",
                matcher.group().replaceAll("\\s+", " ").trim(),
                suggestion(),
                context.location()));
    }
}
