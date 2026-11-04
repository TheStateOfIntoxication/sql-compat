package com.sqlcompat.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java 8 兼容的集合工厂。
 *
 * <p>存在的原因：本工具要求**运行在 JDK 8 上**（信创现场大量项目就是 JDK 8，插件必须能在
 * 那样的 Maven 里被加载），而 {@code List.of / Set.of / Map.of} 都是 Java 9+ 的 API。
 *
 * <p>与 JDK 原生工厂的差异（都是放宽，不是收紧）：
 * <ul>
 *   <li>允许 {@code null} 元素 —— {@code List.of} 遇到 null 会抛 NPE，这里不会</li>
 *   <li>返回的是**只读视图**，与 {@code List.of} 一致，但底层允许 {@code Arrays.asList} 的 set 语义</li>
 *   <li>{@link #set} 用 {@link LinkedHashSet} 保证**迭代顺序 = 声明顺序**，
 *       这一点很重要：规则注册顺序会影响报告里问题的排列</li>
 * </ul>
 */
public final class J8 {

    private J8() {
    }

    /** 只读列表，保留声明顺序。 */
    @SafeVarargs
    public static <T> List<T> list(T... items) {
        return Collections.unmodifiableList(Arrays.asList(items));
    }

    /** 只读集合，迭代顺序 = 声明顺序。 */
    @SafeVarargs
    public static <T> Set<T> set(T... items) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(items)));
    }

    /** 单键值对的只读 Map。 */
    public static <K, V> Map<K, V> map(K key, V value) {
        Map<K, V> m = new LinkedHashMap<>();
        m.put(key, value);
        return Collections.unmodifiableMap(m);
    }

    /** 两键值对的只读 Map。 */
    public static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2) {
        Map<K, V> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return Collections.unmodifiableMap(m);
    }

    /** 任意数量键值对的只读 Map，键值交替传入（键、值、键、值…）。 */
    public static Map<String, String> map(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("键值必须成对出现，实参个数为 " + keyValues.length);
        }
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put(keyValues[i], keyValues[i + 1]);
        }
        return Collections.unmodifiableMap(m);
    }

    /** 空的可变列表，省掉重复的 {@code new ArrayList<>()}。 */
    public static <T> List<T> mutableList() {
        return new ArrayList<>();
    }
}
