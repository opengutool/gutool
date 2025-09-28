package raven.toast.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.codicons.Codicons;
import org.kordamp.ikonli.swing.FontIcon;
import raven.toast.Notifications;
import raven.toast.ToastClientProperties;

import javax.swing.*;
import java.awt.*;

public class ToastNotificationPanel extends JPanel {

    protected JWindow window;
    protected JLabel labelIcon;

    private Notifications.Type type;

    public ToastNotificationPanel() {
        installDefault();
    }

    private void installPropertyStyle() {
        String key = getKey();
        String outlineColor = toTextColor(getDefaultColor());
        String outline = convertsKey(key, "outlineColor", outlineColor);
        putClientProperty(FlatClientProperties.STYLE, "" +
                "background:" + convertsKey(key, "background", "$Panel.background") + ";" +
                "outlineColor:" + outline + ";" +
                "effectColor:" + convertsKey(key, "effectColor", outline));

        labelIcon.putClientProperty(FlatClientProperties.STYLE, "" +
                "foreground:" + convertsKey(getKey(), "foreground", "$TextPane.foreground") + ";");
    }

    private String convertsKey(String key, String value, String defaultValue) {
        return "if($Toast." + key + "." + value + ", $Toast." + key + "." + value + ", if($Toast." + value + ", $Toast." + value + ", " + defaultValue + "))";
    }

    @Override
    public void updateUI() {
        setUI(new ToastPanelUI());
        removeDialogBackground();
    }

    private void removeDialogBackground() {
        if (window != null) {
            Color bg = getBackground();
            window.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 0));
            window.setSize(getPreferredSize());
        }
    }

    private void installDefault() {
        labelIcon = new JLabel();
        labelIcon.setText("Hello!\nToast Notification");
        putClientProperty(ToastClientProperties.TOAST_ICON, labelIcon);
    }

    public void set(Notifications.Type type, String message) {
        this.type = type;
        labelIcon.setIcon(getDefaultIcon());
        labelIcon.setText(message);
        installPropertyStyle();
    }

    public void setDialog(JWindow window) {
        this.window = window;
        removeDialogBackground();
    }

    public Color getDefaultColor() {
        if (type == Notifications.Type.SUCCESS) {
            return Color.decode("#2e7d32");
        } else if (type == Notifications.Type.INFO) {
            return Color.decode("#0288d1");
        } else if (type == Notifications.Type.WARNING) {
            return Color.decode("#ed6c02");
        } else {
            return Color.decode("#d32f2f");
        }
    }

    private String toTextColor(Color color) {
        return "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
    }

    public Icon getDefaultIcon() {
        String key = getKey();
        Icon icon = UIManager.getIcon("Toast." + key + ".icon");
        if (icon != null) {
            return icon;
        }
        return FontIcon.of(this.getKeyIkon(), 28, getDefaultColor());
    }

    public Ikon getKeyIkon() {
        if (type == Notifications.Type.SUCCESS) {
            return Codicons.CHECK;
        } else if (type == Notifications.Type.INFO) {
            return Codicons.INFO;
        } else if (type == Notifications.Type.WARNING) {
            return Codicons.WARNING;
        } else {
            return Codicons.ERROR;
        }
    }


    public String getKey() {
        if (type == Notifications.Type.SUCCESS) {
            return "success";
        } else if (type == Notifications.Type.INFO) {
            return "info";
        } else if (type == Notifications.Type.WARNING) {
            return "warning";
        } else {
            return "error";
        }
    }
}
