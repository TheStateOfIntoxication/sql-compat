package com.sqlcompat.core.report;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Report;
import com.sqlcompat.core.model.ScanResult;
import com.sqlcompat.core.model.Severity;

import java.util.List;
import java.util.Map;

/**
 * Plain Markdown report: easy to read in a terminal, easy to diff in CI logs.
 */
public final class MarkdownReportRenderer implements ReportRenderer {

    @Override
    public String extension() {
        return "md";
    }

    @Override
    public String render(Report report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# sql-compat report\n\n");
        sb.append("| item | value |\n|---|---|\n");
        sb.append("| target | `").append(report.target()).append("` |\n");
        sb.append("| dialects | ").append(report.sourceDialect()).append(" -> ")
                .append(report.targetDialect()).append(" |\n");
        sb.append("| statements scanned | ").append(report.statementCount()).append(" |\n");
        sb.append("| issues | ").append(report.issueCount()).append(" |\n");
        sb.append("| parse failures | ").append(report.parseErrorCount()).append(" |\n");
        sb.append("| elapsed | ").append(report.elapsed().toMillis()).append(" ms |\n\n");

        sb.append("## By severity\n\n");
        for (Map.Entry<Severity, Integer> entry : report.issueCountBySeverity().entrySet()) {
            sb.append("- **").append(entry.getKey()).append("**: ").append(entry.getValue()).append('\n');
        }
        sb.append('\n');

        sb.append("## Findings\n\n");
        List<ScanResult> withIssues = report.results().stream().filter(r -> !r.clean()).toList();
        if (withIssues.isEmpty()) {
            sb.append("No compatibility issues found.\n");
            return sb.toString();
        }
        for (ScanResult result : withIssues) {
            sb.append("### ").append(result.statement().qualifiedId())
                    .append(" — `").append(result.statement().location()).append("`\n\n");
            for (Issue issue : result.issues()) {
                sb.append("- **").append(issue.severity()).append("** `").append(issue.ruleId())
                        .append("` ").append(issue.ruleName()).append('\n');
                sb.append("  - ").append(issue.message()).append('\n');
                if (issue.snippet() != null && !issue.snippet().isBlank()) {
                    sb.append("  - found: `").append(issue.snippet()).append("`\n");
                }
                if (issue.suggestion() != null && !issue.suggestion().isBlank()) {
                    sb.append("  - fix: `").append(issue.suggestion()).append("`\n");
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
