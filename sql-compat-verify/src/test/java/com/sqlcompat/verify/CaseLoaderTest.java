package com.sqlcompat.verify;

import com.sqlcompat.verify.model.SqlCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import com.sqlcompat.core.util.Io;

class CaseLoaderTest {

    @Test
    @DisplayName("一个文件一条用例，编号取自文件名，并按名排序保证报告稳定")
    void loadsCasesInStableOrder(@TempDir Path dir) throws IOException {
        Io.writeString(dir.resolve("02_second.sql"), "SELECT 2;");
        Io.writeString(dir.resolve("01_first.sql"), "SELECT 1;");
        Io.writeString(dir.resolve("readme.txt"), "ignored");

        List<SqlCase> cases = new CaseLoader().load(dir);

        assertThat(cases).extracting(SqlCase::id).containsExactly("01_first", "02_second");
        assertThat(cases.get(0).sql()).isEqualTo("SELECT 1");
    }

    @Test
    @DisplayName("含 ORDER BY 的用例标记为对行序敏感")
    void detectsOrderedCase(@TempDir Path dir) throws IOException {
        Io.writeString(dir.resolve("a_ordered.sql"), "SELECT id FROM t ORDER BY id;");
        Io.writeString(dir.resolve("b_unordered.sql"), "SELECT id FROM t;");

        List<SqlCase> cases = new CaseLoader().load(dir);

        assertThat(cases).filteredOn(SqlCase::ordered).extracting(SqlCase::id).containsExactly("a_ordered");
        assertThat(cases).filteredOn(c -> !c.ordered()).extracting(SqlCase::id).containsExactly("b_unordered");
    }

    @Test
    @DisplayName("纯注释文件被丢弃，不会产生空用例")
    void skipsCommentOnlyFile(@TempDir Path dir) throws IOException {
        Io.writeString(dir.resolve("empty.sql"), "-- 只有注释\n-- 再来一行\n");

        assertThat(new CaseLoader().load(dir)).isEmpty();
    }

    @Test
    @DisplayName("行注释被剥掉，执行时的 SQL 保持合法")
    void stripsComments(@TempDir Path dir) throws IOException {
        Io.writeString(dir.resolve("case.sql"), "-- 前置说明\nSELECT 1;\n-- 后置说明\n");

        assertThat(new CaseLoader().load(dir).get(0).sql()).isEqualTo("SELECT 1");
    }
}
