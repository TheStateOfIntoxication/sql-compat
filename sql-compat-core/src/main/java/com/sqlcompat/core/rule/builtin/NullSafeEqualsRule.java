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
 * R016 —— MySQL 的 NULL 安全等于运算符 {@code <=>}。
 *
 * <p>MySQL 用 {@code a <=> b} 表示"NULL 等于 NULL 也算相等"。Kingbase / PostgreSQL 用
 * SQL 标准的 {@code a IS NOT DISTINCT FROM b}，没有运算符形式，所以是解析期报错
 * —— 但这种表达式常常藏在很长的 {@code <if>} 块里，很容易被漏掉。
 */
public final class NullSafeEqualsRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile("<=>");

    @Override
    public String id() {
        return "R016";
    }

    @Override
    public String name() {
        return "<=> 需改为 IS NOT DISTINCT FROM";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "MySQL 的 NULL 安全等于 <=> 在 Kingbase/PostgreSQL 上没有运算符等价形式。";
    }

    @Override
    public String suggestion() {
        return "a <=> b -> a IS NOT DISTINCT FROM b";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String sql = context.sql();
        Matcher matcher = PATTERN.matcher(sql);
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            int start = Math.max(0, matcher.start() - 30);
            int end = Math.min(sql.length(), matcher.end() + 30);
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "MySQL 的 NULL 安全等于运算符在 Kingbase/PostgreSQL 上不存在。",
                    sql.substring(start, end).replaceAll("\\s+", " ").trim(),
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
