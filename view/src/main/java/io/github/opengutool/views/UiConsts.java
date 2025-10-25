package io.github.opengutool.views;

import javax.swing.*;
import java.awt.*;

/**
 * <pre>
 * UI相关的常量
 * </pre>
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class UiConsts {

    /**
     * 软件名称,版本
     */
    public static final String APP_NAME = "Gutool";
    public static final String APP_VERSION = "v1.1.2";

    /**
     * Logo-64*64
     */
    public static final Image IMAGE_LOGO_64 = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icons/gutool-64.png"));

    /**
     * ICON-64*64
     */
    public static final ImageIcon IMAGE_ICON_64 = new ImageIcon(UiConsts.class.getResource("/icons/gutool-64.png"));

}
