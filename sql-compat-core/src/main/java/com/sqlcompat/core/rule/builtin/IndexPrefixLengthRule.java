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
 * R018 —— MySQL 的索引前缀长度，例如 {@code KEY idx_name (name(10))}。
 *
 * <p>Kingbase / PostgreSQL 不能只索引某列的前缀。常见替代是表达式索引 {@code left(col, 10)}，
 * 而该索引只有在查询用**同一个表达式**时才会命中，所以 SQL 也得一起改。
 * 这是"只改 DDL 不够"的典型场景。
 */
public final class IndexPrefixLengthRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\b(?:key|index)\\s+`?(\\w+)`?\\s*\\(\\s*`?(\\w+)`?\\s*\\(\\s*(\\d+)\\s*\\)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R018";
    }

    @Override
    public String name() {
        return "索引前缀长度不支持";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }

    @Override
    public String description() {
        return "MySQL 可以只索引列的前缀；Kingbase/PostgreSQL 不支持。";
    }

    @Override
    public String suggestion() {
        return "KEY idx (col(10)) -> CREATE INDEX idx ON t (left(col, 10))（查询也要用同一个表达式）";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        Matcher matcher = PATTERN.matcher(context.sql());
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            String indexName = matcher.group(1);
            String column = matcher.group(2);
            String length = matcher.group(3);
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "列 \"" + column + "\" 上的前缀索引没有直接等价写法；表达式索引只有在查询使用同一表达式时才生效。",
                    matcher.group().replaceAll("\\s+", " ").trim(),
                    "CREATE INDEX " + indexName + " ON <table> (left(" + column + ", " + length + "))",
                    context.location()));
        }
        return issues;
    }
}
