package com.sqlcompat.verify.model;

import java.util.List;
import java.util.Locale;

/**
 * Result of a full consistency run.
 */
public record ComparisonReport(
        String mysqlLabel,
        String postgresLabel,
        List<CaseOutcome> outcomes) {

    public ComparisonReport {
        outcomes = List.copyOf(outcomes);
    }

    public int caseCount() {
        return outcomes.size();
    }

    public int consistentCount() {
        return (int) outcomes.stream().filter(o -> o.status() == Status.CONSISTENT).count();
    }

    public int differentCount() {
        return (int) outcomes.stream().filter(o -> o.status() == Status.DIFFERENT).count();
    }

    public int errorCount() {
        return (int) outcomes.stream().filter(o -> o.status() == Status.ERROR).count();
    }

    /**
     * Share of cases that produced identical results, as a percentage with one decimal.
     * Error cases count as inconsistent: a statement that fails on one side is not "consistent".
     */
    public double consistencyRate() {
        if (caseCount() == 0) {
            return 0.0;
        }
        return consistentCount() * 100.0 / caseCount();
    }

    public String consistencyRateText() {
        return String.format(Locale.ROOT, "%.1f%%", consistencyRate());
    }

    /** Sum of per-case median times, a rough order-of-magnitude comparison between engines. */
    public long mysqlTotalMillis() {
        return outcomes.stream().mapToLong(o -> o.mysql().millis()).sum();
    }

    public long postgresTotalMillis() {
        return outcomes.stream().mapToLong(o -> o.postgres().millis()).sum();
    }
}
