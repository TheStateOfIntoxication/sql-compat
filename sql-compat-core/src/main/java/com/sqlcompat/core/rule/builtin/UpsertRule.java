package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * R006 —— MySQL 的 {@code ON DUPLICATE KEY UPDATE}。
 *
 * <p>Kingbase / PostgreSQL 需写成 {@code ON CONFLICT (cols) DO UPDATE SET ...}，其中
 * <b>冲突目标列是强制的</b>；同时 {@code VALUES(col)} 在那边没有意义，要换成 {@code EXCLUDED.col}。
 */
public final class UpsertRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\bon\\s+duplicate\\s+key\\s+update\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUES_REF = Pattern.compile(
            "\\bvalues\\s*\\(\\s*([A-Za-z0-9_.]+)\\s*\\)", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R006";
    }

    @Override
    public String name() {
        return "ON DUPLICATE KEY UPDATE 需改为 ON CONFLICT ... DO UPDATE";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "MySQL 的 upsert 语法在 Kingbase/PostgreSQL 上没有对应写法。";
    }

    @Override
    public String suggestion() {
        return "ON DUPLICATE KEY UPDATE col = VALUES(col) -> ON CONFLICT (key) DO UPDATE SET col = EXCLUDED.col";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String sql = context.sql();
        Matcher matcher = PATTERN.matcher(sql);
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            List<String> valuesRefs = new ArrayList<>();
            Matcher refMatcher = VALUES_REF.matcher(sql.substring(matcher.end()));
            while (refMatcher.find()) {
                valuesRefs.add(refMatcher.group(1));
            }
            String message = valuesRefs.isEmpty()
                    ? "ON DUPLICATE KEY UPDATE 是 MySQL 专有；ON CONFLICT 的冲突目标列是强制的，无法自动推断。"
                    : "ON DUPLICATE KEY UPDATE 是 MySQL 专有；VALUES(" + String.join(", ", valuesRefs)
                      + ") 要改成 EXCLUDED." + valuesRefs.get(0).toLowerCase(Locale.ROOT)
                      + "，并且必须显式写出冲突目标列。";
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    message,
                    "ON DUPLICATE KEY UPDATE",
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
