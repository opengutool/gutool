package io.github.opengutool.views.util;

import javax.swing.*;

/**
 * some functions about scroll
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class ScrollUtil {

    public static void smoothPane(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(14);
        scrollPane.getVerticalScrollBar().setDoubleBuffered(true);
        scrollPane.getHorizontalScrollBar().setDoubleBuffered(true);
    }
}
