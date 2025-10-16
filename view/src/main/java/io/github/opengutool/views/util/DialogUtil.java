package io.github.opengutool.views.util;

import raven.toast.Notifications;

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
        JDialog dialog = pane.createDialog(parentComponent, title);
        yesButton.addActionListener(e -> {
            pane.setValue(yesButton);
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> {
            pane.setValue(cancelButton);
            dialog.dispose();
        });

        dialog.setVisible(true);
        Object selected = pane.getValue();
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

        JDialog dialog = pane.createDialog(parentComponent, title);

        okButton.addActionListener(e -> {
            pane.setValue(okButton);
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> {
            pane.setValue(cancelButton);
            dialog.dispose();
        });

        dialog.setVisible(true);

        Object selected = pane.getValue();
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
