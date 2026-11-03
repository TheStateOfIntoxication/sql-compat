package com.sqlcompat.core.engine;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Report;
import com.sqlcompat.core.model.ScanResult;
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
import java.util.stream.Stream;

/**
 * Scans a file or a directory tree and produces a {@link Report}.
 *
 * <p>Supported input: {@code *.xml} MyBatis mappers and {@code *.sql} files containing
 * statements separated by {@code ;}.
 */
public final class CompatEngine {

    private final RuleRegistry registry;
    private final MyBatisXmlParser xmlParser = new MyBatisXmlParser();
    private final SqlParserFacade sqlParser = new SqlParserFacade();

    public CompatEngine(RuleRegistry registry) {
        this.registry = registry;
    }

    public CompatEngine() {
        this(RuleRegistry.defaults());
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
            results.add(new ScanResult(statement, issues, outcome.ok() ? null : outcome.error()));
        }
        Duration elapsed = Duration.between(started, Instant.now());
        return new Report(input.toString(), sourceDialect, targetDialect, results, elapsed);
    }

    /** Collects statements from a mapper XML file, a {@code .sql} file, or a directory. */
    public List<SqlStatement> collectStatements(Path input) throws IOException {
        List<Path> files = new ArrayList<>();
        if (Files.isDirectory(input)) {
            try (Stream<Path> walk = Files.walk(input)) {
                walk.filter(Files::isRegularFile)
                        .filter(p -> isMapperXml(p) || isSqlFile(p))
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(files::add);
            }
        } else if (Files.isRegularFile(input)) {
            files.add(input);
        } else {
            throw new IOException("Input not found: " + input);
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
                    com.sqlcompat.core.model.SourceLocation.of(file.getFileName().toString(), i + 1)));
        }
        return result;
    }

    /**
     * Drops {@code --} line comments from a statement chunk.
     *
     * <p>Earlier versions skipped a chunk outright when it started with a comment, which silently
     * dropped any statement that had a comment above it — the DDL fixtures made that obvious.
     * Known limitation: a line whose literal value starts with {@code --} is dropped as well.
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
