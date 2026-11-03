package com.sqlcompat.verify.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The normalizer is what makes cross-engine comparison meaningful. Every case here is a real
 * difference between what MySQL and PostgreSQL return for the same logical value.
 */
class ValueNormalizerTest {

    @Test
    @DisplayName("null becomes a single literal so both sides compare equal")
    void normalizesNull() {
        assertThat(ValueNormalizer.normalize(null)).isEqualTo("<null>");
    }

    @Test
    @DisplayName("boolean true and integer 1 collapse to the same value")
    void normalizesBooleanAgainstTinyInt() {
        // MySQL TINYINT(1) comes back as Integer 1, PostgreSQL BOOLEAN as true.
        assertThat(ValueNormalizer.normalize(Boolean.TRUE)).isEqualTo(ValueNormalizer.normalize(1));
        assertThat(ValueNormalizer.normalize(Boolean.FALSE)).isEqualTo(ValueNormalizer.normalize(0));
    }

    @Test
    @DisplayName("decimal scale differences do not count as a difference")
    void normalizesDecimalScale() {
        assertThat(ValueNormalizer.normalize(new BigDecimal("100.50")))
                .isEqualTo(ValueNormalizer.normalize(new BigDecimal("100.5")));
        assertThat(ValueNormalizer.normalize(new BigDecimal("1E+3")))
                .isEqualTo(ValueNormalizer.normalize(new BigDecimal("1000")));
    }

    @Test
    @DisplayName("double artifacts do not leak into the comparison")
    void normalizesFloatingPoint() {
        assertThat(ValueNormalizer.normalize(0.1 + 0.2)).isEqualTo(ValueNormalizer.normalize(0.3));
    }

    @Test
    @DisplayName("timestamp precision differences do not count as a difference")
    void normalizesTimestamp() {
        assertThat(ValueNormalizer.normalize(Timestamp.valueOf("2024-02-01 08:00:00")))
                .isEqualTo("2024-02-01T08:00");
    }

    @Test
    @DisplayName("byte arrays are compared as base64, not as object identity")
    void normalizesByteArray() {
        assertThat(ValueNormalizer.normalize(new byte[]{1, 2, 3}))
                .isEqualTo(ValueNormalizer.normalize(new byte[]{1, 2, 3}));
    }

    @Test
    @DisplayName("integer and decimal division stay different — that is a real dialect gap")
    void doesNotHideDivisionSemantics() {
        assertThat(ValueNormalizer.normalize(new BigDecimal("0.5000")))
                .isNotEqualTo(ValueNormalizer.normalize(0));
    }
}
