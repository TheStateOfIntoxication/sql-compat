package com.sqlcompat.core.report;

import com.sqlcompat.core.model.Report;

/**
 * Turns a {@link Report} into text.
 *
 * <p>Renderers must be side-effect free; the caller decides where to write.
 */
public interface ReportRenderer {

    /** File extension the renderer produces, without the dot, e.g. {@code html}. */
    String extension();

    String render(Report report);
}
