package com.sqlcompat.core.rule;

import com.sqlcompat.core.rule.builtin.AutoIncrementRule;
import com.sqlcompat.core.rule.builtin.BacktickQuoteRule;
import com.sqlcompat.core.rule.builtin.DateFormatRule;
import com.sqlcompat.core.rule.builtin.DateFunctionRule;
import com.sqlcompat.core.rule.builtin.DeleteUpdateLimitRule;
import com.sqlcompat.core.rule.builtin.ForeachBindingRule;
import com.sqlcompat.core.rule.builtin.GroupConcatRule;
import com.sqlcompat.core.rule.builtin.IfnullRule;
import com.sqlcompat.core.rule.builtin.IndexPrefixLengthRule;
import com.sqlcompat.core.rule.builtin.InsertIgnoreRule;
import com.sqlcompat.core.rule.builtin.JsonExtractRule;
import com.sqlcompat.core.rule.builtin.LimitWithoutOrderByRule;
import com.sqlcompat.core.rule.builtin.NullOrderingRule;
import com.sqlcompat.core.rule.builtin.NullSafeEqualsRule;
import com.sqlcompat.core.rule.builtin.OnUpdateTimestampRule;
import com.sqlcompat.core.rule.builtin.PagingSyntaxRule;
import com.sqlcompat.core.rule.builtin.RegexpOperatorRule;
import com.sqlcompat.core.rule.builtin.ReplaceIntoRule;
import com.sqlcompat.core.rule.builtin.TinyIntBooleanRule;
import com.sqlcompat.core.rule.builtin.UpsertRule;

import java.util.List;

/**
 * 内置规则清单。
 *
 * <p>内置规则用显式列表而不是 ServiceLoader，是为了保证首次运行结果确定，也避免 IDE 沙箱里
 * 类路径不一致带来的意外。新增内置规则在此登记即可；{@link RuleRegistry} 会按编号排序，
 * 登记顺序不影响报告顺序。
 */
final class BuiltinRules {

    private BuiltinRules() {
    }

    static List<DialectRule> all() {
        return List.of(
                // 批 1 · 函数类
                new IfnullRule(),
                new DateFormatRule(),
                new GroupConcatRule(),
                new RegexpOperatorRule(),
                new JsonExtractRule(),
                // 批 2 · DML / DDL 语法
                new DeleteUpdateLimitRule(),
                new PagingSyntaxRule(),
                new InsertIgnoreRule(),
                new UpsertRule(),
                new AutoIncrementRule(),
                new ReplaceIntoRule(),
                // 批 3 · 标识符 / MyBatis / 类型
                new BacktickQuoteRule(),
                new ForeachBindingRule(),
                new TinyIntBooleanRule(),
                new OnUpdateTimestampRule(),
                new IndexPrefixLengthRule(),
                // 批 4 · 语义 / 边界
                new LimitWithoutOrderByRule(),
                new NullSafeEqualsRule(),
                new NullOrderingRule(),
                new DateFunctionRule()
        );
    }
}
