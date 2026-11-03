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
        description = "Scan mapper XML / SQL files and write a compatibility report.")
public final class CheckCommand implements Callable<Integer> {

    @Option(names = {"-i", "--input"}, required = true,
            description = "Mapper XML file, .sql file, or a directory to scan.")
    Path input;

    @Option(names = {"-o", "--output"},
            description = "Report file. Defaults to sql-compat-report.<ext> in the current directory.")
    Path output;

    @Option(names = {"-f", "--format"}, defaultValue = "html",
            description = "Report format: html | md. Default: ${DEFAULT-VALUE}")
    String format;

    @Option(names = {"--fail-on"},
            description = "Exit with code 1 when an issue of this severity or above is found: "
                    + "BLOCKER | MAJOR | MINOR | INFO. Default: never fail.")
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
                System.err.printf("FAILED: worst severity %s >= %s%n", worst, failOn);
                return 1;
            }
        }
        return 0;
    }

    private void printSummary(Report report, Path target) {
        System.out.printf(Locale.ROOT, "scanned %d statements in %d ms%n",
                report.statementCount(), report.elapsed().toMillis());
        System.out.printf(Locale.ROOT, "issues: %d  (blocker=%d major=%d minor=%d info=%d)%n",
                report.issueCount(),
                report.issueCountBySeverity().getOrDefault(Severity.BLOCKER, 0),
                report.issueCountBySeverity().getOrDefault(Severity.MAJOR, 0),
                report.issueCountBySeverity().getOrDefault(Severity.MINOR, 0),
                report.issueCountBySeverity().getOrDefault(Severity.INFO, 0));
        if (report.parseErrorCount() > 0) {
            System.out.printf(Locale.ROOT, "parse failures: %d%n", report.parseErrorCount());
        }
        System.out.println("report written to " + target.toAbsolutePath());
    }
}
