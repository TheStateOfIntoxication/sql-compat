package com.sqlcompat.verify;

import com.sqlcompat.verify.model.SqlCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
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
    @DisplayName("行完全相同即一致")
    void identical() {
        assertThat(differ.diff(unordered(), List.of(List.of("1")), List.of(List.of("1")))).isNull();
    }

    @Test
    @DisplayName("无序用例忽略行序，因为两个引擎本来就可能返回不同顺序")
    void unorderedIgnoresRowOrder() {
        List<List<String>> a = List.of(List.of("1"), List.of("2"));
        List<List<String>> b = List.of(List.of("2"), List.of("1"));

        assertThat(differ.diff(unordered(), a, b)).isNull();
    }

    @Test
    @DisplayName("有序用例尊重行序")
    void orderedRespectsRowOrder() {
        List<List<String>> a = List.of(List.of("1"), List.of("2"));
        List<List<String>> b = List.of(List.of("2"), List.of("1"));

        assertThat(differ.diff(ordered(), a, b)).contains("第 0 行").contains("mysql='1'");
    }

    @Test
    @DisplayName("行数不同会明确报出来")
    void rowCountMismatch() {
        assertThat(differ.diff(unordered(), List.of(List.of("1")), List.of()))
                .isEqualTo("行数不同：mysql=1，postgres=0");
    }

    @Test
    @DisplayName("单元格不同会指出行、列与两侧的值")
    void cellMismatch() {
        String result = differ.diff(unordered(), List.of(List.of("1", "alpha")), List.of(List.of("1", "ALPHA")));

        assertThat(result).isEqualTo("第 0 行第 1 列不同：mysql='alpha'，postgres='ALPHA'");
    }

    @Test
    @DisplayName("null 显示为 null，而不是字符串 'null'")
    void nullCells() {
        String result = differ.diff(unordered(), List.of(Collections.singletonList(null)),
                List.of(List.of("<null>")));

        assertThat(result).contains("mysql=null");
    }
}
