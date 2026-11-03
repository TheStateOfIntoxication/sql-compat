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
 * R015 —— MySQL 的 {@code DATETIME ... ON UPDATE CURRENT_TIMESTAMP}。
 *
 * <p>Kingbase / PostgreSQL 没有列级的 on update 子句，行为要挪到触发器或应用层。
 * 对 MyBatis 代码库来说放应用层通常更合适，但这必须是个决定 —— 迁移后这一列会**默默停止更新**，
 * 而语句本身照常成功，几个月后才有人发现。
 */
public final class OnUpdateTimestampRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\bon\\s+update\\s+(?:current_timestamp(?:\\(\\s*\\d*\\s*\\))?|now\\s*\\(\\s*\\))",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "R015";
    }

    @Override
    public String name() {
        return "ON UPDATE CURRENT_TIMESTAMP 没有列级等价写法";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }

    @Override
    public String description() {
        return "列级 ON UPDATE CURRENT_TIMESTAMP 是 MySQL 专有，需改触发器或应用层维护。";
    }

    @Override
    public String suggestion() {
        return "去掉 ON UPDATE CURRENT_TIMESTAMP，在应用层写 updated_at，或建 BEFORE UPDATE 触发器";
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
                    "迁移后这一列会默默停止更新 —— 语句照常成功，所以特别容易漏。",
                    matcher.group(),
                    suggestion(),
                    context.location()));
        }
        return issues;
    }
}
