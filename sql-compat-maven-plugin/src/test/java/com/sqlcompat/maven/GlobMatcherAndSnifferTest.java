package com.sqlcompat.maven;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import com.sqlcompat.core.util.J8;
import com.sqlcompat.core.util.Io;
import java.nio.file.Paths;
import java.util.regex.Matcher;

/**
 * 插件的两个前置判断：include 通配符与"这到底是不是 mapper"。
 *
 * <p>这两个判断存在的唯一理由是**别把构建搞红**：把 logback.xml 当 mapper 解析会抛异常，
 * 而没有 mapper 的模块不该因为多配一段 pom 就失败。
 */
class GlobMatcherAndSnifferTest {

    @Test
    @DisplayName("** 跨目录、* 不跨目录")
    void globSemantics() {
        GlobMatcher matcher = GlobMatcher.of(J8.list("**/*.xml"));

        assertThat(matcher.matches(Paths.get("DemoMapper.xml"))).isTrue();
        assertThat(matcher.matches(Paths.get("mapper", "DemoMapper.xml"))).isTrue();
        assertThat(matcher.matches(Paths.get("a", "b", "c", "DemoMapper.xml"))).isTrue();
        assertThat(matcher.matches(Paths.get("mapper", "DemoMapper.sql"))).isFalse();
    }

    @Test
    @DisplayName("单层 * 不跨目录")
    void singleStarDoesNotCrossDirectories() {
        GlobMatcher matcher = GlobMatcher.of(J8.list("mapper/*.xml"));

        assertThat(matcher.matches(Paths.get("mapper", "DemoMapper.xml"))).isTrue();
        assertThat(matcher.matches(Paths.get("mapper", "sub", "DemoMapper.xml"))).isFalse();
    }

    @Test
    @DisplayName("多个 include 命中其一即可")
    void multipleIncludes() {
        GlobMatcher matcher = GlobMatcher.of(J8.list("**/Demo*.xml", "**/*.sql"));

        assertThat(matcher.matches(Paths.get("mapper", "DemoMapper.xml"))).isTrue();
        assertThat(matcher.matches(Paths.get("mapper", "Other.xml"))).isFalse();
        assertThat(matcher.matches(Paths.get("sql", "legacy.sql"))).isTrue();
    }

    @Test
    @DisplayName("Windows 反斜杠路径也能匹配")
    void windowsPathsAreNormalized() {
        GlobMatcher matcher = GlobMatcher.of(J8.list("**/*.xml"));

        assertThat(matcher.matches(Paths.get("D:", "work", "mapper", "DemoMapper.xml"))).isTrue();
    }

    @Test
    @DisplayName("没配 include 时一律放行")
    void emptyIncludesMatchEverything() {
        assertThat(GlobMatcher.of(J8.list()).matches(Paths.get("any", "thing.xml"))).isTrue();
        assertThat(GlobMatcher.of(null).matches(Paths.get("any", "thing.sql"))).isTrue();
    }

    @Test
    @DisplayName("只有带 <mapper 的 XML 才算 mapper，logback.xml 不算")
    void snifferRecognisesMapperXml(@TempDir Path dir) throws IOException {
        Path mapper = Io.writeString(dir.resolve("DemoMapper.xml"),
                "<mapper namespace=\"demo\">\n</mapper>");
        Path logback = Io.writeString(dir.resolve("logback.xml"),
                "<configuration><appender name=\"A\"/></configuration>");
        Path sql = Io.writeString(dir.resolve("legacy.sql"), "SELECT 1;");

        assertThat(MapperSniffer.looksLikeMapper(mapper)).isTrue();
        assertThat(MapperSniffer.looksLikeMapper(logback)).isFalse();
        assertThat(MapperSniffer.looksLikeMapper(sql)).as(".sql 一律放行").isTrue();
    }

    @Test
    @DisplayName("带注释与 BOM 的 mapper 也能识别")
    void snifferHandlesBomAndComments(@TempDir Path dir) throws IOException {
        String content = "\uFEFF<?xml version=\"1.0\"?>\n<!-- 注释 -->\n<!DOCTYPE mapper>\n<mapper namespace=\"demo\"/>";
        Path file = Io.writeString(dir.resolve("WithBom.xml"), content);

        assertThat(MapperSniffer.looksLikeMapper(file)).isTrue();
    }

    @Test
    @DisplayName("非 xml / sql 后缀直接排除")
    void snifferRejectsOtherExtensions(@TempDir Path dir) throws IOException {
        Path text = Io.writeString(dir.resolve("readme.txt"), "<mapper/>");

        assertThat(MapperSniffer.looksLikeMapper(text)).isFalse();
    }
}
