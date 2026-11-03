package com.sqlcompat.verify.db;

/**
 * 一个 JDBC 连接目标。密码只在内存中携带，不会写进任何报告。
 *
 * @param label    报告中显示的短名，例如 {@code mysql}
 * @param url      JDBC 连接串
 * @param user     用户名
 * @param password 密码
 */
public record DbEndpoint(String label, String url, String user, String password) {

    @Override
    public String toString() {
        return label + "(" + url + ")";
    }

    /** 防御性处理：有人误把整个 record 打进日志时，凭据不会跟着出去。 */
    public String redactedDsn() {
        return url.replaceAll("(?i)(password=)[^&;]+", "$1***")
                .replaceAll("(?i)(user=)[^&;]+", "$1***");
    }
}
