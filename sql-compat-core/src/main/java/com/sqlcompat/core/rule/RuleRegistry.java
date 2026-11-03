package com.sqlcompat.core.rule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 当前生效的规则集合。
 *
 * <p>规则按编号排序，保证同一份输入在不同机器、不同运行次数下产出同一份报告。
 */
public final class RuleRegistry {

    private final List<DialectRule> rules = new ArrayList<>();

    public static RuleRegistry empty() {
        return new RuleRegistry();
    }

    /**
     * 内置规则，加上通过 {@code META-INF/services/com.sqlcompat.core.rule.DialectRule}
     * 注册进来的外部规则。
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
            throw new IllegalArgumentException("规则编号重复：" + rule.id());
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
