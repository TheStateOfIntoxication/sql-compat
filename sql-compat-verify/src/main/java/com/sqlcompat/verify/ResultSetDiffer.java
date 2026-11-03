package com.sqlcompat.verify;

import com.sqlcompat.verify.model.SqlCase;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Compares two result sets cell by cell and explains the first difference.
 *
 * <p>Ordering rule: when the case has no {@code ORDER BY}, both sides are sorted before comparing.
 * Without that, every unordered query would be reported as inconsistent simply because the two
 * engines return rows in different physical order — a difference that is by definition allowed.
 * When the case does have {@code ORDER BY}, order is part of the contract and is compared as-is.
 */
public final class ResultSetDiffer {

    /**
     * @return {@code null} when the two sides agree, otherwise a short human readable reason
     */
    public String diff(SqlCase sqlCase, List<List<String>> mysqlRows, List<List<String>> postgresRows) {
        List<List<String>> left = sqlCase.ordered() ? mysqlRows : sorted(mysqlRows);
        List<List<String>> right = sqlCase.ordered() ? postgresRows : sorted(postgresRows);

        if (left.size() != right.size()) {
            return "row count differs: mysql=" + left.size() + ", postgres=" + right.size();
        }
        for (int i = 0; i < left.size(); i++) {
            List<String> a = left.get(i);
            List<String> b = right.get(i);
            if (a.size() != b.size()) {
                return "column count differs at row " + i + ": mysql=" + a.size() + ", postgres=" + b.size();
            }
            for (int c = 0; c < a.size(); c++) {
                if (!Objects.equals(a.get(c), b.get(c))) {
                    return "row " + i + ", column " + c + ": mysql=" + quote(a.get(c))
                            + ", postgres=" + quote(b.get(c));
                }
            }
        }
        return null;
    }

    private List<List<String>> sorted(List<List<String>> rows) {
        return rows.stream()
                .sorted(Comparator.comparing(row -> String.join("\u0001", row)))
                .toList();
    }

    private String quote(String value) {
        return value == null ? "null" : "'" + value + "'";
    }
}
