package io.github.opengutool.views.util;


import io.github.opengutool.GutoolApp;

import java.awt.*;

/**
 * <pre>
 * 组件工具
 * </pre>
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class ComponentUtil {
    private static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    private static Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(GutoolApp.mainFrame.getGraphicsConfiguration());

    private static int screenWidth = screenSize.width - screenInsets.left - screenInsets.right;

    private static int screenHeight = screenSize.height - screenInsets.top - screenInsets.bottom;

    /**
     * 设置组件preferSize并定位于屏幕中央
     */
    public static void setPreferSizeAndLocateToCenter(Component component, int preferWidth, int preferHeight) {
        component.setBounds((screenWidth - preferWidth) / 2, (screenHeight - preferHeight) / 2,
                preferWidth, preferHeight);
        Dimension preferSize = new Dimension(preferWidth, preferHeight);
        component.setPreferredSize(preferSize);
    }

    /**
     * 设置组件preferSize并定位于屏幕中央(基于屏幕宽高的百分百)
     */
    public static void setPreferSizeAndLocateToCenter(Component component, double preferWidthPercent, double preferHeightPercent) {
        int preferWidth = (int) (screenWidth * preferWidthPercent);
        int preferHeight = (int) (screenHeight * preferHeightPercent);
        setPreferSizeAndLocateToCenter(component, preferWidth, preferHeight);
    }
}
