package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.List;
import com.sqlcompat.core.util.J8;

/**
 * R011 —— MyBatis {@code <foreach>} 生成集合参数带来的差异。
 *
 * <p>本规则不检查 SQL 正文，而是标记**含有** {@code <foreach>} 的语句，因为迁移后有三处需要人工确认：
 *
 * <ul>
 *   <li>空集合会生成 {@code IN ()} —— 两个方言都是语法错误，但 MySQL 在少数上下文里能容忍，
 *       PostgreSQL 永远不忍</li>
 *   <li>批量插入通常和 {@code INSERT IGNORE} / {@code ON DUPLICATE KEY} 一起出现（见 R005 / R006）</li>
 *   <li>PostgreSQL 单语句绑定参数上限 65535，MySQL 侧没有这个限制</li>
 * </ul>
 *
 * <p>定为 MINOR 而不是 BLOCKER：规则无法知道集合大小与否为空，这是"请复核"标记，不是缺陷判定。
 */
public final class ForeachBindingRule implements DialectRule {

    @Override
    public String id() {
        return "R011";
    }

    @Override
    public String name() {
        return "<foreach> 拼接结果需人工复核";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MINOR;
    }

    @Override
    public String description() {
        return "含 <foreach> 的语句，迁移后需复核空集合与参数上限。";
    }

    @Override
    public String suggestion() {
        return "守住空集合（IN (NULL) 或用 <if> 包一层），批量操作的单批行数控制在 65535 以内";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        if (!context.statement().hasDynamicTag("foreach")) {
            return J8.list();
        }
        return J8.list(new Issue(
                id(),
                name(),
                defaultSeverity(),
                "该语句用 <foreach> 拼接列表。请复核两件事：空集合会生成 IN ()（语法错误），"
                        + "以及批量操作是否超过 PostgreSQL 单语句 65535 个绑定参数的上限。",
                "<foreach>",
                suggestion(),
                context.location()));
    }
}
