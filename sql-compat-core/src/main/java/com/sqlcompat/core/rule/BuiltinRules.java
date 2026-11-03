package com.sqlcompat.core.rule;

import com.sqlcompat.core.rule.builtin.AutoIncrementRule;
import com.sqlcompat.core.rule.builtin.BacktickQuoteRule;
import com.sqlcompat.core.rule.builtin.DateFormatRule;
import com.sqlcompat.core.rule.builtin.DeleteUpdateLimitRule;
import com.sqlcompat.core.rule.builtin.ForeachBindingRule;
import com.sqlcompat.core.rule.builtin.GroupConcatRule;
import com.sqlcompat.core.rule.builtin.IfnullRule;
import com.sqlcompat.core.rule.builtin.IndexPrefixLengthRule;
import com.sqlcompat.core.rule.builtin.InsertIgnoreRule;
import com.sqlcompat.core.rule.builtin.JsonExtractRule;
import com.sqlcompat.core.rule.builtin.OnUpdateTimestampRule;
import com.sqlcompat.core.rule.builtin.PagingSyntaxRule;
import com.sqlcompat.core.rule.builtin.RegexpOperatorRule;
import com.sqlcompat.core.rule.builtin.ReplaceIntoRule;
import com.sqlcompat.core.rule.builtin.TinyIntBooleanRule;
import com.sqlcompat.core.rule.builtin.UpsertRule;

import java.util.List;

/**
 * Built-in rule catalogue.
 *
 * <p>Using an explicit list (instead of ServiceLoader) for built-ins keeps the first run
 * deterministic and avoids classpath surprises in IDE sandboxes. Register new built-in
 * rules here; {@link RuleRegistry} sorts them by id, so insertion order does not matter.
 */
final class BuiltinRules {

    private BuiltinRules() {
    }

    static List<DialectRule> all() {
        return List.of(
                // batch 1 - functions
                new IfnullRule(),
                new DateFormatRule(),
                new GroupConcatRule(),
                new RegexpOperatorRule(),
                new JsonExtractRule(),
                // batch 2 - DML / DDL syntax
                new DeleteUpdateLimitRule(),
                new PagingSyntaxRule(),
                new InsertIgnoreRule(),
                new UpsertRule(),
                new AutoIncrementRule(),
                new ReplaceIntoRule(),
                // batch 3 - identifiers / MyBatis / types
                new BacktickQuoteRule(),
                new ForeachBindingRule(),
                new TinyIntBooleanRule(),
                new OnUpdateTimestampRule(),
                new IndexPrefixLengthRule()
        );
    }
}
