package com.sqlcompat.core.rule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Holds the active rule set.
 *
 * <p>Rules are sorted by id so reports are stable across runs and machines.
 */
public final class RuleRegistry {

    private final List<DialectRule> rules = new ArrayList<>();

    public static RuleRegistry empty() {
        return new RuleRegistry();
    }

    /**
     * Built-in rules plus everything registered through
     * {@code META-INF/services/com.sqlcompat.core.rule.DialectRule}.
     */
    public static RuleRegistry defaults() {
        RuleRegistry registry = new RuleRegistry();
        BuiltinRules.all().forEach(registry::register);
        ServiceLoader.load(DialectRule.class).forEach(registry::register);
        return registry;
    }

    public RuleRegistry register(DialectRule rule) {
        boolean duplicate = rules.stream().anyMatch(r -> r.id().equals(rule.id()));
        if (duplicate) {
            throw new IllegalArgumentException("Duplicate rule id: " + rule.id());
        }
        rules.add(rule);
        rules.sort(Comparator.comparing(DialectRule::id));
        return this;
    }

    public List<DialectRule> rules() {
        return List.copyOf(rules);
    }

    public int size() {
        return rules.size();
    }
}
