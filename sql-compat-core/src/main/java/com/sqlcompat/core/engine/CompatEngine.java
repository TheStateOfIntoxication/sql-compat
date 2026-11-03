package com.sqlcompat.core.engine;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Report;
import com.sqlcompat.core.model.ScanResult;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.model.SourceLocation;
import com.sqlcompat.core.model.SqlStatement;
import com.sqlcompat.core.parser.MyBatisXmlParser;
import com.sqlcompat.core.parser.SqlParserFacade;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;
import com.sqlcompat.core.rule.RuleRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 扫描一个文件或一棵目录树，产出 {@link Report}。
 *
 * <p>支持的输入：{@code *.xml}（MyBatis Mapper）、{@code *.sql}（以 {@code ;} 分隔多条语句）。
 */
public final class CompatEngine {

    /** 解析失败时使用的伪规则编号，便于在报告与 {@code --ignore} 中精确定位。 */
    public static final String PARSE_ISSUE_RULE_ID = "PARSE";

    private final RuleRegistry registry;
    private final Set<String> ignoredRuleIds;
    private final MyBatisXmlParser xmlParser = new MyBatisXmlParser();
    private final SqlParserFacade sqlParser = new SqlParserFacade();

    public CompatEngine(RuleRegistry registry) {
        this(registry, Set.of());
    }

    /** @param ignoredRuleIds 需要屏蔽的规则编号；含 {@link #PARSE_ISSUE_RULE_ID} 时不再报解析失败 */
    public CompatEngine(RuleRegistry registry, Set<String> ignoredRuleIds) {
        this.registry = registry;
        this.ignoredRuleIds = ignoredRuleIds == null ? Set.of() : Set.copyOf(ignoredRuleIds);
    }

    public CompatEngine() {
        this(RuleRegistry.defaults(), Set.of());
    }

    public Report scan(Path input, String sourceDialect, String targetDialect) throws IOException {
        Instant started = Instant.now();
        List<SqlStatement> statements = collectStatements(input);
        List<ScanResult> results = new ArrayList<>();
        for (SqlStatement statement : statements) {
            SqlParserFacade.ParseOutcome outcome = sqlParser.parse(statement.sqlText());
            RuleContext context = new RuleContext(statement, outcome.ast());
            List<Issue> issues = new ArrayList<>();
            for (DialectRule rule : registry.rules()) {
                issues.addAll(rule.check(context));
            }
            if (!outcome.ok() && !parseIssuesIgnored()) {
                // 解析失败不当作错误，但必须**看得见**：否则用户无从知道哪些语句只跑了文本级规则。
                issues.add(new Issue(
                        PARSE_ISSUE_RULE_ID,
                        "语句无法解析成 AST",
                        Severity.INFO,
                        "解析失败，本条只跑了文本级规则。**当前 20 条内置规则均为文本级，因此检出结果不受影响**；"
                                + "此条仅作记录，后续引入 AST 规则时需先修复此类语句。原因：" + outcome.error(),
                        firstLine(statement.sqlText()),
                        "常见原因：Oracle 的 INSERT ALL / CONNECT BY、一个 update 里拼了多条语句、动态标签展平后缺关键字；"
                                + "逐类说明见 docs/known-limitations.md",
                        statement.location()));
            }
            results.add(new ScanResult(statement, issues, outcome.ok() ? null : outcome.error()));
        }
        Duration elapsed = Duration.between(started, Instant.now());
        return new Report(input.toString(), sourceDialect, targetDialect, results, elapsed);
    }

    private boolean parseIssuesIgnored() {
        return ignoredRuleIds.stream().anyMatch(id -> id.equalsIgnoreCase(PARSE_ISSUE_RULE_ID));
    }

    private String firstLine(String sql) {
        String oneLine = sql.replaceAll("\\s+", " ").strip();
        return oneLine.length() > 120 ? oneLine.substring(0, 117) + "..." : oneLine;
    }

    /** 从 Mapper XML、{@code .sql} 文件或目录中采集语句。 */
    public List<SqlStatement> collectStatements(Path input) throws IOException {
        List<Path> files = new ArrayList<>();
        if (Files.isDirectory(input)) {
            try (Stream<Path> walk = Files.walk(input)) {
                walk.filter(Files::isRegularFile)
                        .filter(p -> isMapperXml(p) || isSqlFile(p))
                        // 排序是为了一次扫描的产出稳定，CI 里才能 diff 报告。
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(files::add);
            }
        } else if (Files.isRegularFile(input)) {
            files.add(input);
        } else {
            throw new IOException("输入路径不存在：" + input);
        }

        List<SqlStatement> statements = new ArrayList<>();
        for (Path file : files) {
            if (isMapperXml(file)) {
                statements.addAll(xmlParser.parse(file));
            } else {
                statements.addAll(parseSqlFile(file));
            }
        }
        return statements;
    }

    private List<SqlStatement> parseSqlFile(Path file) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        List<SqlStatement> result = new ArrayList<>();
        String[] parts = content.split(";");
        for (int i = 0; i < parts.length; i++) {
            String sql = stripLineComments(parts[i]).strip();
            if (sql.isEmpty()) {
                continue;
            }
            result.add(new SqlStatement(
                    "",
                    file.getFileName() + "#" + (i + 1),
                    sql,
                    SourceLocation.of(file.getFileName().toString(), i + 1)));
        }
        return result;
    }

    /**
     * 剥掉语句块里的 {@code --} 行注释。
     *
     * <p>早期版本遇到以注释开头的语句块就整块跳过，导致"上面有注释的语句"全部被静默丢弃
     * ——DDL fixture 把这个问题暴露了出来。
     * 已知局限：字面量本身以 {@code --} 开头的行也会被当成注释。
     */
    private String stripLineComments(String chunk) {
        StringBuilder sb = new StringBuilder(chunk.length());
        for (String line : chunk.split("\\R")) {
            if (line.stripLeading().startsWith("--")) {
                continue;
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private boolean isMapperXml(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".xml");
    }

    private boolean isSqlFile(Path path) {
        return path.getFileName().toString().toLowerCase().endsWith(".sql");
    }
}

