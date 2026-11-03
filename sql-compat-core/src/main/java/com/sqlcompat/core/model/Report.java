package com.sqlcompat.core.model;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 一次完整扫描的输出，交由 {@code ReportRenderer} 渲染。
 */
public record Report(
        String target,
        String sourceDialect,
        String targetDialect,
        List<ScanResult> results,
        Duration elapsed) {

    public Report {
        results = List.copyOf(results);
    }

    public int statementCount() {
        return results.size();
    }

    public int issueCount() {
        return results.stream().mapToInt(r -> r.issues().size()).sum();
    }

    public int parseErrorCount() {
        return (int) results.stream().filter(r -> r.parseError() != null).count();
    }

    public Map<Severity, Integer> issueCountBySeverity() {
        Map<Severity, Integer> counts = new EnumMap<>(Severity.class);
        for (Severity s : Severity.values()) {
            counts.put(s, 0);
        }
        results.stream()
                .flatMap(r -> r.issues().stream())
                .forEach(i -> counts.merge(i.severity(), 1, Integer::sum));
        return counts;
    }

    /** 出现过的最高级别；扫描结果干净时返回 {@code null}。 */
    public Severity worstSeverity() {
        Severity worst = null;
        for (ScanResult r : results) {
            for (Issue i : r.issues()) {
                if (worst == null || i.severity().weight() > worst.weight()) {
                    worst = i.severity();
                }
            }
        }
        return worst;
    }
}
