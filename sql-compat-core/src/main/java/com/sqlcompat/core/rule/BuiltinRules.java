package com.sqlcompat.core.rule;

import com.sqlcompat.core.rule.builtin.DateFormatRule;
import com.sqlcompat.core.rule.builtin.DeleteUpdateLimitRule;
import com.sqlcompat.core.rule.builtin.GroupConcatRule;
import com.sqlcompat.core.rule.builtin.IfnullRule;
import com.sqlcompat.core.rule.builtin.JsonExtractRule;
import com.sqlcompat.core.rule.builtin.RegexpOperatorRule;

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
                // batch 2 - DML syntax
                new DeleteUpdateLimitRule()
        );
    }
}
