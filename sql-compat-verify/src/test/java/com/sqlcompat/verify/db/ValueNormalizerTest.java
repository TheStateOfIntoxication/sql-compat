package com.sqlcompat.verify.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 归一化器是跨引擎对比成立的前提。这里的每个用例都是 MySQL 与 PostgreSQL
 * 对同一个逻辑值返回不同表示的客观差异。
 */
class ValueNormalizerTest {

    @Test
    @DisplayName("null 收敛成同一个字面量，两侧才能比平")
    void normalizesNull() {
        assertThat(ValueNormalizer.normalize(null)).isEqualTo("<null>");
    }

    @Test
    @DisplayName("布尔 true 与整数 1 收敛成同一个值")
    void normalizesBooleanAgainstTinyInt() {
        // MySQL 的 TINYINT(1) 返回 Integer 1，PostgreSQL 的 BOOLEAN 返回 true。
        assertThat(ValueNormalizer.normalize(Boolean.TRUE)).isEqualTo(ValueNormalizer.normalize(1));
        assertThat(ValueNormalizer.normalize(Boolean.FALSE)).isEqualTo(ValueNormalizer.normalize(0));
    }

    @Test
    @DisplayName("小数标度不同不算差异")
    void normalizesDecimalScale() {
        assertThat(ValueNormalizer.normalize(new BigDecimal("100.50")))
                .isEqualTo(ValueNormalizer.normalize(new BigDecimal("100.5")));
        assertThat(ValueNormalizer.normalize(new BigDecimal("1E+3")))
                .isEqualTo(ValueNormalizer.normalize(new BigDecimal("1000")));
    }

    @Test
    @DisplayName("浮点二进制误差不会漏进对比结果")
    void normalizesFloatingPoint() {
        assertThat(ValueNormalizer.normalize(0.1 + 0.2)).isEqualTo(ValueNormalizer.normalize(0.3));
    }

    @Test
    @DisplayName("时间戳精度差异不算差异")
    void normalizesTimestamp() {
        assertThat(ValueNormalizer.normalize(Timestamp.valueOf("2024-02-01 08:00:00")))
                .isEqualTo("2024-02-01T08:00");
    }

    @Test
    @DisplayName("字节数组按 base64 比较，而不是比较对象引用")
    void normalizesByteArray() {
        assertThat(ValueNormalizer.normalize(new byte[]{1, 2, 3}))
                .isEqualTo(ValueNormalizer.normalize(new byte[]{1, 2, 3}));
    }

    @Test
    @DisplayName("整数除法与小数除法必须保持不同 —— 那才是真实差异")
    void doesNotHideDivisionSemantics() {
        assertThat(ValueNormalizer.normalize(new BigDecimal("0.5000")))
                .isNotEqualTo(ValueNormalizer.normalize(0));
    }
}
