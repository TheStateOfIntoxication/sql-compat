package com.sqlcompat.core.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;

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
        return Collections.unmodifiableList(new ArrayList<>(rules));
    }

    /** 返回忽略指定编号后的新注册表；用于在报告里屏蔽某条规则（如 foreach 密集项目的 R011）。 */
    public RuleRegistry excluding(Collection<String> ruleIds) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            return this;
        }
        Set<String> skip = ruleIds.stream()
                .filter(Objects::nonNull)
                .map(id -> id.trim().toUpperCase(Locale.ROOT))
                .filter(id -> !id.isEmpty())
                .collect(Collectors.collectingAndThen(Collectors.toCollection(java.util.LinkedHashSet::new), java.util.Collections::unmodifiableSet));
        RuleRegistry filtered = new RuleRegistry();
        rules.stream()
                .filter(rule -> !skip.contains(rule.id().toUpperCase(Locale.ROOT)))
                .forEach(filtered::register);
        return filtered;
    }

    public int size() {
        return rules.size();
    }
}
