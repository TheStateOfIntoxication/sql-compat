package com.sqlcompat.cli;

import com.sqlcompat.core.engine.CompatEngine;
import com.sqlcompat.core.fix.AutoFixer;
import com.sqlcompat.core.fix.FixMode;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * {@code sql-compat check -i ./mappers -o report.html}
 */
@Command(
        name = "check",
        mixinStandardHelpOptions = true,
        description = "扫描 Mapper XML / SQL 文件并输出兼容性报告；可选自动改写。")
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

    @Option(names = {"--fix"},
            description = "自动执行等价改写（R001 IFNULL / R004 分页 / R016 <=>），"
                    + "结果写到 --fix-out 指定的目录，不改源文件。")
    boolean fix;

    @Option(names = {"--fix-all"},
            description = "连需要人工确认的改写一起执行（R002 日期格式 / R009 反引号 / R013 JSON）。"
                    + "改写前请先读 docs/manual-decisions.md。")
    boolean fixAll;

    @Option(names = {"--fix-out"},
            description = "改写结果输出目录，默认 ./sql-compat-fixed。")
    Path fixOut;

    @Option(names = {"--in-place"},
            description = "直接在源目录改写。除特殊场景外不建议使用。")
    boolean inPlace;

    @Override
    public Integer call() throws Exception {
        ReportRenderer renderer = "md".equalsIgnoreCase(format)
                ? new MarkdownReportRenderer()
                : new HtmlReportRenderer();

        CompatEngine engine = new CompatEngine();
        Report before = engine.scan(input, "MySQL", "Kingbase/PostgreSQL");

        Report reported = before;
        FixMode mode = fixAll ? FixMode.INCLUDE_REVIEW : (fix ? FixMode.SAFE_ONLY : FixMode.OFF);
        if (mode != FixMode.OFF) {
            reported = applyFix(engine, mode, before);
        }

        Path target = output != null
                ? output
                : Path.of("sql-compat-report." + renderer.extension());
        Files.writeString(target, renderer.render(reported), StandardCharsets.UTF_8);

        printSummary(reported, target);

        if (failOn != null) {
            Severity worst = reported.worstSeverity();
            if (worst != null && worst.atLeast(failOn)) {
                System.err.printf(Locale.ROOT, "失败：最高级别 %s 达到或超过阈值 %s%n", worst, failOn);
                return 1;
            }
        }
        // 有改写但仍有 BLOCKER 时给出非 0 退出码：让人别以为万事大吉。
        if (mode != FixMode.OFF && reported.worstSeverity() == Severity.BLOCKER) {
            System.err.println("提示：自动改写后仍存在 BLOCKER，剩余项需人工决策，"
                    + "见 docs/manual-decisions.md。");
        }
        return 0;
    }

    private Report applyFix(CompatEngine engine, FixMode mode, Report before) throws Exception {
        Path targetDir = inPlace ? input : (fixOut != null ? fixOut : Path.of("sql-compat-fixed"));
        List<AutoFixer.FixResult> results = new AutoFixer(
                com.sqlcompat.core.rule.RuleRegistry.defaults(), mode).fixTree(input, targetDir);

        List<String> appliedRules = new ArrayList<>();
        int changedFiles = 0;
        for (AutoFixer.FixResult result : results) {
            if (result.changed()) {
                changedFiles++;
                appliedRules.addAll(result.appliedRuleIds());
            }
        }

        System.out.printf(Locale.ROOT, "改写完成：%d/%d 个文件有变更，%d 条问题 -> ",
                changedFiles, results.size(), before.issueCount());
        System.out.println(appliedRules.stream().distinct().sorted().toList() + " 规则生效");
        System.out.println("改写结果已写入 " + targetDir.toAbsolutePath()
                + (inPlace ? "（原地改写）" : ""));

        // 对改写结果重新扫描：这是改写唯一有意义的验收标准。
        Report after = engine.scan(targetDir, "MySQL", "Kingbase/PostgreSQL");
        System.out.printf(Locale.ROOT, "重新扫描：问题 %d -> %d（减少 %d）%n",
                before.issueCount(), after.issueCount(),
                before.issueCount() - after.issueCount());
        return after;
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
