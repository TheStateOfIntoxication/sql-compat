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
 * R012 —— MySQL 的 {@code col REGEXP 'pattern'} / {@code RLIKE}。
 *
 * <p>Kingbase / PostgreSQL 用 {@code ~} 运算符（区分大小写），不区分大小写用 {@code ~*}。
 */
public final class RegexpOperatorRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile("\\b(?:regexp|rlike)\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R012";
    }

    @Override
    public String name() {
        return "REGEXP / RLIKE 需改为 ~ 运算符";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }

    @Override
    public String description() {
        return "MySQL 的 REGEXP / RLIKE 运算符在 Kingbase/PostgreSQL 上不存在。";
    }

    @Override
    public String suggestion() {
        return "col REGEXP 'p' -> col ~ 'p'（不区分大小写用 col ~* 'p'）";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String sql = context.sql();
        Matcher matcher = PATTERN.matcher(sql);
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            int start = Math.max(0, matcher.start() - 25);
            int end = Math.min(sql.length(), matcher.end() + 25);
            String snippet = sql.substring(start, end).replaceAll("\\s+", " ").trim();
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "检测到 MySQL 正则运算符；Kingbase/PostgreSQL 请改用 ~（区分大小写）或 ~*（不区分大小写）。",
                    snippet,
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
