package io.github.opengutool.views;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.jthemedetecor.OsThemeDetector;
import io.github.opengutool.GutoolApp;
import io.github.opengutool.views.func.JavaConsoleForm;
import io.github.opengutool.views.util.MybatisUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * <pre>
 * 初始化类
 * </pre>
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class Init {

    private static final Log logger = LogFactory.get();

    /**
     * 初始化look and feel
     */
    public static void initTheme() {
        try {

            final OsThemeDetector detector = OsThemeDetector.getDetector();
            final boolean isDarkThemeUsed = detector.isDark();
            if (isDarkThemeUsed) {
                FlatMacDarkLaf.setup();
            } else {
                FlatMacLightLaf.setup();
            }

            if (FlatLaf.isLafDark()) {
//                FlatSVGIcon.ColorFilter.getInstance().setMapper(color -> color.brighter().brighter());
            } else {
                FlatSVGIcon.ColorFilter.getInstance().setMapper(color -> color.darker().darker());
//                SwingUtilities.windowForComponent(App.mainFrame).repaint();
            }

            UIManager.put("TitlePane.unifiedBackground", true);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * 初始化系统托盘
     */
    public static void initTray() {

        try {
            if (SystemTray.isSupported() && GutoolApp.tray == null) {
                GutoolApp.tray = SystemTray.getSystemTray();
                GutoolApp.popupMenu = new JPopupMenu();
                JMenuItem exitItem = new JMenuItem("退出");

                exitItem.addActionListener(e -> {
                    shutdown();
                });
                // GutoolApp.popupMenu.addSeparator();
                GutoolApp.popupMenu.add(exitItem);

                GutoolApp.trayIcon = new TrayIcon(UiConsts.IMAGE_LOGO_64, "Gutool");
                GutoolApp.trayIcon.setImageAutoSize(true);

                GutoolApp.trayIcon.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        // 双击（左键）
                        if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                            showMainFrame();
                        }
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        maybeShowPopup(e);
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        maybeShowPopup(e);
                    }
                });

                try {
                    GutoolApp.tray.add(GutoolApp.trayIcon);
                } catch (AWTException e) {
                    e.printStackTrace();
                    logger.error(ExceptionUtils.getStackTrace(e));
                }
            }
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }

    public static void showMainFrame() {
        GutoolApp.mainFrame.setAlwaysOnTop(true);
        GutoolApp.mainFrame.setVisible(true);
        if (GutoolApp.mainFrame.getExtendedState() == Frame.ICONIFIED) {
            GutoolApp.mainFrame.setExtendedState(Frame.NORMAL);
        } else if (GutoolApp.mainFrame.getExtendedState() == 7) {
            GutoolApp.mainFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
        GutoolApp.mainFrame.requestFocus();
        GutoolApp.mainFrame.setAlwaysOnTop(false);
    }

    public static void shutdown() {
        GutoolApp.config.save();
        JavaConsoleForm.getInstance().shutdown();
        MybatisUtil.shutdown();
        GutoolApp.mainFrame.dispose();
        System.exit(0);
    }

    private static void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            Dimension size = GutoolApp.popupMenu.getPreferredSize();
            GutoolApp.popupMenu.setLocation(e.getX() - size.width, e.getY() - size.height);
            GutoolApp.popupMenu.setInvoker(GutoolApp.popupMenu);
            GutoolApp.popupMenu.setVisible(true);
        }
    }

}
