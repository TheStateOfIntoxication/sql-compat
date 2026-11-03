package com.sqlcompat.verify.report;

import com.sqlcompat.verify.model.CaseOutcome;
import com.sqlcompat.verify.model.ComparisonReport;
import com.sqlcompat.verify.model.Status;

import java.util.List;
import java.util.Locale;

/**
 * Markdown report for a consistency run.
 *
 * <p>The headline is the consistency rate; the differences table is what a migration owner
 * actually reads, so it comes second and carries the full explanation.
 */
public final class ComparisonReportRenderer {

    public String render(ComparisonReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# sql-compat consistency report\n\n");

        sb.append("| metric | value |\n|---|---|\n");
        sb.append("| cases | ").append(report.caseCount()).append(" |\n");
        sb.append("| consistent | ").append(report.consistentCount()).append(" |\n");
        sb.append("| different | ").append(report.differentCount()).append(" |\n");
        sb.append("| errors | ").append(report.errorCount()).append(" |\n");
        sb.append("| **consistency rate** | **").append(report.consistencyRateText()).append("** |\n");
        sb.append("| sum of per-case median time | ").append(report.mysqlLabel()).append(' ')
                .append(report.mysqlTotalMillis()).append(" ms / ")
                .append(report.postgresLabel()).append(' ')
                .append(report.postgresTotalMillis()).append(" ms |\n\n");

        appendDifferences(sb, report);
        appendAllCases(sb, report);

        sb.append("\n> Timing is a median over a few iterations after a warm-up round, measured on the\n")
                .append("> same machine with the same data. It is an indicative comparison between two\n")
                .append("> engines, **not** a benchmark.\n");
        return sb.toString();
    }

    private void appendDifferences(StringBuilder sb, ComparisonReport report) {
        List<CaseOutcome> differences = report.outcomes().stream()
                .filter(o -> o.status() != Status.CONSISTENT)
                .toList();
        sb.append("## Differences\n\n");
        if (differences.isEmpty()) {
            sb.append("Every case returned the same result on both databases.\n\n");
            return;
        }
        for (CaseOutcome outcome : differences) {
            sb.append("### `").append(outcome.sqlCase().id()).append("` — ")
                    .append(outcome.status()).append("\n\n");
            sb.append("- reason: ").append(outcome.detail()).append('\n');
            sb.append("- rows: ").append(report.mysqlLabel()).append('=')
                    .append(outcome.mysql().rowCount()).append(", ")
                    .append(report.postgresLabel()).append('=')
                    .append(outcome.postgres().rowCount()).append('\n');
            sb.append("\n```sql\n").append(outcome.sqlCase().sql().strip()).append("\n```\n\n");
        }
    }

    private void appendAllCases(StringBuilder sb, ComparisonReport report) {
        sb.append("## All cases\n\n");
        sb.append("| case | status | ").append(report.mysqlLabel()).append(" ms | ")
                .append(report.postgresLabel()).append(" ms | detail |\n");
        sb.append("|---|---|---|---|---|\n");
        for (CaseOutcome outcome : report.outcomes()) {
            sb.append("| `").append(outcome.sqlCase().id()).append("` | ")
                    .append(outcome.status()).append(" | ")
                    .append(outcome.mysql().millis()).append(" | ")
                    .append(outcome.postgres().millis()).append(" | ")
                    .append(escapePipes(outcome.detail() == null ? "" : outcome.detail()))
                    .append(" |\n");
        }
        sb.append('\n');
    }

    private String escapePipes(String value) {
        String trimmed = value.length() > 120 ? value.substring(0, 117) + "..." : value;
        return trimmed.replace("|", "\\|");
    }

    /** Convenience for the CLI summary line. */
    public String summaryLine(ComparisonReport report) {
        return String.format(Locale.ROOT, "%d/%d consistent (%s), %d different, %d error",
                report.consistentCount(), report.caseCount(), report.consistencyRateText(),
                report.differentCount(), report.errorCount());
    }
}
