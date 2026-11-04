package com.sqlcompat.core.util;

import java.io.IOException;
import java.nio.charset.Charset;
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

    /**
     * 单文件读取上限：8 MB。
     *
     * <p>Mapper XML 与 SQL 脚本都在几十 KB 量级，8 MB 已经留了两个数量级的余量。
     * 设上限是为了让「喂错路径」变成一条明确报错，而不是把几百 MB 的二进制文件
     * 读进堆里、在 OOM 堆栈里找原因。
     */
    private static final long MAX_READ_BYTES = 8L * 1024 * 1024;

    private Io() {
    }

    /** 以 UTF-8 读整个文件。 */
    public static String readString(Path path) throws IOException {
        return readString(path, StandardCharsets.UTF_8);
    }

    /** 按指定字符集读整个文件（保留原有调用点的显式字符集写法）。 */
    public static String readString(Path path, Charset charset) throws IOException {
        long size = Files.size(path);
        if (size > MAX_READ_BYTES) {
            throw new IOException("文件过大，拒绝读取：" + path + "（" + size
                    + " 字节，上限 " + MAX_READ_BYTES + " 字节）");
        }
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
    public static Path writeString(Path path, CharSequence content, Charset charset) throws IOException {
        return Files.write(path, content.toString().getBytes(charset));
    }
}
