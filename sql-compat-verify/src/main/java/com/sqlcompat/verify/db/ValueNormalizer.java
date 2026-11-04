package com.sqlcompat.verify.db;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.temporal.TemporalAccessor;
import java.util.Base64;
import java.util.Locale;

/**
 * 把 JDBC 值收敛成规范字符串，好让两个引擎的结果可比。
 *
 * <p>这个类是对比的核心，而且刻意做了取舍。没有它，几乎每条用例都会"不一致"，
 * 但原因并不是真的差异：
 *
 * <ul>
 *   <li>布尔列 MySQL 返回 {@code 1}，PostgreSQL 返回 {@code true}</li>
 *   <li>{@code DECIMAL} / {@code numeric} 运算后标度不同，但数值相等</li>
 *   <li>时间戳精度与 {@code toString()} 格式不一致</li>
 * </ul>
 *
 * <p>**故意不归一**的两件事：整数除法与字符串拼接运算符。那是真实方言差异，应该被报出来。
 */
public final class ValueNormalizer {

    private static final String NULL_LITERAL = "<null>";

    private ValueNormalizer() {
    }

    public static String normalize(Object value) {
        if (value == null) {
            return NULL_LITERAL;
        }
        if (value instanceof Boolean) {
            // MySQL TINYINT(1) 返回 Integer(1)，PostgreSQL 返回 Boolean(true)。
            return ((Boolean) value) ? "1" : "0";
        }
        if (value instanceof BigDecimal) {
            BigDecimal stripped = ((BigDecimal) value).stripTrailingZeros();
            return stripped.scale() < 0 ? stripped.setScale(0).toPlainString() : stripped.toPlainString();
        }
        if (value instanceof Double || value instanceof Float) {
            // 驱动只用 Double 表示 FLOAT/REAL 列，而二进制表示会让 0.1 + 0.2 不等于 0.3。
            // 按 10 位有效数字比较，避免这种噪声被误报为差异。
            // 代价：第 10 位有效数字之后的小差异看不出来 —— 迁移检查可以接受。
            return String.format(Locale.ROOT, "%.10g", ((Number) value).doubleValue());
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof byte[]) {
            return Base64.getEncoder().encodeToString((byte[]) value);
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().toString();
        }
        if (value instanceof Date) {
            return ((Date) value).toLocalDate().toString();
        }
        if (value instanceof Time) {
            return ((Time) value).toLocalTime().toString();
        }
        if (value instanceof TemporalAccessor) {
            return value.toString();
        }
        return value.toString();
    }

    public static String nullLiteral() {
        return NULL_LITERAL;
    }
}
