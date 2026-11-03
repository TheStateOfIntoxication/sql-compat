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
 * R008 —— MySQL 的 {@code GROUP_CONCAT(col ORDER BY x SEPARATOR ',')}。
 *
 * <p>Kingbase / PostgreSQL 用 {@code STRING_AGG(col, ',')}，且排序列要挪到聚合函数自己的
 * {@code ORDER BY} 子句里；分隔符从关键字变成第二个位置参数。
 */
public final class GroupConcatRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile("\\bgroup_concat\\s*\\(", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R008";
    }

    @Override
    public String name() {
        return "GROUP_CONCAT 需改写为 STRING_AGG";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "MySQL 的 GROUP_CONCAT 在 Kingbase/PostgreSQL 上没有同名函数。";
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
            // SEPARATOR 在 MySQL 里是关键字，在 PostgreSQL 里是第二个位置参数。
            boolean hasSeparator = snippet.toLowerCase().contains("separator");
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    hasSeparator
                            ? "GROUP_CONCAT 是 MySQL 专有；SEPARATOR ',' 要变成 STRING_AGG 的第二个参数。"
                            : "GROUP_CONCAT 是 MySQL 专有；STRING_AGG 必须显式给出分隔符（通常为 ','）。",
                    snippet,
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
