package com.sqlcompat.verify.db;

import java.util.Objects;

/**
 * 一个 JDBC 连接目标。密码只在内存中携带，不会写进任何报告。
 */
public final class DbEndpoint {

    private final String label;
    private final String url;
    private final String user;
    private final String password;

    public DbEndpoint(String label, String url, String user, String password) {
        this.label = label;
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** 报告中显示的短名，例如 {@code mysql}。 */
    public String label() {
        return label;
    }

    /** JDBC 连接串。 */
    public String url() {
        return url;
    }

    /** 用户名。 */
    public String user() {
        return user;
    }

    /** 密码。 */
    public String password() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DbEndpoint)) {
            return false;
        }
        DbEndpoint that = (DbEndpoint) o;
        return Objects.equals(label, that.label)
                && Objects.equals(url, that.url)
                && Objects.equals(user, that.user)
                && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, url, user, password);
    }

    @Override
    public String toString() {
        return label + "(" + url + ")";
    }

    /** 防御性处理：有人误把整个对象打进日志时，凭据不会跟着出去。 */
    public String redactedDsn() {
        return url.replaceAll("(?i)(password=)[^&;]+", "$1***")
                .replaceAll("(?i)(user=)[^&;]+", "$1***");
    }
}