package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.List;
import com.sqlcompat.core.util.J8;

/**
 * R010 —— 带 {@code LIMIT} 却没有 {@code ORDER BY}。
 *
 * <p>不是语法错误，这正是它危险的原因。没有 {@code ORDER BY} 时行序未定义，而 MySQL 与
 * PostgreSQL 的访问路径和物理行序本来就不同：一条在 MySQL 上看着正确的分页，迁移后会默默
 * 返回别的记录，日志里没有任何痕迹。
 *
 * <p>规则范围刻意收窄：只在同时具备 {@code LIMIT} 与**完全没有** {@code ORDER BY} 时触发。
 * 子查询内部有排序也算"有排序"，这是已知简化。
 */
public final class LimitWithoutOrderByRule implements DialectRule {

    @Override
    public String id() {
        return "R010";
    }

    @Override
    public String name() {
        return "LIMIT 无 ORDER BY，返回行序由引擎决定";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MINOR;
    }

    @Override
    public String description() {
        return "无 ORDER BY 的分页行序未定义，不同引擎结果不同。";
    }

    @Override
    public String suggestion() {
        return "在 LIMIT 之前补一个确定的 ORDER BY（唯一键）";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String lower = context.lowerCaseSql();
        if (!lower.contains("limit") || lower.contains("order by")) {
            return J8.list();
        }
        return J8.list(new Issue(
                id(),
                name(),
                defaultSeverity(),
                "该语句用 LIMIT 分页但没有 ORDER BY。行序未定义，MySQL 与 Kingbase/PostgreSQL "
                        + "返回的记录会不同 —— 同一页可能换人，而且不会报错。",
                "LIMIT without ORDER BY",
                suggestion(),
                context.location()));
    }
}
