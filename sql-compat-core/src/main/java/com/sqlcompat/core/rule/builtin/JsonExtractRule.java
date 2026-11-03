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
 * R013 —— MySQL 的 {@code JSON_EXTRACT(col, '$.key')}。
 *
 * <p>Kingbase / PostgreSQL 用运算符 {@code ->}（返回 jsonb）和 {@code ->>}（返回文本）。
 * 实际踩坑点在于选错：{@code ->} 会保留引号，拿它和普通字符串做等值比较**不会报错但永远不相等**。
 */
public final class JsonExtractRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile("\\bjson_extract\\s*\\(", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R013";
    }

    @Override
    public String name() {
        return "JSON_EXTRACT 需改写为 -> 或 ->>";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }

    @Override
    public String description() {
        return "MySQL 的 JSON_EXTRACT 在 Kingbase/PostgreSQL 上不可用。";
    }

    @Override
    public String suggestion() {
        return "JSON_EXTRACT(col, '$.k') -> col ->> 'k'（需要 jsonb 时才用 ->）";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String sql = context.sql();
        Matcher matcher = PATTERN.matcher(sql);
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            int end = Math.min(sql.length(), matcher.end() + 50);
            String snippet = sql.substring(matcher.start(), end).replaceAll("\\s+", " ").trim();
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "JSON_EXTRACT 是 MySQL 专有。做文本比较用 ->>，需要 jsonb 类型用 ->。",
                    snippet,
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
