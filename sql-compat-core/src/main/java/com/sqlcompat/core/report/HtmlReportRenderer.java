package com.sqlcompat.core.report;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Report;
import com.sqlcompat.core.model.ScanResult;
import com.sqlcompat.core.model.Severity;

import java.util.List;
import java.util.Map;

/**
 * Self-contained HTML report: inline CSS, no external assets, no JavaScript.
 * Double-click it and it works — which is what you want when you demo the tool.
 */
public final class HtmlReportRenderer implements ReportRenderer {

    @Override
    public String extension() {
        return "html";
    }

    @Override
    public String render(Report report) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <title>sql-compat report</title>
                <style>
                :root{--bg:#0f1115;--fg:#e6e6e6;--muted:#9aa4b2;--line:#252a33;
                      --blocker:#ff5c5c;--major:#ffb020;--minor:#4aa8ff;--info:#8b949e;--card:#161a21}
                *{box-sizing:border-box}
                body{margin:0;padding:32px;background:var(--bg);color:var(--fg);
                     font:14px/1.6 -apple-system,"Segoe UI",Roboto,"Microsoft YaHei",sans-serif}
                h1{font-size:22px;margin:0 0 4px}
                .sub{color:var(--muted);margin-bottom:24px}
                .cards{display:flex;gap:12px;flex-wrap:wrap;margin-bottom:24px}
                .card{background:var(--card);border:1px solid var(--line);border-radius:10px;
                      padding:14px 18px;min-width:132px}
                .card .k{color:var(--muted);font-size:12px;text-transform:uppercase;letter-spacing:.04em}
                .card .v{font-size:24px;font-weight:600;margin-top:2px}
                .stmt{background:var(--card);border:1px solid var(--line);border-radius:10px;
                      padding:16px 18px;margin-bottom:14px}
                .stmt h2{font-size:15px;margin:0 0 2px}
                .stmt .loc{color:var(--muted);font-size:12px;margin-bottom:12px}
                .issue{border-left:3px solid var(--line);padding:8px 0 8px 12px;margin-bottom:10px}
                .issue.BLOCKER{border-color:var(--blocker)}
                .issue.MAJOR{border-color:var(--major)}
                .issue.MINOR{border-color:var(--minor)}
                .issue.INFO{border-color:var(--info)}
                .tag{display:inline-block;font-size:11px;font-weight:700;padding:1px 7px;
                     border-radius:5px;margin-right:6px;color:#0f1115}
                .tag.BLOCKER{background:var(--blocker)}
                .tag.MAJOR{background:var(--major)}
                .tag.MINOR{background:var(--minor)}
                .tag.INFO{background:var(--info)}
                .rid{color:var(--muted);font-family:ui-monospace,Consolas,monospace;font-size:12px}
                .msg{margin:4px 0}
                code{font-family:ui-monospace,Consolas,monospace;font-size:12.5px;
                     background:#1d222b;padding:1px 5px;border-radius:4px}
                .fix{color:#7ee787}
                .clean{color:#7ee787;font-size:15px}
                </style>
                </head>
                <body>
                """);

        sb.append("<h1>sql-compat report</h1>\n");
        sb.append("<div class=\"sub\">").append(escape(report.sourceDialect()))
                .append(" &rarr; ").append(escape(report.targetDialect()))
                .append(" &nbsp;·&nbsp; ").append(escape(report.target()))
                .append(" &nbsp;·&nbsp; ").append(report.elapsed().toMillis()).append(" ms</div>\n");

        Map<Severity, Integer> bySeverity = report.issueCountBySeverity();
        sb.append("<div class=\"cards\">\n");
        card(sb, "statements", String.valueOf(report.statementCount()), null);
        card(sb, "issues", String.valueOf(report.issueCount()), null);
        for (Severity severity : Severity.values()) {
            card(sb, severity.name().toLowerCase(), String.valueOf(bySeverity.get(severity)), severity);
        }
        card(sb, "parse failures", String.valueOf(report.parseErrorCount()), null);
        sb.append("</div>\n");

        if (report.issueCount() == 0) {
            sb.append("<p class=\"clean\">No compatibility issues found.</p>\n");
        }

        List<ScanResult> withIssues = report.results().stream().filter(r -> !r.clean()).toList();
        for (ScanResult result : withIssues) {
            sb.append("<div class=\"stmt\">\n<h2>").append(escape(result.statement().qualifiedId()))
                    .append("</h2>\n<div class=\"loc\">")
                    .append(escape(result.statement().location().toString())).append("</div>\n");
            for (Issue issue : result.issues()) {
                sb.append("<div class=\"issue ").append(issue.severity()).append("\">\n");
                sb.append("<span class=\"tag ").append(issue.severity()).append("\">")
                        .append(issue.severity()).append("</span>");
                sb.append("<span class=\"rid\">").append(escape(issue.ruleId())).append("</span> ");
                sb.append("<strong>").append(escape(issue.ruleName())).append("</strong>\n");
                sb.append("<div class=\"msg\">").append(escape(issue.message())).append("</div>\n");
                if (issue.snippet() != null && !issue.snippet().isBlank()) {
                    sb.append("<div>found: <code>").append(escape(issue.snippet())).append("</code></div>\n");
                }
                if (issue.suggestion() != null && !issue.suggestion().isBlank()) {
                    sb.append("<div class=\"fix\">fix: <code>")
                            .append(escape(issue.suggestion())).append("</code></div>\n");
                }
                sb.append("</div>\n");
            }
            sb.append("</div>\n");
        }
        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    private void card(StringBuilder sb, String key, String value, Severity severity) {
        sb.append("<div class=\"card\"><div class=\"k\">").append(escape(key)).append("</div>");
        sb.append("<div class=\"v\"");
        if (severity != null) {
            sb.append(" style=\"color:var(--").append(severity.name().toLowerCase()).append(")\"");
        }
        sb.append('>').append(value).append("</div></div>\n");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
