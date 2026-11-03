package com.sqlcompat.verify.model;

import java.util.List;
import java.util.Locale;

/**
 * 一次完整一致性对比的结果。
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
}
