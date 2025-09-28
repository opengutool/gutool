package io.github.opengutool;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.codicons.Codicons;
import org.kordamp.ikonli.swing.FontIcon;
import raven.toast.Notifications;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumSet;
import java.util.Objects;

import static java.util.EnumSet.allOf;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class IconTest {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(IconTest::launch);
    }

    private static void launch() {
        SwingUtilities.invokeLater(FlatJetBrainsMonoFont::install);
        FlatMacLightLaf.setup();
        JFrame frame = new JFrame("Codicons");
        DemoTab demoTab = new DemoTab(allOf(Codicons.class));

        JTextField searchField = new JTextField(10);
        JButton searchBtn = new JButton("查找下一个");
        searchBtn.addActionListener(new ActionListener() {
            int preIndex = -1;
            JLabel preJLabel = null;

            @Override
            public void actionPerformed(ActionEvent e) {
                String keyword = searchField.getText();
                if (keyword.isEmpty()) return;
                // 还原
                if (Objects.nonNull(preJLabel)) {
                    preJLabel.setForeground(Color.BLUE);
                    preJLabel.setOpaque(false);  // 必须先设置为不透明才能取消背景色
                    preJLabel.setBackground(null);  // 或者使用父容器的背景色
                }
                Component[] comps = demoTab.getPane().getComponents();
                boolean found = false;
                for (int i = preIndex > 0 ? preIndex + 1 : 0; i < comps.length; i++) {
                    Component c = comps[i];
                    if (c instanceof JLabel) {
                        JLabel label = (JLabel) c;
                        if (label.getText().contains(keyword)) {
                            label.setForeground(Color.RED);
                            label.setOpaque(true);
                            label.setBackground(Color.YELLOW);
                            SwingUtilities.invokeLater(() -> {
                                Rectangle bounds = label.getBounds();
                                bounds.setLocation(bounds.x, bounds.y + 500);
                                demoTab.getPane().scrollRectToVisible(bounds);
                            });
                            found = true;
                            preIndex = i;
                            preJLabel = label;
                            break;
                        }
                    }
                }
                if (!found) {
                    preIndex = -1;
                    preJLabel = null;
                    SwingUtilities.invokeLater(() -> {
                        Rectangle bounds = comps[0].getBounds();
                        bounds.setLocation(bounds.x, bounds.y);
                        demoTab.getPane().scrollRectToVisible(bounds);
                    });
                    Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "已查找到最后一个");
                }
            }
        });
        JPanel top = new JPanel();
        top.add(new JLabel("关键字:"));
        top.add(searchField);
        top.add(searchBtn);

        frame.add(top, BorderLayout.NORTH);
        frame.add(demoTab, BorderLayout.CENTER);

        frame.add(demoTab);


        frame.setSize(new Dimension(1024, 1024));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
        Notifications.getInstance().setJFrame(frame);
    }

    private static class DemoTab extends JScrollPane {

        JPanel pane = new JPanel();

        public JPanel getPane() {
            return pane;
        }

        private DemoTab(EnumSet<? extends Ikon> enumSet) {
            pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
            setViewportView(pane);
            for (Ikon value : enumSet) {
                JLabel label = new JLabel(value.getDescription(), FontIcon.of(value, 48), JLabel.LEFT);
                label.setForeground(Color.BLUE);
                label.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mousePressed(MouseEvent e) {
                        // 单击立即触发
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "已复制：" + value.getDescription());
                            ClipboardUtil.setStr(value.getDescription());
                        }
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        label.setForeground(Color.RED);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        label.setForeground(Color.BLUE);
                    }
                });
                pane.add(label);
            }
        }
    }
}
