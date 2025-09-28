package io.github.opengutool;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import cn.hutool.core.util.StrUtil;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.codicons.Codicons;
import org.kordamp.ikonli.swing.FontIcon;
import raven.toast.Notifications;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.EnumSet.allOf;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class IconFilterTest {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(IconFilterTest::launch);
    }

    private static void launch() {
        SwingUtilities.invokeLater(FlatJetBrainsMonoFont::install);
        FlatMacLightLaf.setup();
        JFrame frame = new JFrame("Codicons");
        DemoTab demoTab = new DemoTab(allOf(Codicons.class));

        JTextField searchField = new JTextField(10);
        searchField.addActionListener(e -> {
            demoTab.filterReloadIcon(searchField.getText());
        });
        JPanel top = new JPanel();
        top.add(new JLabel("查找:"));
        top.add(searchField);

        frame.add(top, BorderLayout.NORTH);
        frame.add(demoTab, BorderLayout.CENTER);

        frame.add(demoTab);
        frame.setSize(new Dimension(1050, 500));
        frame.setResizable(false);

        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
        Notifications.getInstance().setJFrame(frame);
    }

    private static class DemoTab extends JScrollPane {

        JPanel pane = new JPanel();

        final java.util.List<JLabel> labels = new ArrayList<>();

        private DemoTab(EnumSet<? extends Ikon> enumSet) {
            // pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
            pane.setLayout(new GridLayout(0, 3, 10, 10));
            setViewportView(pane);
            this.labels.addAll(enumSet.stream().filter(Objects::nonNull).map(this::buildIKonLabel).collect(Collectors.toList()));
            labels.forEach(label -> pane.add(label));
        }

        private JLabel buildIKonLabel(Ikon value) {
            JLabel label = new JLabel(value.getDescription(), FontIcon.of(value, 48), JLabel.LEFT);
            label.setForeground(Color.BLUE);
            label.addMouseListener(new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    // 单击立即触发
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        label.setForeground(Color.RED);
                        Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "已复制：" + value.getDescription());
                        ClipboardUtil.setStr(value.getDescription());
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                }
            });
            return label;
        }

        public void filterReloadIcon(String keyword) {
            pane.removeAll();
            // 红色的放前面
            labels.stream()
                    .filter(label -> label.getForeground() == Color.RED)
                    .filter(label -> StrUtil.isBlank(keyword) || label.getText().contains(keyword))
                    .forEach(label -> pane.add(label));
            labels.stream()
                    .filter(label -> label.getForeground() != Color.RED)
                    .filter(label -> StrUtil.isBlank(keyword) || label.getText().contains(keyword))
                    .forEach(label -> pane.add(label));
            pane.revalidate();
            pane.repaint();
        }
    }
}
