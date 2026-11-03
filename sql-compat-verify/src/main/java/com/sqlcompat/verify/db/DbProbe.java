package com.sqlcompat.verify.db;

import com.sqlcompat.verify.model.QueryOutcome;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 在单个连接目标上执行用例并计时。
 *
 * <p>每个目标只开一个连接、全程复用：如果每条用例都新建连接，TCP 握手与认证会被算进测量窗口，
 * 而两个驱动在这上面的开销并不相同。
 *
 * <p>耗时取预热后若干轮的中位数。这是**指示性的跨引擎对比**，不是 benchmark ——
 * 引擎不同、优化器不同、默认存储也不同。
 */
public final class DbProbe implements AutoCloseable {

    private static final int WARMUP_ITERATIONS = 2;
    private static final int MEASURED_ITERATIONS = 5;
    private static final int QUERY_TIMEOUT_SECONDS = 30;

    private final DbEndpoint endpoint;
    private final Connection connection;

    public DbProbe(DbEndpoint endpoint) throws SQLException {
        this.endpoint = endpoint;
        this.connection = DriverManager.getConnection(
                endpoint.url(), endpoint.user(), endpoint.password());
        this.connection.setAutoCommit(true);
    }

    public DbEndpoint endpoint() {
        return endpoint;
    }

    /** 只执行一次，不预热也不计时。用于建表造数脚本。 */
    public QueryOutcome runOnce(String sql) {
        long started = System.nanoTime();
        try {
            List<List<String>> rows = execute(sql);
            long elapsed = (System.nanoTime() - started) / 1_000_000L;
            return new QueryOutcome(rows, elapsed, null);
        } catch (SQLException e) {
            return QueryOutcome.failure(e.getClass().getSimpleName() + ": " + firstLine(e.getMessage()));
        }
    }

    /** 执行语句，返回归一化后的行与中位耗时。 */
    public QueryOutcome run(String sql) {
        List<List<String>> rows = null;
        List<Long> samples = new ArrayList<>(MEASURED_ITERATIONS);
        try {
            for (int i = 0; i < WARMUP_ITERATIONS + MEASURED_ITERATIONS; i++) {
                long started = System.nanoTime();
                rows = execute(sql);
                long elapsed = (System.nanoTime() - started) / 1_000_000L;
                if (i >= WARMUP_ITERATIONS) {
                    samples.add(elapsed);
                }
            }
            return new QueryOutcome(rows, median(samples), null);
        } catch (SQLException e) {
            return QueryOutcome.failure(e.getClass().getSimpleName() + ": " + firstLine(e.getMessage()));
        }
    }

    private List<List<String>> execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            boolean isResultSet = statement.execute(sql);
            if (isResultSet) {
                try (ResultSet rs = statement.getResultSet()) {
                    return readRows(rs);
                }
            }
            // DML：比较影响行数，而不是结果集。
            return List.of(List.of(String.valueOf(statement.getUpdateCount())));
        }
    }

    private List<List<String>> readRows(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        List<List<String>> rows = new ArrayList<>();
        while (rs.next()) {
            List<String> row = new ArrayList<>(columnCount);
            for (int c = 1; c <= columnCount; c++) {
                row.add(ValueNormalizer.normalize(rs.getObject(c)));
            }
            rows.add(List.copyOf(row));
        }
        return List.copyOf(rows);
    }

    private long median(List<Long> samples) {
        if (samples.isEmpty()) {
            return 0L;
        }
        long[] sorted = samples.stream().mapToLong(Long::longValue).sorted().toArray();
        return sorted[sorted.length / 2];
    }

    private String firstLine(String message) {
        if (message == null) {
            return "";
        }
        return Arrays.stream(message.split("\\R")).findFirst().orElse("").trim();
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
