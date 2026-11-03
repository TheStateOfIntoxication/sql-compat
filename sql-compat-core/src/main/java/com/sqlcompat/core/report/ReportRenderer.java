package com.sqlcompat.core.report;

import com.sqlcompat.core.model.Report;

/**
 * 把 {@link Report} 变成文本。
 *
 * <p>实现必须无副作用；写到哪里由调用方决定。
 */
public interface ReportRenderer {

    /** 渲染产物的文件扩展名，不含点，例如 {@code html}。 */
    String extension();

    String render(Report report);
}
