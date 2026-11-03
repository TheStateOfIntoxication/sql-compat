package com.sqlcompat.core.parser;

import com.sqlcompat.core.model.SqlStatement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 动态标签展平的语义。
 *
 * <p>{@code <where>} / {@code <set>} 不能只丢标签 —— 它们负责生成 {@code WHERE} / {@code SET}。
 * 这个错误是在真实项目上暴露的：29 条解析失败里有 13 条由它导致。
 */
class MyBatisXmlParserTest {

    private List<SqlStatement> parse(String xml) throws IOException {
        Path dir = Files.createTempDirectory("mapper");
        Path file = dir.resolve("DemoMapper.xml");
        Files.writeString(file, xml);
        return new MyBatisXmlParser().parse(file);
    }

    @Test
    @DisplayName("<where> 生成 WHERE，并抹掉内层开头的 AND")
    void whereEmitsKeywordAndStripsLeadingAnd() throws IOException {
        List<SqlStatement> statements = parse("""
                <mapper namespace="demo">
                    <select id="byStatus">
                        SELECT a.id FROM demo_asset a
                        <where>
                            <if test="status != null">AND a.status = #{status}</if>
                            <if test="owner != null">AND a.owner_id = #{owner}</if>
                        </where>
                    </select>
                </mapper>
                """);

        String sql = statements.getFirst().sqlText();
        assertThat(sql).contains("WHERE a.status = #{status}");
        assertThat(sql).doesNotContain("WHERE AND");
        assertThat(sql).as("占位符要留给 SqlParserFacade 处理，parser 不做替换").contains("#{status}");
        assertThat(statements.getFirst().hasDynamicTag("where")).isTrue();
    }

    @Test
    @DisplayName("<set> 生成 SET，并抹掉末尾多余逗号")
    void setEmitsKeywordAndStripsTrailingComma() throws IOException {
        List<SqlStatement> statements = parse("""
                <mapper namespace="demo">
                    <update id="touch">
                        UPDATE demo_asset a
                        <set>
                            <if test="name != null">a.asset_name = #{name},</if>
                            <if test="status != null">a.status = #{status},</if>
                        </set>
                        WHERE a.id = #{id}
                    </update>
                </mapper>
                """);

        String sql = statements.getFirst().sqlText();
        assertThat(sql).contains("SET a.asset_name = #{name},");
        assertThat(sql).doesNotContain(", WHERE");
        assertThat(sql).endsWith("WHERE a.id = #{id}");
    }

    @Test
    @DisplayName("空的 <where> / <set> 不留下裸关键字")
    void emptyBlocksProduceNoKeyword() throws IOException {
        List<SqlStatement> statements = parse("""
                <mapper namespace="demo">
                    <select id="plain">
                        SELECT a.id FROM demo_asset a <where></where>
                    </select>
                </mapper>
                """);

        assertThat(statements.getFirst().sqlText()).doesNotContain("WHERE");
    }

    @Test
    @DisplayName("列出的动态标签都会被记录，供规则判断")
    void tracksDynamicTags() throws IOException {
        List<SqlStatement> statements = parse("""
                <mapper namespace="demo">
                    <select id="byOwners">
                        SELECT a.id FROM demo_asset a WHERE a.owner_id IN
                        <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
                    </select>
                </mapper>
                """);

        assertThat(statements.getFirst().dynamicTags()).contains("foreach");
        assertThat(statements.getFirst().hasDynamicTag("FOREACH")).isTrue();
    }

    @Test
    @DisplayName("<choose> 只取一个分支，不能把 when 与 otherwise 拼在一起")
    void chooseTakesOnlyOneBranch() throws IOException {
        List<SqlStatement> statements = parse("""
                <mapper namespace="demo">
                    <select id="paged">
                        SELECT a.id FROM demo_asset a
                        <choose>
                            <when test="col != null">ORDER BY ${col} ${sort}</when>
                            <otherwise>ORDER BY a.id DESC</otherwise>
                        </choose>
                    </select>
                </mapper>
                """);

        String sql = statements.getFirst().sqlText();
        assertThat(sql).contains("ORDER BY ${col} ${sort}");
        assertThat(sql).as("两个分支都拼进来会出现两个 ORDER BY").doesNotContain("ORDER BY a.id DESC");
        assertThat(statements.getFirst().hasDynamicTag("choose")).isTrue();
    }

    @Test
    @DisplayName("相邻的多个 ${} 折叠成一个标识符，否则 ORDER BY t t 不是合法 SQL")
    void adjacentSubstitutionsAreCollapsed() {
        SqlParserFacade facade = new SqlParserFacade();

        assertThat(facade.parse("SELECT a.id FROM demo_asset a ORDER BY ${col} ${sort}").ok())
                .as("ORDER BY ${col} ${sort} 必须能被解析")
                .isTrue();
        assertThat(facade.parse("SELECT a.id FROM demo_asset a WHERE a.name = ${name}").ok())
                .isTrue();
        assertThat(facade.parse("SELECT a.id FROM demo_asset a ORDER BY ${a} ${b} ${c}").ok())
                .isTrue();
        assertThat(facade.parse("SELECT a.id FROM demo_asset a ORDER BY \"${col}\" ${sort}").ok())
                .as("双引号包住的拼接占位符也要能解析")
                .isTrue();
    }}
