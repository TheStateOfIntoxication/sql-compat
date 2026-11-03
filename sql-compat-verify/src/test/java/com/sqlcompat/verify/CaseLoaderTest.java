package com.sqlcompat.verify;

import com.sqlcompat.verify.model.SqlCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaseLoaderTest {

    @Test
    @DisplayName("one file is one case, id comes from the file name, sorted for stable reports")
    void loadsCasesInStableOrder(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("02_second.sql"), "SELECT 2;");
        Files.writeString(dir.resolve("01_first.sql"), "SELECT 1;");
        Files.writeString(dir.resolve("readme.txt"), "ignored");

        List<SqlCase> cases = new CaseLoader().load(dir);

        assertThat(cases).extracting(SqlCase::id).containsExactly("01_first", "02_second");
        assertThat(cases.getFirst().sql()).isEqualTo("SELECT 1");
    }

    @Test
    @DisplayName("ORDER BY marks the case as order-sensitive")
    void detectsOrderedCase(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("a_ordered.sql"), "SELECT id FROM t ORDER BY id;");
        Files.writeString(dir.resolve("b_unordered.sql"), "SELECT id FROM t;");

        List<SqlCase> cases = new CaseLoader().load(dir);

        assertThat(cases).filteredOn(SqlCase::ordered).extracting(SqlCase::id).containsExactly("a_ordered");
        assertThat(cases).filteredOn(c -> !c.ordered()).extracting(SqlCase::id).containsExactly("b_unordered");
    }

    @Test
    @DisplayName("comment-only files are dropped instead of producing empty cases")
    void skipsCommentOnlyFile(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("empty.sql"), "-- just a note\n-- and another\n");

        assertThat(new CaseLoader().load(dir)).isEmpty();
    }

    @Test
    @DisplayName("line comments are stripped so the executed SQL stays valid")
    void stripsComments(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("case.sql"), "-- leading note\nSELECT 1;\n-- trailing note\n");

        assertThat(new CaseLoader().load(dir).getFirst().sql()).isEqualTo("SELECT 1");
    }
}
