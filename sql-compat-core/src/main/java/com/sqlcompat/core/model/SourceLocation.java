package com.sqlcompat.core.model;

import java.util.Objects;

/**
 * 问题所在位置。{@code line} 从 1 开始，未知时为 {@code 0}。
 *
 * <p>写成普通类而不是 {@code record}：本工具要求运行在 JDK 8 上（信创现场大量 Maven 就是
 * JDK 8，插件必须能在其中被加载），而 record 是 Java 16 的语法。
 */
public final class SourceLocation {

    public static final SourceLocation UNKNOWN = new SourceLocation("<unknown>", 0);

    private final String file;
    private final int line;

    public SourceLocation(String file, int line) {
        this.file = file;
        this.line = line;
    }

    public static SourceLocation of(String file, int line) {
        return new SourceLocation(file, line);
    }

    public String file() {
        return file;
    }

    public int line() {
        return line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SourceLocation)) {
            return false;
        }
        SourceLocation that = (SourceLocation) o;
        return line == that.line && Objects.equals(file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, line);
    }

    @Override
    public String toString() {
        return line > 0 ? file + ":" + line : file;
    }
}
