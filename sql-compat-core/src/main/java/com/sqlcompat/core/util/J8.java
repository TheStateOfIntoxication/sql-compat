package com.sqlcompat.core.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Java 8 兼容的集合工厂。
 *
 * <p>存在的原因：本工具要求**运行在 JDK 8 上**（信创现场大量项目就是 JDK 8，插件必须能在
 * 那样的 Maven 里被加载），而 {@code List.of / Set.of} 都是 Java 9+ 的 API。
 *
 * <p>两条设计约束：
 * <ul>
 *   <li><b>严格性与原生工厂一致</b>：{@code list} 遇到 {@code null} 元素抛 NPE，
 *       {@code set} 遇到重复元素抛 IAE —— 与 {@code List.of / Set.of} 相同。
 *       放宽成「静默接受」会让 null 与重复缺陷推迟到下游才爆发，反而更难查</li>
 *   <li><b>迭代顺序 = 声明顺序</b>：{@link #set} 用 {@link LinkedHashSet}。
 *       这一点很重要，规则注册顺序会影响报告里问题的排列</li>
 * </ul>
 */
public final class J8 {

    private J8() {
    }

    /** 只读列表，保留声明顺序；含 {@code null} 元素时抛 NPE（与 {@code List.of} 一致）。 */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> List<T> list(T... items) {
        for (T item : items) {
            Objects.requireNonNull(item, "集合元素不可为 null");
        }
        return Collections.unmodifiableList(Arrays.asList(items));
    }

    /**
     * 只读集合，迭代顺序 = 声明顺序；含 {@code null} 或重复元素时抛异常
     * （与 {@code Set.of} 一致）。
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> Set<T> set(T... items) {
        Set<T> unique = new LinkedHashSet<>(items.length * 2);
        for (T item : items) {
            Objects.requireNonNull(item, "集合元素不可为 null");
            if (!unique.add(item)) {
                throw new IllegalArgumentException("重复元素: " + item);
            }
        }
        return Collections.unmodifiableSet(unique);
    }

    /**
     * 按「键、值、键、值…」构造只读字符串 Map。
     *
     * <p>只支持 String 键值，且方法名点明了这一点 —— 之前用 {@code map} 做名字，
     * 与泛型重载放在一起时，{@code map("a", "b")} 会走泛型版本、{@code map(a, b, c, d)}
     * 走 varargs，同形调用语义分叉，读代码时容易误判。
     */
    public static java.util.Map<String, String> stringMap(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("键值必须成对出现，实参个数为 " + keyValues.length);
        }
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>(keyValues.length);
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put(Objects.requireNonNull(keyValues[i], "键不可为 null"),
                    Objects.requireNonNull(keyValues[i + 1], "值不可为 null"));
        }
        return Collections.unmodifiableMap(m);
    }
}
