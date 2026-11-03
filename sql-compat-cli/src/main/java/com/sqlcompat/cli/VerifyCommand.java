package com.sqlcompat.cli;

import com.sqlcompat.verify.CaseLoader;
import com.sqlcompat.verify.DualDbComparator;
import com.sqlcompat.verify.db.DbEndpoint;
import com.sqlcompat.verify.model.ComparisonReport;
import com.sqlcompat.verify.model.SqlCase;
import com.sqlcompat.verify.report.ComparisonReportRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * {@code sql-compat verify --mysql-url ... --pg-url ... -c ./cases}
 *
 * <p>工具的第二层：静态规则找语法问题，这一层找"两边都能跑、但结果不一样"的用例。
 *
 * <p>CI 里凭据应走环境变量，不要写在命令行 —— 进程参数会进 shell 历史与构建日志。
 */
@Command(
        name = "verify",
        mixinStandardHelpOptions = true,
        description = "把同一套 SQL 用例跑在 MySQL 与 PostgreSQL 上，报告一致性。")
public final class VerifyCommand implements Callable<Integer> {

    @Option(names = {"--mysql-url"}, description = "MySQL 的 JDBC 连接串。环境变量：SQLCOMPAT_MYSQL_URL")
    String mysqlUrl;

    @Option(names = {"--mysql-user"}, defaultValue = "root", description = "MySQL 用户名")
    String mysqlUser;

    @Option(names = {"--mysql-password"}, description = "MySQL 密码。环境变量：SQLCOMPAT_MYSQL_PASSWORD")
    String mysqlPassword;

    @Option(names = {"--pg-url"}, description = "PostgreSQL 的 JDBC 连接串。环境变量：SQLCOMPAT_PG_URL")
    String pgUrl;

    @Option(names = {"--pg-user"}, defaultValue = "postgres", description = "PostgreSQL 用户名")
    String pgUser;

    @Option(names = {"--pg-password"}, description = "PostgreSQL 密码。环境变量：SQLCOMPAT_PG_PASSWORD")
    String pgPassword;

    @Option(names = {"-c", "--cases"}, required = true, description = "存放 *.sql 用例的目录")
    Path casesDir;

    @Option(names = {"-s", "--setup"}, description = "可选的建表造数脚本，会先在两个库上执行")
    Path setupScript;

    @Option(names = {"-o", "--output"}, description = "报告输出路径，默认 sql-compat-verify.md")
    Path output;

    @Option(names = {"--fail-under"}, description = "一致率低于该百分比时以退出码 1 结束，例如 95")
    Double failUnder;

    @Override
    public Integer call() throws Exception {
        String myUrl = firstNonBlank(mysqlUrl, System.getenv("SQLCOMPAT_MYSQL_URL"));
        String pgUrlResolved = firstNonBlank(pgUrl, System.getenv("SQLCOMPAT_PG_URL"));
        if (myUrl == null || pgUrlResolved == null) {
            System.err.println("必须提供 --mysql-url 与 --pg-url（或对应的环境变量）。");
            return 2;
        }
        String myPassword = firstNonBlank(mysqlPassword, System.getenv("SQLCOMPAT_MYSQL_PASSWORD"));
        String pgPasswordResolved = firstNonBlank(pgPassword, System.getenv("SQLCOMPAT_PG_PASSWORD"));

        List<SqlCase> cases = new CaseLoader().load(casesDir);
        if (cases.isEmpty()) {
            System.err.println("目录里没有找到 .sql 用例：" + casesDir);
            return 2;
        }

        DbEndpoint mysql = new DbEndpoint("mysql", myUrl, mysqlUser, nullToEmpty(myPassword));
        DbEndpoint postgres = new DbEndpoint("postgres", pgUrlResolved, pgUser, nullToEmpty(pgPasswordResolved));

        ComparisonReport report = new DualDbComparator(mysql, postgres).compare(setupScript, cases);

        ComparisonReportRenderer renderer = new ComparisonReportRenderer();
        Path target = output != null ? output : Path.of("sql-compat-verify.md");
        Files.writeString(target, renderer.render(report), StandardCharsets.UTF_8);

        System.out.println(renderer.summaryLine(report));
        System.out.println("报告已写入 " + target.toAbsolutePath());

        if (failUnder != null && report.consistencyRate() < failUnder) {
            System.err.printf(Locale.ROOT, "失败：一致率 %.1f%% 低于阈值 %.1f%%%n",
                    report.consistencyRate(), failUnder);
            return 1;
        }
        return 0;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
