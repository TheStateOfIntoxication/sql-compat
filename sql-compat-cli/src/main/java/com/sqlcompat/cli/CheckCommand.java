package com.sqlcompat.cli;

import com.sqlcompat.core.engine.CompatEngine;
import com.sqlcompat.core.model.Report;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.report.HtmlReportRenderer;
import com.sqlcompat.core.report.MarkdownReportRenderer;
import com.sqlcompat.core.report.ReportRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * {@code sql-compat check -i ./mappers -o report.html}
 */
@Command(
        name = "check",
        mixinStandardHelpOptions = true,
        description = "扫描 Mapper XML / SQL 文件并输出兼容性报告。")
public final class CheckCommand implements Callable<Integer> {

    @Option(names = {"-i", "--input"}, required = true,
            description = "要扫描的 Mapper XML 文件、.sql 文件或目录。")
    Path input;

    @Option(names = {"-o", "--output"},
            description = "报告输出路径，默认输出到当前目录的 sql-compat-report.<ext>。")
    Path output;

    @Option(names = {"-f", "--format"}, defaultValue = "html",
            description = "报告格式：html | md。默认：${DEFAULT-VALUE}")
    String format;

    @Option(names = {"--fail-on"},
            description = "出现该级别及以上的问题时以退出码 1 结束："
                    + "BLOCKER | MAJOR | MINOR | INFO。默认：不因问题而失败。")
    Severity failOn;

    @Override
    public Integer call() throws Exception {
        ReportRenderer renderer = "md".equalsIgnoreCase(format)
                ? new MarkdownReportRenderer()
                : new HtmlReportRenderer();

        Report report = new CompatEngine().scan(input, "MySQL", "Kingbase/PostgreSQL");

        Path target = output != null
                ? output
                : Path.of("sql-compat-report." + renderer.extension());
        Files.writeString(target, renderer.render(report), StandardCharsets.UTF_8);

        printSummary(report, target);

        if (failOn != null) {
            Severity worst = report.worstSeverity();
            if (worst != null && worst.atLeast(failOn)) {
                System.err.printf(Locale.ROOT, "失败：最高级别 %s 达到或超过阈值 %s%n", worst, failOn);
                return 1;
            }
        }
        return 0;
    }

    private void printSummary(Report report, Path target) {
        System.out.printf(Locale.ROOT, "共扫描 %d 条语句，耗时 %d ms%n",
                report.statementCount(), report.elapsed().toMillis());
        System.out.printf(Locale.ROOT, "问题 %d 个（blocker=%d major=%d minor=%d info=%d）%n",
                report.issueCount(),
                report.issueCountBySeverity().getOrDefault(Severity.BLOCKER, 0),
                report.issueCountBySeverity().getOrDefault(Severity.MAJOR, 0),
                report.issueCountBySeverity().getOrDefault(Severity.MINOR, 0),
                report.issueCountBySeverity().getOrDefault(Severity.INFO, 0));
        if (report.parseErrorCount() > 0) {
            System.out.printf(Locale.ROOT, "解析失败 %d 条%n", report.parseErrorCount());
        }
        System.out.println("报告已写入 " + target.toAbsolutePath());
    }
}
