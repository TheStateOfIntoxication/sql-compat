package com.sqlcompat.verify;

import com.sqlcompat.verify.db.DbEndpoint;
import com.sqlcompat.verify.db.DbProbe;
import com.sqlcompat.verify.model.CaseOutcome;
import com.sqlcompat.verify.model.ComparisonReport;
import com.sqlcompat.verify.model.QueryOutcome;
import com.sqlcompat.verify.model.SqlCase;
import com.sqlcompat.verify.model.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 把一套用例同时跑在 MySQL 与 PostgreSQL 上，报告其中有多少是一致的。
 *
 * <p>用例执行前会先在两个库上应用建表造数脚本，这样全新的 CI 容器和开发者本地库表现一致。
 */
public final class DualDbComparator {

    private final DbEndpoint mysql;
    private final DbEndpoint postgres;
    private final ResultSetDiffer differ = new ResultSetDiffer();

    public DualDbComparator(DbEndpoint mysql, DbEndpoint postgres) {
        this.mysql = mysql;
        this.postgres = postgres;
    }

    /**
     * @param setupSql 可选：先在两库上执行的 DDL 与造数语句
     * @param cases    要跑的用例
     */
    public ComparisonReport compare(Path setupSql, List<SqlCase> cases) throws IOException, SQLException {
        try (DbProbe mysqlProbe = new DbProbe(mysql);
             DbProbe postgresProbe = new DbProbe(postgres)) {

            if (setupSql != null) {
                applySetup(mysqlProbe, postgresProbe, setupSql);
            }

            List<CaseOutcome> outcomes = new ArrayList<>();
            for (SqlCase sqlCase : cases) {
                outcomes.add(runCase(mysqlProbe, postgresProbe, sqlCase));
            }
            return new ComparisonReport(mysql.label(), postgres.label(), outcomes);
        }
    }

    private void applySetup(DbProbe mysqlProbe, DbProbe postgresProbe, Path setupSql)
            throws IOException, SQLException {
        String script = Files.readString(setupSql, StandardCharsets.UTF_8);
        for (String statement : script.split(";")) {
            String sql = stripComments(statement).strip();
            if (sql.isEmpty()) {
                continue;
            }
            QueryOutcome left = mysqlProbe.runOnce(sql);
            if (left.failed()) {
                throw new SQLException("建表造数在 " + mysql.label() + " 侧失败：" + left.error()
                        + "\n  出错语句：" + sql);
            }
            QueryOutcome right = postgresProbe.runOnce(sql);
            if (right.failed()) {
                throw new SQLException("建表造数在 " + postgres.label() + " 侧失败：" + right.error()
                        + "\n  出错语句：" + sql);
            }
        }
    }

    private CaseOutcome runCase(DbProbe mysqlProbe, DbProbe postgresProbe, SqlCase sqlCase) {
        QueryOutcome left = mysqlProbe.run(sqlCase.sql());
        QueryOutcome right = postgresProbe.run(sqlCase.sql());

        if (left.failed() || right.failed()) {
            StringBuilder detail = new StringBuilder();
            if (left.failed()) {
                detail.append("mysql: ").append(left.error());
            }
            if (right.failed()) {
                if (!detail.isEmpty()) {
                    detail.append(" | ");
                }
                detail.append("postgres: ").append(right.error());
            }
            return CaseOutcome.error(sqlCase, detail.toString(), left, right);
        }

        String diff = differ.diff(sqlCase, left.rows(), right.rows());
        Status status = diff == null ? Status.CONSISTENT : Status.DIFFERENT;
        return new CaseOutcome(sqlCase, status, diff, left, right);
    }

    private String stripComments(String chunk) {
        StringBuilder sb = new StringBuilder(chunk.length());
        for (String line : chunk.split("\\R")) {
            if (line.stripLeading().startsWith("--")) {
                continue;
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
