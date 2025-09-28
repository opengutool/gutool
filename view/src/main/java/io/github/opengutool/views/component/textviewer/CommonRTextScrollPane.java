package io.github.opengutool.views.component.textviewer;

import com.formdev.flatlaf.FlatLaf;
import io.github.opengutool.GutoolApp;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;

public class CommonRTextScrollPane extends RTextScrollPane {
    // constructor
    public CommonRTextScrollPane(RSyntaxTextArea textArea) {
        super(textArea);

        updateTheme();
    }

    public void updateTheme() {
        setMaximumSize(new Dimension(-1, -1));
        setMinimumSize(new Dimension(-1, -1));

        Color defaultBackground = GutoolApp.mainFrame.getBackground();

        Gutter gutter = getGutter();
        if (FlatLaf.isLafDark()) {
            gutter.setBorderColor(gutter.getLineNumberColor().darker());
        } else {
            gutter.setBorderColor(gutter.getLineNumberColor().brighter());
        }
        gutter.setBackground(defaultBackground);
        // Font font2 = new Font(GutoolApp.config.getFont(), Font.PLAIN, GutoolApp.config.getFontSize());
        // gutter.setLineNumberFont(font2);
        gutter.setBackground(UIManager.getColor("Editor.gutter.background"));
        gutter.setBorderColor(UIManager.getColor("Editor.gutter.borderColor"));
        gutter.setLineNumberColor(UIManager.getColor("Editor.gutter.lineNumberColor"));
    }
}
