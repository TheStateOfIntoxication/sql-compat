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
 * R005 —— MySQL 的 {@code INSERT IGNORE INTO}。
 *
 * <p>Kingbase / PostgreSQL 需改写为 {@code INSERT ... ON CONFLICT DO NOTHING}。
 * 但两者语义并不等价：MySQL 的 INSERT IGNORE 会把数据错误（截断、NOT NULL）降级成告警继续执行，
 * ON CONFLICT 只处理冲突。改写后原本被静默吞掉的错误会变成失败。
 */
public final class InsertIgnoreRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\binsert\\s+ignore\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R005";
    }

    @Override
    public String name() {
        return "INSERT IGNORE 需改为 ON CONFLICT DO NOTHING";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "INSERT IGNORE 是 MySQL 专有语法。";
    }

    @Override
    public String suggestion() {
        return "INSERT IGNORE INTO t (..) VALUES (..) -> INSERT INTO t (..) VALUES (..) ON CONFLICT DO NOTHING";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        Matcher matcher = PATTERN.matcher(context.sql());
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "INSERT IGNORE 是 MySQL 专有；它还会吞掉数据错误，而 ON CONFLICT 不会 —— 改写前先确认原意。",
                    "INSERT IGNORE",
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
