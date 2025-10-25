/*
 * Copyright © 2025/9/3 gutool (gutool@163.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.opengutool.views.util;

import com.formdev.flatlaf.util.SystemInfo;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.*;

/**
 * macOS 窗口配置工具类
 * 统一处理 macOS 平台下的窗口样式和布局配置
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/10/25
 */
public final class MacWindowUtil {

    private MacWindowUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 默认的窗口边距设置（用于全屏内容模式）
     */
    public static final Insets DEFAULT_WINDOW_INSETS = new Insets(28, 0, 0, 0);

    /**
     * 对话框的窗口边距设置（用于全屏内容模式）
     */
    public static final Insets DIALOG_WINDOW_INSETS = new Insets(28, 10, 0, 10);

    /**
     * 主面板的边距设置
     */
    public static final Insets MAIN_PANEL_INSETS = new Insets(25, 0, 0, 0);

    /**
     * 检查是否支持 macOS 全屏内容模式
     */
    public static boolean isMacFullscreenContentSupported() {
        return SystemUtil.isMacOs() && SystemInfo.isMacFullWindowContentSupported;
    }

    /**
     * 为窗口配置 macOS 全屏内容样式
     *
     * @param window 要配置的窗口（JFrame 或 JDialog）
     */
    public static void configureMacFullscreenContent(Window window) {
        if (!isMacFullscreenContentSupported()) {
            return;
        }
        JRootPane rootPane = null;
        if (window instanceof JFrame) {
            rootPane = ((JFrame) window).getRootPane();

        } else if (window instanceof JDialog) {
            rootPane = ((JDialog) window).getRootPane();
        }
        if (rootPane != null) {
            rootPane.putClientProperty("apple.awt.fullWindowContent", true);
            rootPane.putClientProperty("apple.awt.transparentTitleBar", true);
            rootPane.putClientProperty("apple.awt.fullscreenable", true);
            rootPane.putClientProperty("apple.awt.windowTitleVisible", false);
        }
    }

    /**
     * 为容器配置 macOS 全屏内容模式的边距
     *
     * @param container 要配置的容器
     * @param insets    边距设置
     */
    public static void configureMacInsets(Container container, Insets insets) {
        if (!isMacFullscreenContentSupported()) {
            return;
        }

        LayoutManager layout = container.getLayout();
        if (layout instanceof GridLayoutManager) {
            ((GridLayoutManager) layout).setMargin(insets);
        }
    }
}
