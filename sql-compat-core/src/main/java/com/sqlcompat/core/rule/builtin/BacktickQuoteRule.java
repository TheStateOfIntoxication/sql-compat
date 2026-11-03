package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.fix.FixSafety;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * R009 —— MySQL 的反引号标识符。
 *
 * <p>Kingbase / PostgreSQL 用双引号，且未加引号的标识符会折叠为小写（MySQL 在 Windows 下
 * 默认大小写不敏感）。保留反引号就是语法错误。
 *
 * <p>已知局限：字符串字面量里的反引号也会命中。这是有意的取舍 —— 真问题远比字面量常见，
 * 且报告会展示片段，人工一眼可排除。
 */
public final class BacktickQuoteRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile("`[^`\\r\\n]{1,64}`");

    @Override
    public String id() {
        return "R009";
    }

    @Override
    public String name() {
        return "反引号标识符需改为双引号";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }

    @Override
    public String description() {
        return "MySQL 用反引号引用标识符；Kingbase/PostgreSQL 用双引号。";
    }

    @Override
    public String suggestion() {
        return "`col` -> \"col\"（标识符本身是小写时也可以直接去掉引号）";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        var matcher = PATTERN.matcher(context.sql());
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            String snippet = matcher.group();
            String inner = snippet.substring(1, snippet.length() - 1);
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "反引号引用是 MySQL 专有语法。",
                    snippet,
                    "`" + inner + "` -> \"" + inner + "\"",
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
        // 引号替换本身无损，但 PG 的 "User" 区分大小写而 MySQL 不区分，
        // 所以这条列为需确认：改完必须确认大小写折叠策略。
        return FixSafety.REVIEW;
    }

    @Override
    public String fix(String sql) {
        return PATTERN.matcher(sql).replaceAll(match -> {
            String inner = match.group().substring(1, match.group().length() - 1);
            return "\"" + inner + "\"";
        });
    }
}