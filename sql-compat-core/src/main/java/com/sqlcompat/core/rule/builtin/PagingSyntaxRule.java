package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.fix.FixSafety;
import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * R004 —— MySQL 的分页语法 {@code LIMIT offset, size}。
 *
 * <p>Kingbase / PostgreSQL 只认 {@code LIMIT size OFFSET offset}，逗号形式是语法错误。
 * 这是分页插件按 MySQL 配置时最常见的阻塞项。
 *
 * <p>正则同时匹配占位符（{@code LIMIT #{offset}, #{pageSize}}），因为检查发生在参数替换之前。
 */
public final class PagingSyntaxRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\blimit\\s+([^\\s,;()]+)\\s*,\\s*([^\\s,;()]+)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R004";
    }

    @Override
    public String name() {
        return "LIMIT offset, size 需改为 LIMIT size OFFSET offset";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "MySQL 允许 LIMIT 的逗号形式；Kingbase/PostgreSQL 不允许。";
    }

    @Override
    public String suggestion() {
        return "LIMIT 10, 20 -> LIMIT 20 OFFSET 10（注意两个参数位置互换）";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        Matcher matcher = PATTERN.matcher(context.sql());
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            String offset = matcher.group(1);
            String size = matcher.group(2);
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "LIMIT 的逗号形式在 Kingbase/PostgreSQL 上是语法错误。",
                    "LIMIT " + offset + ", " + size,
                    "LIMIT " + size + " OFFSET " + offset,
                    context.location()));
        }
        return issues;
    }

    @Override
    public boolean fixable() {
        return true;
    }

    @Override
    public FixSafety fixSafety() {
        return FixSafety.SAFE;
    }

    @Override
    public String fix(String sql) {
        // 参数位置互换，语义不变。
        return PATTERN.matcher(sql).replaceAll(match ->
                "LIMIT " + match.group(2) + " OFFSET " + match.group(1));
    }
}
