package io.github.opengutool.views.component.textviewer;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public class JavaRSyntaxTextViewer extends CommonRSyntaxTextViewer {
    public JavaRSyntaxTextViewer() {

        setDoubleBuffered(true);
        setCodeFoldingEnabled(true);

        updateTheme();
    }

    @Override
    public void updateTheme() {
        super.updateTheme();

        setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
    }
}
