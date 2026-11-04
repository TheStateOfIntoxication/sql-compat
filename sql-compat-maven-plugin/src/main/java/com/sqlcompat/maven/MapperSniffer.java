package com.sqlcompat.maven;

import java.io.InputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * 判断一个 XML 到底是不是 MyBatis Mapper。
 *
 * <p>{@code src/main/resources} 下还有 {@code logback.xml}、{@code mybatis-config.xml} 这类文件，
 * 把它们当 mapper 解析会直接抛异常、把构建搞红。所以先闻一下前 4KB 里有没有 {@code <mapper}。
 */
final class MapperSniffer {

    private static final int SNIFF_BYTES = 4096;

    private MapperSniffer() {
    }

    /** {@code .sql} 一律放行；{@code .xml} 必须有 {@code <mapper} 根元素。 */
    static boolean looksLikeMapper(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".sql")) {
            return true;
        }
        if (!name.endsWith(".xml")) {
            return false;
        }
        try {
            byte[] head = readHead(path);
            String text = new String(head, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            return text.contains("<mapper");
        } catch (IOException e) {
            // 读不出来就交给引擎去报错，这里不做二次判断
            return true;
        }
    }

    private static byte[] readHead(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[SNIFF_BYTES];
            int read = in.read(buffer);
            if (read <= 0) {
                return new byte[0];
            }
            byte[] result = new byte[read];
            System.arraycopy(buffer, 0, result, 0, read);
            return result;
        }
    }
}
