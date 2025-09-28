package io.github.opengutool.views.component.textviewer;

import com.formdev.flatlaf.FlatLaf;
import io.github.opengutool.GutoolApp;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class CommonRSyntaxTextViewer extends RSyntaxTextArea {
    public CommonRSyntaxTextViewer() {

        setDoubleBuffered(true);

        updateTheme();

        this.defaultKeyMap();
    }

    private void defaultKeyMap() {
        // Ctrl / Command+X 删除行
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "delline");
        this.getActionMap().put("delline", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteCurrentLine();
            }
        });

        // Ctrl / Command+D 复制行
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "cpline");
        this.getActionMap().put("cpline", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                duplicateCurrentLine();
            }
        });

    }
    /**
     * 按 IntelliJ 风格：删除当前行或与选区相交的整行（修正版）
     */
    private void deleteCurrentLine() {
        try {
            Document doc = getDocument();
            int docLen = doc.getLength();
            if (docLen == 0) return;

            // 先规范选区
            int selA = getSelectionStart();
            int selB = getSelectionEnd();
            int selStart = Math.min(selA, selB);
            int selEnd = Math.max(selA, selB);

            int startLine, endLine;
            if (selStart != selEnd) {
                startLine = getLineOfOffset(selStart);
                int endOffsetForLine = Math.max(0, selEnd - 1);
                endLine = getLineOfOffset(endOffsetForLine);
            } else {
                int caret = getCaretPosition();
                startLine = endLine = getLineOfOffset(caret);
            }

            if (endLine < startLine) {
                int t = startLine; startLine = endLine; endLine = t;
            }

            int lineStartOffset = getLineStartOffset(startLine);
            int lastLineIndex = getLineCount() - 1;
            int deleteEndExclusive;
            if (endLine < lastLineIndex) {
                deleteEndExclusive = getLineStartOffset(endLine + 1); // safe because checked
            } else {
                deleteEndExclusive = doc.getLength();
            }

            if (deleteEndExclusive > lineStartOffset) {
                doc.remove(lineStartOffset, deleteEndExclusive - lineStartOffset);
            }

            // 光标放到删除位置（若文档为空则为0）
            int newPos = Math.min(lineStartOffset, doc.getLength());
            setCaretPosition(newPos);

        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 按 IntelliJ 风格：复制当前行或复制选区（整行或内联）——修正版
     */
    private void duplicateCurrentLine() {
        try {
            Document doc = getDocument();
            int docLen = doc.getLength();
            int lastLineIndex = Math.max(0, getLineCount() - 1);

            // 规范选区
            int selA = getSelectionStart();
            int selB = getSelectionEnd();
            int selStart = Math.min(selA, selB);
            int selEnd = Math.max(selA, selB);

            if (selStart != selEnd) {
                // 有选区：判断是否为整行选区
                int startLine = getLineOfOffset(selStart);
                int endLine = getLineOfOffset(Math.max(0, selEnd - 1));

                int startLineOffset = getLineStartOffset(startLine);
                int afterEndLineOffset = (endLine < lastLineIndex) ? getLineStartOffset(endLine + 1) : doc.getLength();

                boolean isWholeLines = (selStart == startLineOffset) && (selEnd == afterEndLineOffset);

                if (isWholeLines) {
                    // 整行复制（把整块插到选中块下方）
                    String textToCopy = getText(startLineOffset, afterEndLineOffset - startLineOffset);
                    int insertPos = afterEndLineOffset;
                    if (endLine < lastLineIndex) {
                        doc.insertString(insertPos, textToCopy, null);
                        setSelectionStart(insertPos);
                        setSelectionEnd(insertPos + textToCopy.length());
                        setCaretPosition(insertPos + textToCopy.length());
                    } else {
                        // 最后一行：在末尾插入前置换行
                        String toInsert = "\n" + textToCopy;
                        doc.insertString(insertPos, toInsert, null);
                        setSelectionStart(insertPos + 1);
                        setSelectionEnd(insertPos + 1 + textToCopy.length());
                        setCaretPosition(insertPos + 1 + textToCopy.length());
                    }
                } else {
                    // 内联复制：把选中文本复制粘贴到选区后面，并选中新插入的那份
                    String selectedText = getSelectedText();
                    if (selectedText != null && selectedText.length() > 0) {
                        int insertPos = selEnd;
                        doc.insertString(insertPos, selectedText, null);
                        setSelectionStart(insertPos);
                        setSelectionEnd(insertPos + selectedText.length());
                        setCaretPosition(insertPos + selectedText.length());
                    }
                }
            } else {
                // 无选区：复制当前行，光标移动到复制后的同列位置
                int caret = getCaretPosition();
                int currentLine = getLineOfOffset(caret);
                int lineStart = getLineStartOffset(currentLine);
                int lineEnd = getLineEndOffset(currentLine); // 行尾（不含换行符）
                int colInLine = caret - lineStart;
                int origLineLen = lineEnd - lineStart; // 不含换行符

                if (currentLine < lastLineIndex) {
                    int nextLineStart = getLineStartOffset(currentLine + 1);
                    String lineWithNewline = getText(lineStart, nextLineStart - lineStart); // 包含换行符
                    int insertPos = nextLineStart;
                    doc.insertString(insertPos, lineWithNewline, null);
                    // 新行起点是 insertPos，映射到相同列
                    int newCaret = insertPos + Math.min(colInLine, origLineLen);
                    setCaretPosition(Math.min(newCaret, doc.getLength()));
                } else {
                    // 最后一行：在文档末尾插入 "\n" + 行内容
                    String lastLineText = getText(lineStart, lineEnd - lineStart);
                    int insertPos = doc.getLength();
                    String toInsert = "\n" + lastLineText;
                    doc.insertString(insertPos, toInsert, null);
                    int newCaret = insertPos + 1 + Math.min(colInLine, lastLineText.length());
                    setCaretPosition(Math.min(newCaret, doc.getLength()));
                }
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }


    public void updateTheme() {
        try {
            Theme theme;
            if (FlatLaf.isLafDark()) {
                theme = Theme.load(GutoolApp.class.getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
            } else {
                theme = Theme.load(GutoolApp.class.getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/idea.xml"));
            }
            theme.apply(this);
        } catch (IOException ioe) { // Never happens
            ioe.printStackTrace();
        }

        setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        setCodeFoldingEnabled(true);
//        setCurrentLineHighlightColor(new Color(52, 52, 52));
//        setUseSelectedTextColor(true);
//        setSelectedTextColor(new Color(50, 50, 50));

        // 初始化背景色
//        Style.blackTextArea(this);

        // 初始化字体
        // 字体字号+1
        setFont(getFont().deriveFont((float) getFont().getSize() + 1));

        setHyperlinksEnabled(false);

        setBackground(UIManager.getColor("Editor.background"));
        setCaretColor(UIManager.getColor("Editor.caretColor"));
        setSelectionColor(UIManager.getColor("Editor.selectionBackground"));
        setCurrentLineHighlightColor(UIManager.getColor("Editor.currentLineHighlight"));
        setMarkAllHighlightColor(UIManager.getColor("Editor.markAllHighlightColor"));
        setMarkOccurrencesColor(UIManager.getColor("Editor.markOccurrencesColor"));
        setMatchedBracketBGColor(UIManager.getColor("Editor.matchedBracketBackground"));
        setMatchedBracketBorderColor(UIManager.getColor("Editor.matchedBracketBorderColor"));
        setPaintMatchedBracketPair(true);
        setAnimateBracketMatching(false);
    }
}
