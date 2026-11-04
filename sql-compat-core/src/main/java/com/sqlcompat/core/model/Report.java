package com.sqlcompat.core.model;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;

/**
 * 一次完整扫描的输出，交由 {@code ReportRenderer} 渲染。
 */
public final class Report {

    private final String target;
    private final String sourceDialect;
    private final String targetDialect;
    private final List<ScanResult> results;
    private final Duration elapsed;

    public Report(String target,
                  String sourceDialect,
                  String targetDialect,
                  List<ScanResult> results,
                  Duration elapsed) {
        this.target = target;
        this.sourceDialect = sourceDialect;
        this.targetDialect = targetDialect;
        this.results = Collections.unmodifiableList(new ArrayList<>(results));
        this.elapsed = elapsed;
    }

    public String target() {
        return target;
    }

    public String sourceDialect() {
        return sourceDialect;
    }

    public String targetDialect() {
        return targetDialect;
    }

    public List<ScanResult> results() {
        return results;
    }

    public Duration elapsed() {
        return elapsed;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Report)) {
            return false;
        }
        Report that = (Report) o;
        return Objects.equals(target, that.target)
                && Objects.equals(sourceDialect, that.sourceDialect)
                && Objects.equals(targetDialect, that.targetDialect)
                && results.equals(that.results)
                && Objects.equals(elapsed, that.elapsed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, sourceDialect, targetDialect, results, elapsed);
    }

    @Override
    public String toString() {
        return "Report[" + statementCount() + " statements, " + issueCount() + " issues]";
    }
}
