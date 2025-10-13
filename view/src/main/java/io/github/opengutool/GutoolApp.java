package io.github.opengutool;

import cn.hutool.core.io.FileUtil;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatDesktop;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.formdev.flatlaf.util.SystemInfo;
import io.github.opengutool.migration.Upgrade;
import io.github.opengutool.repository.GutoolPoRepository;
import io.github.opengutool.views.Init;
import io.github.opengutool.views.dialog.AboutDialog;
import io.github.opengutool.views.dialog.SettingDialog;
import io.github.opengutool.views.form.LoadingForm;
import io.github.opengutool.views.form.MainWindow;
import io.github.opengutool.views.frame.MainFrame;
import io.github.opengutool.views.func.JavaConsoleForm;
import io.github.opengutool.views.util.ConfigUtil;
import io.github.opengutool.views.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import raven.toast.Notifications;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

/**
 * <pre>
 * Main Enter!
 * </pre>
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
@Slf4j
public class GutoolApp {

    public static ConfigUtil config = ConfigUtil.getInstance();

    public static MainFrame mainFrame;

    public static SystemTray tray;

    public static TrayIcon trayIcon;

    public static JPopupMenu popupMenu;

    public static File tempDir = null;

    public static void main(String[] args) {

        if (SystemInfo.isMacOS) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", "Gutool");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Gutool");
            System.setProperty("apple.awt.application.appearance", "system");
            System.setProperty("flatlaf.useRoundedPopupBorder", "true");

            FlatDesktop.setAboutHandler(() -> {
                try {
                    AboutDialog dialog = new AboutDialog();

                    dialog.pack();
                    dialog.setVisible(true);
                } catch (Exception e2) {
                    log.error(ExceptionUtils.getStackTrace(e2));
                }
            });
            FlatDesktop.setPreferencesHandler(() -> {
                try {
                    SettingDialog dialog = new SettingDialog();

                    dialog.pack();
                    dialog.setVisible(true);
                } catch (Exception e2) {
                    log.error(ExceptionUtils.getStackTrace(e2));
                }
            });
            FlatDesktop.setQuitHandler(FlatDesktop.QuitResponse::performQuit);
        }

        FlatLaf.registerCustomDefaultsSource("themes");
        SwingUtilities.invokeLater(FlatJetBrainsMonoFont::install);

        Init.initTheme();

        mainFrame = new MainFrame();
        mainFrame.init();
        addListeners();


        JPanel loadingPanel = new LoadingForm().getLoadingPanel();
        mainFrame.add(loadingPanel);
        mainFrame.pack();
        mainFrame.setVisible(true);

        Notifications.getInstance().setJFrame(mainFrame);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (config.isDefaultMaxWindow() || screenSize.getWidth() <= 1366) {
            // 低分辨率下自动最大化窗口
            mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        Upgrade.startMigration();

        GutoolPoRepository.init();

        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.setContentPane(MainWindow.getInstance().getMainPanel());

        if (SystemUtil.isLinuxOs()) {
            tempDir = new File(SystemUtil.CONFIG_HOME + File.separator + "temp");
        } else {
            tempDir = new File(FileUtil.getTmpDirPath() + "Gutool");
        }
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        FileUtil.clean(tempDir);
        Init.initTray();
        JavaConsoleForm.init();
        MainWindow.getInstance().init();
        mainFrame.remove(loadingPanel);
    }


    public static void addListeners() {
        GutoolApp.mainFrame.addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                saveBeforeExit();
                if (SystemUtil.isWindowsOs()) {
                    GutoolApp.mainFrame.setVisible(false);
                } else if (SystemUtil.isMacOs()) {
                    // 最小化窗口
                    GutoolApp.mainFrame.setExtendedState(Frame.ICONIFIED);
                } else {
                    GutoolApp.mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {
                if (GutoolApp.config.isDefaultMaxWindow()) {
                    // 低分辨率下自动最大化窗口
                    GutoolApp.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
            }
        });

        // 鼠标双击最大化/还原
        GutoolApp.mainFrame.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    if (GutoolApp.mainFrame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                        GutoolApp.mainFrame.setExtendedState(JFrame.NORMAL);
                    } else {
                        GutoolApp.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        if (SystemUtil.isMacOs()) {
            MainWindow.getInstance().getMainPanel().registerKeyboardAction(e -> GutoolApp.mainFrame.setExtendedState(Frame.NORMAL), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        } else {
            MainWindow.getInstance().getMainPanel().registerKeyboardAction(e -> GutoolApp.mainFrame.setExtendedState(Frame.ICONIFIED), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        // Command + W 最小化窗口
        MainWindow.getInstance().getMainPanel().registerKeyboardAction(e -> GutoolApp.mainFrame.setExtendedState(Frame.ICONIFIED), KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

    }

    public static void saveBeforeExit() {
        // App.config.setRecentTabIndex(MainWindow.getInstance().getTabbedPane().getSelectedIndex());
        GutoolApp.config.save();
    }
}
