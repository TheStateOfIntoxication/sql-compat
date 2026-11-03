package com.sqlcompat.verify.report;

import com.sqlcompat.verify.model.CaseOutcome;
import com.sqlcompat.verify.model.ComparisonReport;
import com.sqlcompat.verify.model.Status;

import java.util.List;
import java.util.Locale;

/**
 * 一致性对比的 Markdown 报告。
 *
 * <p>头条是一致率；差异表次之，但真正的迁移负责人会读的是差异表，所以它带完整说明。
 */
public final class ComparisonReportRenderer {

    public String render(ComparisonReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# sql-compat 一致性报告\n\n");

        sb.append("| 指标 | 值 |\n|---|---|\n");
        sb.append("| 用例数 | ").append(report.caseCount()).append(" |\n");
        sb.append("| 一致 | ").append(report.consistentCount()).append(" |\n");
        sb.append("| 不一致 | ").append(report.differentCount()).append(" |\n");
        sb.append("| 执行失败 | ").append(report.errorCount()).append(" |\n");
        sb.append("| **一致率** | **").append(report.consistencyRateText()).append("** |\n");
        sb.append("| 各用例中位耗时之和 | ").append(report.mysqlLabel()).append(' ')
                .append(report.mysqlTotalMillis()).append(" ms / ")
                .append(report.postgresLabel()).append(' ')
                .append(report.postgresTotalMillis()).append(" ms |\n\n");

        appendDifferences(sb, report);
        appendAllCases(sb, report);

        sb.append("\n> 耗时是预热后若干轮的中位数，同机同数据测得。它是两个引擎之间的**指示性对比**，\n")
                .append("> 不是 benchmark。\n");
        return sb.toString();
    }

    private void appendDifferences(StringBuilder sb, ComparisonReport report) {
        List<CaseOutcome> differences = report.outcomes().stream()
                .filter(o -> o.status() != Status.CONSISTENT)
                .toList();
        sb.append("## 差异明细\n\n");
        if (differences.isEmpty()) {
            sb.append("所有用例在两个数据库上返回了相同结果。\n\n");
            return;
        }
        for (CaseOutcome outcome : differences) {
            sb.append("### `").append(outcome.sqlCase().id()).append("` — ")
                    .append(outcome.status()).append("\n\n");
            sb.append("- 原因：").append(outcome.detail()).append('\n');
            sb.append("- 行数：").append(report.mysqlLabel()).append('=')
                    .append(outcome.mysql().rowCount()).append("，")
                    .append(report.postgresLabel()).append('=')
                    .append(outcome.postgres().rowCount()).append('\n');
            sb.append("\n```sql\n").append(outcome.sqlCase().sql().strip()).append("\n```\n\n");
        }
    }

    private void appendAllCases(StringBuilder sb, ComparisonReport report) {
        sb.append("## 全部用例\n\n");
        sb.append("| 用例 | 结论 | ").append(report.mysqlLabel()).append(" ms | ")
                .append(report.postgresLabel()).append(" ms | 说明 |\n");
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

    /** 供 CLI 打印一行摘要。 */
    public String summaryLine(ComparisonReport report) {
        return String.format(Locale.ROOT, "一致 %d/%d（%s），不一致 %d，失败 %d",
                report.consistentCount(), report.caseCount(), report.consistencyRateText(),
                report.differentCount(), report.errorCount());
    }
}
