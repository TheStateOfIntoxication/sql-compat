package com.sqlcompat.verify.db;

/**
 * A JDBC endpoint. Password is carried here but never rendered into a report.
 *
 * @param label    short name shown in reports, e.g. {@code mysql}
 * @param url      JDBC URL
 * @param user     user name
 * @param password password
 */
public record DbEndpoint(String label, String url, String user, String password) {

    @Override
    public String toString() {
        return label + "(" + url + ")";
    }

    /** Defensive: keep credentials out of logs when someone prints the record by accident. */
    public String redactedDsn() {
        return url.replaceAll("(?i)(password=)[^&;]+", "$1***")
                .replaceAll("(?i)(user=)[^&;]+", "$1***");
    }
}
