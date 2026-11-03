package com.sqlcompat.core.rule;

import com.sqlcompat.core.rule.builtin.DateFormatRule;
import com.sqlcompat.core.rule.builtin.DeleteUpdateLimitRule;
import com.sqlcompat.core.rule.builtin.IfnullRule;

import java.util.List;

/**
 * Built-in rule catalogue.
 *
 * <p>Using an explicit list (instead of ServiceLoader) for built-ins keeps the first run
 * deterministic and avoids classpath surprises in IDE sandboxes.
 */
final class BuiltinRules {

    private BuiltinRules() {
    }

    static List<DialectRule> all() {
        return List.of(
                new IfnullRule(),
                new DateFormatRule(),
                new DeleteUpdateLimitRule()
        );
    }
}
