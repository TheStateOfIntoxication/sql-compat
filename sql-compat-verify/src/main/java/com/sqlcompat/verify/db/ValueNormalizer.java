package com.sqlcompat.verify.db;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.temporal.TemporalAccessor;
import java.util.Base64;

/**
 * Turns JDBC values into a canonical string so two engines can be compared.
 *
 * <p>This class is the heart of the comparison, and it is opinionated on purpose. Without it
 * almost every case "differs" for reasons that are not real differences:
 *
 * <ul>
 *   <li>MySQL returns {@code 1} for a boolean column, PostgreSQL returns {@code true}</li>
 *   <li>MySQL {@code DECIMAL} comes back as {@code 0.5000}, PostgreSQL {@code numeric} as {@code 0.5000}
 *       but with a different scale after arithmetic</li>
 *   <li>timestamps carry different precision and different {@code toString()} formats</li>
 * </ul>
 *
 * <p>What it deliberately does <b>not</b> normalize: integer vs decimal division, and string
 * concatenation operators. Those are genuine dialect differences and should be reported.
 */
public final class ValueNormalizer {

    private static final String NULL_LITERAL = "<null>";

    private ValueNormalizer() {
    }

    public static String normalize(Object value) {
        if (value == null) {
            return NULL_LITERAL;
        }
        if (value instanceof Boolean bool) {
            // MySQL TINYINT(1) arrives as Integer(1); PostgreSQL as Boolean(true).
            return bool ? "1" : "0";
        }
        if (value instanceof BigDecimal decimal) {
            BigDecimal stripped = decimal.stripTrailingZeros();
            return stripped.scale() < 0 ? stripped.setScale(0).toPlainString() : stripped.toPlainString();
        }
        if (value instanceof Double || value instanceof Float) {
            // Drivers return Double only for FLOAT/REAL columns, where binary representation makes
            // 0.1 + 0.2 differ from 0.3. Compare to 10 significant digits so noise does not show
            // up as a false difference. Trade-off: a real difference beyond the 10th significant
            // digit would be missed — acceptable for migration checks.
            return String.format(java.util.Locale.ROOT, "%.10g", ((Number) value).doubleValue());
        }
        if (value instanceof Number number) {
            return number.toString();
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toString();
        }
        if (value instanceof Date date) {
            return date.toLocalDate().toString();
        }
        if (value instanceof Time time) {
            return time.toLocalTime().toString();
        }
        if (value instanceof TemporalAccessor temporal) {
            return temporal.toString();
        }
        return value.toString();
    }

    public static String nullLiteral() {
        return NULL_LITERAL;
    }
}
