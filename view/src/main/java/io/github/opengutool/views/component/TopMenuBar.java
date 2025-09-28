package io.github.opengutool.views.component;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.jthemedetecor.OsThemeDetector;
import io.github.opengutool.GutoolApp;
import io.github.opengutool.views.Init;
import io.github.opengutool.views.dialog.AboutDialog;
import io.github.opengutool.views.dialog.KeyMapDialog;
import io.github.opengutool.views.dialog.SettingDialog;
import io.github.opengutool.views.dialog.SyncAndBackupDialog;
import io.github.opengutool.views.dialog.SystemEnvResultDialog;
import io.github.opengutool.views.func.JavaConsoleForm;
import io.github.opengutool.views.util.SystemUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Map;
import java.util.Properties;

/**
 * 顶部菜单栏
 */
public class TopMenuBar extends JMenuBar {
    private static final Log logger = LogFactory.get();

    private static TopMenuBar menuBar;

    public static String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();


    private TopMenuBar() {
    }

    public static TopMenuBar getInstance() {
        if (menuBar == null) {
            menuBar = new TopMenuBar();
        }
        return menuBar;
    }

    public void init() {
        TopMenuBar topMenuBar = getInstance();
        // ---------应用
        JMenu appMenu = new JMenu();
        appMenu.setText("应用");
        // 设置
        JMenuItem settingMenuItem = new JMenuItem();
        settingMenuItem.setText("设置");
        settingMenuItem.addActionListener(e -> settingActionPerformed());
        appMenu.add(settingMenuItem);
        // 同步和备份
        JMenuItem syncAndBackupMenuItem = new JMenuItem();
        syncAndBackupMenuItem.setText("同步和备份");
        syncAndBackupMenuItem.addActionListener(e -> syncAndBackupActionPerformed());
        appMenu.add(syncAndBackupMenuItem);
        // 快捷键
        JMenuItem keyMapMenuItem = new JMenuItem();
        keyMapMenuItem.setText("快捷键");
        keyMapMenuItem.addActionListener(e -> keyMapActionPerformed());
        appMenu.add(keyMapMenuItem);
        // 查看日志
        JMenuItem logMenuItem = new JMenuItem();
        logMenuItem.setText("查看日志");
        logMenuItem.addActionListener(e -> logActionPerformed());
        appMenu.add(logMenuItem);
        // 系统环境变量
        JMenuItem sysEnvMenuItem = new JMenuItem();
        sysEnvMenuItem.setText("系统环境变量");
        sysEnvMenuItem.addActionListener(e -> sysEnvActionPerformed());
        appMenu.add(sysEnvMenuItem);
        // 退出
        JMenuItem exitMenuItem = new JMenuItem();
        exitMenuItem.setText("退出");
        exitMenuItem.addActionListener(e -> exitActionPerformed());
        appMenu.add(exitMenuItem);
        topMenuBar.add(appMenu);

        // ---------外观
        JMenu appearanceMenu = new JMenu();
        appearanceMenu.setText("外观");

        JCheckBoxMenuItem defaultMaxWindowitem = new JCheckBoxMenuItem("最大化窗口");
        defaultMaxWindowitem.setSelected(GutoolApp.config.isDefaultMaxWindow());
        defaultMaxWindowitem.addActionListener(e -> {
            boolean selected = defaultMaxWindowitem.isSelected();
            if (selected) {
                GutoolApp.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            } else {
                GutoolApp.mainFrame.setExtendedState(JFrame.NORMAL);
            }
            GutoolApp.config.setDefaultMaxWindow(selected);
            GutoolApp.config.save();
        });
        appearanceMenu.add(defaultMaxWindowitem);
        topMenuBar.add(appearanceMenu);

        // ---------关于
        JMenu aboutMenu = new JMenu();
        aboutMenu.setText("关于");

        // 关于
        JMenuItem aboutMenuItem = new JMenuItem();
        aboutMenuItem.setText("关于");
        aboutMenuItem.addActionListener(e -> aboutActionPerformed());
        aboutMenu.add(aboutMenuItem);

        topMenuBar.add(aboutMenu);

        final OsThemeDetector detector = OsThemeDetector.getDetector();
        detector.registerListener(isDark -> SwingUtilities.invokeLater(() -> {
            if (isDark) {
                changeTheme("Flat macOS Dark");
            } else {
                changeTheme("Flat macOS Light");
            }
        }));
    }

    private void syncAndBackupActionPerformed() {
        try {
            SyncAndBackupDialog dialog = new SyncAndBackupDialog();

            dialog.pack();
            dialog.setVisible(true);
        } catch (Exception e2) {
            logger.error(e2);
        }
    }

    private void changeTheme(String selectedThemeName) {
        FlatAnimatedLafChange.showSnapshot();

        GutoolApp.config.setTheme(selectedThemeName);
        GutoolApp.config.save();

        Init.initTheme();

        if (FlatLaf.isLafDark()) {
            FlatSVGIcon.ColorFilter.getInstance().setMapper(color -> color.brighter().brighter());
        }

        SwingUtilities.updateComponentTreeUI(GutoolApp.mainFrame);
        // SwingUtilities.updateComponentTreeUI(MainWindow.getInstance().getTabbedPane());

//                FlatLaf.updateUI();

        FlatAnimatedLafChange.hideSnapshotWithAnimation();

        JavaConsoleForm.getInstance().getTextArea().updateTheme();
        JavaConsoleForm.getInstance().getScrollPane().updateTheme();
        JavaConsoleForm.getInstance().getScrollPane().updateUI();

        SwingUtilities.updateComponentTreeUI(GutoolApp.popupMenu);
        GutoolApp.popupMenu.updateUI();
    }

    private void keyMapActionPerformed() {
        try {
            KeyMapDialog dialog = new KeyMapDialog();

            dialog.pack();
            dialog.setVisible(true);
        } catch (Exception e2) {
            logger.error(e2);
        }
    }

    private void aboutActionPerformed() {
        try {
            AboutDialog dialog = new AboutDialog();

            dialog.pack();
            dialog.setVisible(true);
        } catch (Exception e2) {
            logger.error(e2);
        }
    }

    private void sysEnvActionPerformed() {
        try {
            SystemEnvResultDialog dialog = new SystemEnvResultDialog();

            dialog.appendTextArea("------------System.getenv---------------");
            Map<String, String> map = System.getenv();
            for (Map.Entry<String, String> envEntry : map.entrySet()) {
                dialog.appendTextArea(envEntry.getKey() + "=" + envEntry.getValue());
            }

            dialog.appendTextArea("------------System.getProperties---------------");
            Properties properties = System.getProperties();
            for (Map.Entry<Object, Object> objectObjectEntry : properties.entrySet()) {
                dialog.appendTextArea(objectObjectEntry.getKey() + "=" + objectObjectEntry.getValue());
            }

            dialog.pack();
            dialog.setVisible(true);
        } catch (Exception e2) {
            logger.error("查看系统环境变量失败", e2);
        }
    }

    private void logActionPerformed() {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.open(new File(SystemUtil.LOG_DIR));
        } catch (Exception e2) {
            logger.error("查看日志打开失败", e2);
        }
    }

    private void exitActionPerformed() {
        Init.shutdown();
    }

    private void settingActionPerformed() {
        try {
            SettingDialog dialog = new SettingDialog();

            dialog.pack();
            dialog.setVisible(true);
        } catch (Exception e2) {
            logger.error(e2);
        }
    }
}
