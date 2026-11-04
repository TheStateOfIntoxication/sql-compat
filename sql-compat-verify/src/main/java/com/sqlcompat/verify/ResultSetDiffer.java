package com.sqlcompat.verify;

import com.sqlcompat.verify.model.SqlCase;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 逐单元格比较两侧结果集，并说明第一处差异。
 *
 * <p>行序规则：用例没有 {@code ORDER BY} 时，两侧先排序再比较。否则每条无序查询都会因为两个引擎
 * 返回的物理行序不同而被判为不一致 —— 而那种差异本来就是允许的。
 * 用例带 {@code ORDER BY} 时，行序是契约的一部分，按位比较。
 */
public final class ResultSetDiffer {

    /**
     * @return 两侧一致时返回 {@code null}，否则返回一句可读的差异原因
     */
    public String diff(SqlCase sqlCase, List<List<String>> mysqlRows, List<List<String>> postgresRows) {
        List<List<String>> left = sqlCase.ordered() ? mysqlRows : sorted(mysqlRows);
        List<List<String>> right = sqlCase.ordered() ? postgresRows : sorted(postgresRows);

        if (left.size() != right.size()) {
            return "行数不同：mysql=" + left.size() + "，postgres=" + right.size();
        }
        for (int i = 0; i < left.size(); i++) {
            List<String> a = left.get(i);
            List<String> b = right.get(i);
            if (a.size() != b.size()) {
                return "第 " + i + " 行列数不同：mysql=" + a.size() + "，postgres=" + b.size();
            }
            for (int c = 0; c < a.size(); c++) {
                if (!Objects.equals(a.get(c), b.get(c))) {
                    return "第 " + i + " 行第 " + c + " 列不同：mysql=" + quote(a.get(c))
                            + "，postgres=" + quote(b.get(c));
                }
            }
        }
        return null;
    }

    private List<List<String>> sorted(List<List<String>> rows) {
        return rows.stream()
                .sorted(Comparator.comparing(row -> String.join("\u0001", row)))
                .collect(Collectors.toList());
    }

    private String quote(String value) {
        return value == null ? "null" : "'" + value + "'";
    }
}
