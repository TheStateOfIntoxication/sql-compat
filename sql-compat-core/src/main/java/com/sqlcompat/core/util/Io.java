package com.sqlcompat.core.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Java 8 兼容的文件读写。
 *
 * <p>存在的原因：{@code Files.readString / writeString} 是 Java 11 才有的 API，
 * 而本工具要能在 JDK 8 的 Maven 里运行。语义保持一致：UTF-8、覆盖写、父目录不存在时报错。
 */
public final class Io {

    private Io() {
    }

    /** 以 UTF-8 读整个文件。 */
    public static String readString(Path path) throws IOException {
        return readString(path, StandardCharsets.UTF_8);
    }

    /** 按指定字符集读整个文件（保留原有调用点的显式字符集写法）。 */
    public static String readString(Path path, java.nio.charset.Charset charset) throws IOException {
        return new String(Files.readAllBytes(path), charset);
    }

    /**
     * 以 UTF-8 覆盖写整个文件，返回被写的路径。
     *
     * <p>返回 Path 是为了对齐 {@code Files.writeString} 的语义 —— 调用点常写成
     * {@code Path p = Io.writeString(...)}，返回 void 会让它们编译不过。
     */
    public static Path writeString(Path path, CharSequence content) throws IOException {
        return writeString(path, content, StandardCharsets.UTF_8);
    }

    /** 按指定字符集覆盖写，返回被写的路径。 */
    public static Path writeString(Path path, CharSequence content, java.nio.charset.Charset charset)
            throws IOException {
        return Files.write(path, content.toString().getBytes(charset));
    }

    /** 以 UTF-8 追加写。 */
    public static void appendString(Path path, CharSequence content) throws IOException {
        Files.write(path, content.toString().getBytes(StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }

    /** 便于在 lambda 里调用：把受检的 {@link IOException} 包成非受检。 */
    public static String readUnchecked(Path path) {
        try {
            return readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
