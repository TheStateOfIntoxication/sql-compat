package com.sqlcompat.core.report;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Report;
import com.sqlcompat.core.model.ScanResult;
import com.sqlcompat.core.model.Severity;

import java.util.List;
import java.util.Map;

/**
 * 纯 Markdown 报告：终端里好读，CI 日志里好 diff。
 */
public final class MarkdownReportRenderer implements ReportRenderer {

    @Override
    public String extension() {
        return "md";
    }

    @Override
    public String render(Report report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# sql-compat 兼容性报告\n\n");
        sb.append("| 项 | 值 |\n|---|---|\n");
        sb.append("| 扫描目标 | `").append(report.target()).append("` |\n");
        sb.append("| 方言 | ").append(report.sourceDialect()).append(" -> ")
                .append(report.targetDialect()).append(" |\n");
        sb.append("| 扫描语句数 | ").append(report.statementCount()).append(" |\n");
        sb.append("| 问题数 | ").append(report.issueCount()).append(" |\n");
        sb.append("| 解析失败数 | ").append(report.parseErrorCount()).append(" |\n");
        sb.append("| 耗时 | ").append(report.elapsed().toMillis()).append(" ms |\n\n");

        sb.append("## 按级别统计\n\n");
        for (Map.Entry<Severity, Integer> entry : report.issueCountBySeverity().entrySet()) {
            sb.append("- **").append(entry.getKey()).append("**：").append(entry.getValue()).append('\n');
        }
        sb.append('\n');

        sb.append("## 问题明细\n\n");
        List<ScanResult> withIssues = report.results().stream().filter(r -> !r.clean()).toList();
        if (withIssues.isEmpty()) {
            sb.append("未发现兼容性问题。\n");
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
                    sb.append("  - 命中：`").append(issue.snippet()).append("`\n");
                }
                if (issue.suggestion() != null && !issue.suggestion().isBlank()) {
                    sb.append("  - 改写：`").append(issue.suggestion()).append("`\n");
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
