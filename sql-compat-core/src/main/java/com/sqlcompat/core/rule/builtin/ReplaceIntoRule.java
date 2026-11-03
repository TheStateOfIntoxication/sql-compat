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
 * R019 —— MySQL 的 {@code REPLACE INTO t ...}。
 *
 * <p>Kingbase / PostgreSQL 没有 REPLACE INTO，但它也不是 upsert 的同义词：MySQL 的 REPLACE
 * 是**先删后插**，触发器会触发两次、自增主键会变。改写成 ON CONFLICT 是行为变更
 * —— 通常是想要的结果，但必须是个决定，而不是顺手替换。
 */
public final class ReplaceIntoRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\breplace\\s+into\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R019";
    }

    @Override
    public String name() {
        return "REPLACE INTO 需改为 ON CONFLICT（或 MERGE）";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }

    @Override
    public String description() {
        return "REPLACE INTO 是 MySQL 专有，且语义为删后插。";
    }

    @Override
    public String suggestion() {
        return "REPLACE INTO t (..) VALUES (..) -> INSERT INTO t (..) VALUES (..) ON CONFLICT (key) DO UPDATE SET ...";
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
                    "Kingbase/PostgreSQL 没有 REPLACE INTO，且它的先删后插语义与 ON CONFLICT 不同"
                            + "（触发器次数、自增主键都会变）。",
                    "REPLACE INTO",
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
