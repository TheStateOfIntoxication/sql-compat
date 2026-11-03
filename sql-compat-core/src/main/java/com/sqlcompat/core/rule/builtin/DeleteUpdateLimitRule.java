package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.List;
import java.util.regex.Pattern;

/**
 * R003 —— MySQL 允许 {@code DELETE ... LIMIT n} / {@code UPDATE ... LIMIT n}。
 *
 * <p>Kingbase / PostgreSQL 不支持（语法直接报错），需改成用 {@code ctid} 子查询或主键区间定位。
 */
public final class DeleteUpdateLimitRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\b(delete|update)\\b[\\s\\S]{0,2000}?\\blimit\\b",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R003";
    }

    @Override
    public String name() {
        return "DELETE/UPDATE 不支持 LIMIT";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "MySQL 支持在 DELETE/UPDATE 上带 LIMIT；Kingbase/PostgreSQL 不支持。";
    }

    @Override
    public String suggestion() {
        return "DELETE FROM t WHERE ctid IN (SELECT ctid FROM t WHERE ... LIMIT n)";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        var matcher = PATTERN.matcher(context.sql());
        if (!matcher.find()) {
            return List.of();
        }
        String keyword = matcher.group(1).toLowerCase();
        return List.of(new Issue(
                id(),
                name(),
                defaultSeverity(),
                keyword.toUpperCase() + " 语句带 LIMIT，在 Kingbase/PostgreSQL 上是语法错误。",
                keyword + " ... limit",
                suggestion(),
                context.location()));
    }
}
