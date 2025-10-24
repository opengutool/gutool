package io.github.opengutool.views.util;

import io.github.opengutool.views.UiConsts;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/10/16
 */
public class DialogUtil {

    public static void showDialog(Component parentComponent,
                                  Object message, String title,
                                  Runnable successCallback, Runnable cancelCallback) {
        JButton yesButton = new JButton("确定");
        JButton cancelButton = new JButton("取消");
        Object[] options = {yesButton, cancelButton};
        JOptionPane pane = new JOptionPane(
                message,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                options,
                yesButton
        );

        JDialog dialog = new JDialog((Frame)null, title, true);
        dialog.setContentPane(pane);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setIconImage(UiConsts.IMAGE_LOGO_64);

        // 添加监听器处理按钮事件
        pane.addPropertyChangeListener(e -> {
            if (e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) {
                dialog.setVisible(false);
            }
        });

        // 处理点击X按钮关闭对话框
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                pane.setValue(cancelButton);
                dialog.setVisible(false);
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(parentComponent);

        yesButton.addActionListener(e -> {
            pane.setValue(yesButton);
        });
        cancelButton.addActionListener(e -> {
            pane.setValue(cancelButton);
        });

        dialog.setVisible(true);
        Object selected = pane.getValue();
        dialog.dispose();

        if (selected == yesButton) {
            if (Objects.nonNull(successCallback)) {
                successCallback.run();
            }
        } else {
            if (Objects.nonNull(cancelCallback)) {
                cancelCallback.run();
            }
        }
    }

    public static void showInputDialog(Component parentComponent,
                                       String title,
                                       String message,
                                       String defaultValue,
                                       Consumer<String> successCallback,
                                       Runnable cancelCallback) {
        JTextField textField = new JTextField(defaultValue != null ? defaultValue : "");
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel(message), BorderLayout.NORTH);
        panel.add(textField, BorderLayout.CENTER);

        JButton okButton = new JButton("确定");
        JButton cancelButton = new JButton("取消");
        Object[] options = {okButton, cancelButton};

        JOptionPane pane = new JOptionPane(
                panel,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                options,
                okButton
        );

        JDialog dialog = new JDialog((Frame)null, title, true);
        dialog.setContentPane(pane);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setIconImage(UiConsts.IMAGE_LOGO_64);

        // 添加监听器处理按钮事件
        pane.addPropertyChangeListener(e -> {
            if (e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) {
                dialog.setVisible(false);
            }
        });

        // 处理点击X按钮关闭对话框
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                pane.setValue(cancelButton);
                dialog.setVisible(false);
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(parentComponent);

        okButton.addActionListener(e -> {
            pane.setValue(okButton);
        });
        cancelButton.addActionListener(e -> {
            pane.setValue(cancelButton);
        });

        dialog.setVisible(true);

        Object selected = pane.getValue();
        dialog.dispose();

        if (selected == okButton) {
            String input = textField.getText();
            if (Objects.nonNull(successCallback)) {
                successCallback.accept(input);
            }
        } else {
            if (Objects.nonNull(cancelCallback)) {
                cancelCallback.run();
            }
        }
    }
}
