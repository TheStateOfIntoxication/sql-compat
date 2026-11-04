package com.sqlcompat.verify.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 一次完整一致性对比的结果。
 */
public final class ComparisonReport {

    private final String mysqlLabel;
    private final String postgresLabel;
    private final List<CaseOutcome> outcomes;

    public ComparisonReport(String mysqlLabel, String postgresLabel, List<CaseOutcome> outcomes) {
        this.mysqlLabel = mysqlLabel;
        this.postgresLabel = postgresLabel;
        this.outcomes = Collections.unmodifiableList(new ArrayList<>(outcomes));
    }

    public String mysqlLabel() {
        return mysqlLabel;
    }

    public String postgresLabel() {
        return postgresLabel;
    }

    public List<CaseOutcome> outcomes() {
        return outcomes;
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
     * 结果完全一致的用例占比，百分比保留一位小数。
     * 出错也计入不一致：一侧执行失败的语句不能算"一致"。
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

    /** 各用例中位耗时之和，用于粗略比较两个引擎的量级。 */
    public long mysqlTotalMillis() {
        return outcomes.stream().mapToLong(o -> o.mysql().millis()).sum();
    }

    public long postgresTotalMillis() {
        return outcomes.stream().mapToLong(o -> o.postgres().millis()).sum();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ComparisonReport)) {
            return false;
        }
        ComparisonReport that = (ComparisonReport) o;
        return Objects.equals(mysqlLabel, that.mysqlLabel)
                && Objects.equals(postgresLabel, that.postgresLabel)
                && outcomes.equals(that.outcomes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mysqlLabel, postgresLabel, outcomes);
    }

    @Override
    public String toString() {
        return "ComparisonReport[" + caseCount() + " cases, " + consistencyRateText() + "]";
    }
}
