package com.sqlcompat.maven;

import com.sqlcompat.core.engine.CompatEngine;
import com.sqlcompat.core.model.Report;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.report.HtmlReportRenderer;
import com.sqlcompat.core.report.MarkdownReportRenderer;
import com.sqlcompat.core.report.ReportRenderer;
import com.sqlcompat.core.rule.RuleRegistry;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.sqlcompat.core.util.J8;
import com.sqlcompat.core.util.Io;
import java.util.LinkedHashSet;
import java.util.Collections;

/**
 * 把兼容性检查挂到 {@code verify} 阶段。
 *
 * <pre>{@code
 * <plugin>
 *   <groupId>io.github.sqlcompat</groupId>
 *   <artifactId>sql-compat-maven-plugin</artifactId>
 *   <version>0.1.0</version>
 *   <configuration>
 *     <input>${project.basedir}/src/main/resources/mapper</input>
 *     <failOn>BLOCKER</failOn>
 *   </configuration>
 *   <executions>
 *     <execution><goals><goal>check</goal></goals></execution>
 *   </executions>
 * </plugin>
 * }</pre>
 *
 * <p><b>找不到 mapper 就跳过而不是失败</b>：插件要能无痛加到任意项目上，
 * 一个没有 mapper 的模块不该因为多配了一段 pom 就红。
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class CheckMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /** 要扫描的目录或单个文件。 */
    @Parameter(property = "sqlcompat.input", defaultValue = "${project.basedir}/src/main/resources")
    private File input;

    /** 文件名通配符，逗号分隔或逐个配置。 */
    @Parameter(property = "sqlcompat.includes")
    private List<String> includes;

    /** 报告格式：{@code html} 或 {@code md}。 */
    @Parameter(property = "sqlcompat.format", defaultValue = "html")
    private String format;

    /** 报告输出到哪个目录，文件名固定为 {@code sql-compat-report.<ext>}。 */
    @Parameter(property = "sqlcompat.outputDirectory",
            defaultValue = "${project.build.directory}/sql-compat")
    private File outputDirectory;

    /** 达到该级别即让构建失败：BLOCKER / MAJOR / MINOR / INFO。不配则只出报告。 */
    @Parameter(property = "sqlcompat.failOn")
    private String failOn;

    /** 忽略的规则编号，逗号分隔。 */
    @Parameter(property = "sqlcompat.ignore")
    private List<String> ignore;

    /** 跳过整个检查。 */
    @Parameter(property = "sqlcompat.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoFailureException {
        if (skip) {
            getLog().info("sql-compat 已跳过（sqlcompat.skip=true）");
            return;
        }
        if (input == null || !input.exists()) {
            getLog().info("sql-compat：输入路径不存在，跳过 -> " + input);
            return;
        }

        GlobMatcher globs = GlobMatcher.of(includes);
        CompatEngine engine = new CompatEngine(filteredRegistry());
        Report report;
        try {
            report = engine.scan(input.toPath(), "MySQL", "Kingbase/PostgreSQL",
                    path -> globs.matches(path) && MapperSniffer.looksLikeMapper(path));
        } catch (IOException e) {
            throw new MojoFailureException("sql-compat 扫描失败：" + e.getMessage(), e);
        }

        if (report.statementCount() == 0) {
            getLog().info("sql-compat：未发现 Mapper / SQL 文件，跳过 -> " + input);
            return;
        }

        Path reportFile = writeReport(report);

        getLog().info(String.format(Locale.ROOT,
                "sql-compat：扫描 %d 条语句，问题 %d 个（blocker=%d major=%d minor=%d info=%d）",
                report.statementCount(),
                report.issueCount(),
                report.issueCountBySeverity().getOrDefault(Severity.BLOCKER, 0),
                report.issueCountBySeverity().getOrDefault(Severity.MAJOR, 0),
                report.issueCountBySeverity().getOrDefault(Severity.MINOR, 0),
                report.issueCountBySeverity().getOrDefault(Severity.INFO, 0)));
        if (report.parseErrorCount() > 0) {
            getLog().info("sql-compat：另有 " + report.parseErrorCount()
                    + " 条语句未解析成 AST（不影响文本级规则，详见报告）");
        }
        getLog().info("sql-compat：报告 -> " + reportFile);

        Severity threshold = parseSeverity(failOn);
        if (threshold != null) {
            Severity worst = report.worstSeverity();
            if (worst != null && worst.atLeast(threshold)) {
                throw new MojoFailureException(String.format(Locale.ROOT,
                        "sql-compat 检查未通过：最高级别 %s 达到或超过阈值 %s，详见 %s",
                        worst, threshold, reportFile));
            }
        }
    }

    private RuleRegistry filteredRegistry() {
        RuleRegistry registry = RuleRegistry.defaults();
        if (ignore == null || ignore.isEmpty()) {
            return registry;
        }
        // 允许写 "R011,PARSE" 这种逗号串
        List<String> ids = ignore.stream()
                .flatMap(value -> J8.list(value.split(",")).stream())
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        return registry.excluding(ids);
    }

    private Path writeReport(Report report) throws MojoFailureException {
        ReportRenderer renderer = "md".equalsIgnoreCase(format)
                ? new MarkdownReportRenderer()
                : new HtmlReportRenderer();
        Path dir = outputDirectory.toPath();
        Path target = dir.resolve("sql-compat-report." + renderer.extension());
        try {
            Files.createDirectories(dir);
            Io.writeString(target, renderer.render(report), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoFailureException("sql-compat 写报告失败：" + e.getMessage(), e);
        }
        project.getProperties().setProperty("sqlcompat.report", target.toAbsolutePath().toString());
        return target;
    }

    private Severity parseSeverity(String value) throws MojoFailureException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Severity.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new MojoFailureException("sqlcompat.failOn 取值非法：" + value
                    + "（可选 BLOCKER / MAJOR / MINOR / INFO）", e);
        }
    }

    /** 供测试使用。 */
    Set<String> configuredIgnores() {
        return ignore == null ? J8.set() : java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(ignore));
    }
}
