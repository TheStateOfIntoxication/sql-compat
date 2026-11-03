package com.sqlcompat.verify;

import com.sqlcompat.verify.model.SqlCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultSetDifferTest {

    private final ResultSetDiffer differ = new ResultSetDiffer();

    private SqlCase unordered() {
        return new SqlCase("case", "SELECT id FROM t", false);
    }

    private SqlCase ordered() {
        return new SqlCase("case", "SELECT id FROM t ORDER BY id", true);
    }

    @Test
    @DisplayName("identical rows are consistent")
    void identical() {
        assertThat(differ.diff(unordered(), List.of(List.of("1")), List.of(List.of("1")))).isNull();
    }

    @Test
    @DisplayName("unordered cases ignore row order, because two engines may legitimately differ")
    void unorderedIgnoresRowOrder() {
        List<List<String>> a = List.of(List.of("1"), List.of("2"));
        List<List<String>> b = List.of(List.of("2"), List.of("1"));

        assertThat(differ.diff(unordered(), a, b)).isNull();
    }

    @Test
    @DisplayName("ordered cases respect row order")
    void orderedRespectsRowOrder() {
        List<List<String>> a = List.of(List.of("1"), List.of("2"));
        List<List<String>> b = List.of(List.of("2"), List.of("1"));

        assertThat(differ.diff(ordered(), a, b)).contains("row 0").contains("mysql='1'");
    }

    @Test
    @DisplayName("row count mismatch is reported explicitly")
    void rowCountMismatch() {
        assertThat(differ.diff(unordered(), List.of(List.of("1")), List.of()))
                .isEqualTo("row count differs: mysql=1, postgres=0");
    }

    @Test
    @DisplayName("cell mismatch names the row, the column and both values")
    void cellMismatch() {
        String result = differ.diff(unordered(), List.of(List.of("1", "alpha")), List.of(List.of("1", "ALPHA")));

        assertThat(result).isEqualTo("row 0, column 1: mysql='alpha', postgres='ALPHA'");
    }

    @Test
    @DisplayName("null is shown as null rather than as the string 'null'")
    void nullCells() {
        String result = differ.diff(unordered(), List.of(java.util.Collections.singletonList(null)),
                List.of(List.of("<null>")));

        assertThat(result).contains("mysql=null");
    }
}
