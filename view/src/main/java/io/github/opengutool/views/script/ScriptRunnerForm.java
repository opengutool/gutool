package io.github.opengutool.views.script;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import io.github.opengutool.GutoolApp;
import io.github.opengutool.domain.func.GutoolFunc;
import io.github.opengutool.domain.func.GutoolFuncContainer;
import io.github.opengutool.domain.func.GutoolFuncTabPanel;
import io.github.opengutool.domain.func.GutoolFuncTabPanelDefineButton;
import io.github.opengutool.repository.GutoolPoQueryRepository;
import io.github.opengutool.repository.GutoolPoRepository;
import io.github.opengutool.repository.po.GutoolFuncRunHistoryPo;
import io.github.opengutool.views.component.textviewer.JsonRSyntaxTextViewer;
import io.github.opengutool.views.component.textviewer.JsonRTextScrollPane;
import io.github.opengutool.views.util.JTableUtil;
import io.github.opengutool.views.util.UndoUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.codicons.Codicons;
import org.kordamp.ikonli.swing.FontIcon;
import raven.toast.Notifications;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
@Slf4j
@Getter
public class ScriptRunnerForm {
    private JPanel inputPanel;
    private JTabbedPane tabbedPane1;
    private JTextArea resultArea;
    private JTable historyTable;
    private JTable btnTable;
    private JPanel funcRunPanel;
    private JSplitPane contentSplitPane;
    private JPanel menuPanel;
    private JTextArea remarkTextArea;

    private JsonRSyntaxTextViewer textTextViewer;
    private JsonRTextScrollPane textTextViewerScrollPane;
    private JsonRSyntaxTextViewer resultTextViewer;
    private JsonRTextScrollPane resultTextViewerScrollPane;

    private final GutoolFuncTabPanel funcTabPanel;
    private final JButton addButton = new JButton();
    private final JButton editButton = new JButton();
    private final Spacer buttonSpacer = new Spacer();

    public ScriptRunnerForm(GutoolFuncTabPanel funcTabPanel, JTabbedPane funcTabbedPane) {

        UndoUtil.register(this);

        this.funcTabPanel = funcTabPanel;
        remarkTextArea.setText(funcTabPanel.getRemark());
        textTextViewer = new JsonRSyntaxTextViewer();
        textTextViewerScrollPane = new JsonRTextScrollPane(textTextViewer);
        textTextViewer.setRequestFocusEnabled(true);
        textTextViewer.requestFocusInWindow();
        textTextViewer.setText(funcTabPanel.getDefine().getFuncIn());


        // addButton.setIcon(FontIcon.of(Codicons.ADD, 18));
        addButton.setText("添加按钮");
        // addButton.setToolTipText("添加按钮");
        addButton.addActionListener(e -> {
            GutoolFuncTabPanelDefineButton addBtn = new GutoolFuncTabPanelDefineButton();
            addBtn.setOrder(funcTabPanel.getButtons().size());
            FuncTabPanelBtnDialog dialog = new FuncTabPanelBtnDialog(addBtn,
                    defineButton -> {
                        funcTabPanel.addButton(defineButton);
                        Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "保存成功");
                        this.reloadMenuPanel();
                        this.reloadBtnListTable();
                    }
            );
            dialog.pack();
            dialog.setVisible(true);
        });

        // editButton.setIcon(FontIcon.of(Codicons.EDIT, 18));
        editButton.setText("编辑面板");
        // editButton.setToolTipText("编辑面板信息");
        editButton.addActionListener(e -> {
            final String oldName = funcTabPanel.getName();
            FunTabPanelDialog dialog = new FunTabPanelDialog(funcTabPanel, update -> {
                int tabIndex = funcTabbedPane.indexOfTab(oldName);
                if (tabIndex != -1) {
                    funcTabbedPane.setTitleAt(tabIndex, funcTabPanel.getName());
                }
                remarkTextArea.setText(funcTabPanel.getRemark());
                this.reloadInputPanel();
            }, update -> {

            });
            dialog.pack();
            dialog.setVisible(true);
        });

        reloadMenuPanel();
        contentSplitPane.setDividerLocation((int) (GutoolApp.mainFrame.getWidth() / 1.5));

        textTextViewer.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                saveFuncIn();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                saveFuncIn();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            private void saveFuncIn() {
                funcTabPanel.setFuncIn(textTextViewer.getText());
            }
        });

        this.reloadInputPanel();
        this.initHistoryTable(funcTabPanel);
        this.initBtnTable();
    }


    private void reloadInputPanel() {
        inputPanel.removeAll();
        if (funcTabPanel.isOutTextEnabled()) {
            if (Objects.isNull(resultTextViewer)) {
                resultTextViewer = new JsonRSyntaxTextViewer();
            }
            if (Objects.isNull(resultTextViewerScrollPane)) {
                resultTextViewerScrollPane = new JsonRTextScrollPane(resultTextViewer);
            }
            JSplitPane resultSplitPane = new JSplitPane(
                    JSplitPane.VERTICAL_SPLIT,
                    textTextViewerScrollPane,
                    resultTextViewerScrollPane
            );
            inputPanel.add(resultSplitPane, BorderLayout.CENTER);
            resultSplitPane.setDividerLocation((int) (GutoolApp.mainFrame.getHeight() / 2.5));
        } else {
            inputPanel.add(textTextViewerScrollPane, BorderLayout.CENTER);
        }
        inputPanel.revalidate();
        inputPanel.repaint();
    }

    private void initHistoryTable(GutoolFuncTabPanel funcTabPanel) {
        String[] historyHeaderNames = {"id", "运行脚本", "运行参数", "返回结果", "cost", "status", "启动时间"};
        Object[][] emptyData = new Object[0][historyHeaderNames.length];
        DefaultTableModel historyModel = new DefaultTableModel(emptyData, historyHeaderNames);
        historyTable.setModel(historyModel);
        TableColumn cost = historyTable.getColumnModel().getColumn(4);
        cost.setMaxWidth(40);
        cost.setMinWidth(40);

        TableColumn status = historyTable.getColumnModel().getColumn(5);
        status.setMaxWidth(60);
        status.setMinWidth(60);
        // 隐藏id列
        JTableUtil.hideColumn(historyTable, 0);

        // 左侧表格增加右键菜单
        JPopupMenu historyListTablePopupMenu = new JPopupMenu();
        JMenuItem deleteMenuItem = new JMenuItem("删除");
        JMenuItem reloadMenuItem = new JMenuItem("刷新");
        historyListTablePopupMenu.add(deleteMenuItem);
        historyListTablePopupMenu.add(reloadMenuItem);
        historyTable.setComponentPopupMenu(null);
        // 添加鼠标监听器
        historyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = historyTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        // 如果右键点到的是某一行，就选中它（可选）
                        historyTable.setRowSelectionInterval(row, row);
                        // 弹出菜单
                        historyListTablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        deleteMenuItem.addActionListener(e -> {
            int historySelectedRow = historyTable.getSelectedRow();
            if (historySelectedRow != -1) {
                // 获取隐藏列的数据（id）
                Object historyIdValue = historyModel.getValueAt(historySelectedRow, 0);
                GutoolPoRepository.deleteFuncRunHistoryById((Long) historyIdValue);
                this.reloadHistoryListTable(funcTabPanel.getId());
            }
        });
        // 刷新
        reloadMenuItem.addActionListener(e -> {
            this.reloadHistoryListTable(funcTabPanel.getId());
        });
        this.reloadHistoryListTable(funcTabPanel.getId());
    }

    private void initBtnTable() {
        String[] btnTableHeaderNames = {"名称", "描述", "排序", "绑定脚本", "输出模式"};
        Object[][] btnTableEmptyData = new Object[0][btnTableHeaderNames.length];
        DefaultTableModel btnTableHistoryModel = new DefaultTableModel(btnTableEmptyData, btnTableHeaderNames);
        btnTable.setModel(btnTableHistoryModel);
        TableColumn order = btnTable.getColumnModel().getColumn(2);
        order.setMaxWidth(40);
        order.setMinWidth(40);
        // 左侧表格增加右键菜单
        JPopupMenu btnTablePopupMenu = new JPopupMenu();
        JMenuItem deleteMenuItem = new JMenuItem("删除");
        JMenuItem reloadMenuItem = new JMenuItem("刷新");
        btnTablePopupMenu.add(deleteMenuItem);
        btnTablePopupMenu.add(reloadMenuItem);
        btnTable.setComponentPopupMenu(null);
        // 添加鼠标监听器
        btnTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = btnTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        // 如果右键点到的是某一行，就选中它（可选）
                        btnTable.setRowSelectionInterval(row, row);
                        // 弹出菜单
                        btnTablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        deleteMenuItem.addActionListener(e -> {
            int btnSelectedRow = btnTable.getSelectedRow();
            if (btnSelectedRow != -1) {
                Object btnTextValue = btnTable.getValueAt(btnSelectedRow, 0);
                funcTabPanel.removeButton((String) btnTextValue);
                Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "保存成功");
                this.reloadMenuPanel();
                this.reloadBtnListTable();
            }
        });
        // 刷新
        reloadMenuItem.addActionListener(e -> {
            this.reloadBtnListTable();
        });
        this.reloadBtnListTable();
    }

    public void reloadBtnListTable() {
        DefaultTableModel model = (DefaultTableModel) btnTable.getModel();
        // 清空原有数据
        model.setRowCount(0);
        // 重新获取数据并填充
        Object[] data = new Object[5];
        List<Object[]> funcDataObjectList = GutoolPoQueryRepository.selectFuncAllDataObjectList();
        for (GutoolFuncTabPanelDefineButton button : funcTabPanel.getButtons()) {
            data[0] = button.getText();
            data[1] = button.getToolTipText();
            data[2] = button.getOrder();
            data[3] = funcDataObjectList.stream().filter(funcDataObject ->
                            ((Long) funcDataObject[0]).equals(button.getActionTriggerFuncId()))
                    .map(funcDataObject -> ((String) funcDataObject[1])).findFirst().orElse("未知脚本请重新绑定");
            data[4] = button.getFunOutMode();
            model.addRow(data);
        }
    }

    private void reloadHistoryListTable(Long tabPanelId) {
        DefaultTableModel model = (DefaultTableModel) historyTable.getModel();
        // 清空原有数据
        model.setRowCount(0);
        // 重新获取数据并填充
        final List<GutoolFuncRunHistoryPo> historyList = GutoolPoQueryRepository.selectFuncRunHistoryListByTabPanelId(tabPanelId);
        Object[] data = new Object[7];
        for (GutoolFuncRunHistoryPo history : historyList) {
            data[0] = history.getId();
            data[1] = history.getFuncMirror();
            data[2] = history.getFuncIn();
            data[3] = history.getFuncOut();
            data[4] = history.getCostTime();
            data[5] = history.getStatus();
            data[6] = history.getCreateTime();
            model.addRow(data);
        }
    }

    private void reloadMenuPanel() {
        menuPanel.removeAll();
        List<GutoolFuncTabPanelDefineButton> defineButtons = funcTabPanel.getButtons();
        defineButtons.sort(Comparator.comparingInt(GutoolFuncTabPanelDefineButton::getOrder));
        int size = defineButtons.size();
        menuPanel.setLayout(new GridLayoutManager(1, size + 3, new Insets(8, 10, 3, 10), -1, -1));
        for (int i = 0; i < defineButtons.size(); i++) {
            GutoolFuncTabPanelDefineButton button = defineButtons.get(i);
            JButton defineButton = new JButton();
            defineButton.setText(button.getText());
            defineButton.setToolTipText(button.getToolTipText());
            defineButton.addActionListener(e -> {
                GutoolFunc func = GutoolFuncContainer.getFuncById(button.getActionTriggerFuncId());
                if (Objects.isNull(func)) {
                    Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "脚本未找到");
                    return;
                }
                if (StrUtil.isBlankOrUndefined(func.getContent())) {
                    Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "脚本内容为空");
                    return;
                }

                // 运行时每次清空
                resultArea.setText("");

                // run func
                func.initRunner(funcTabPanel,
                        textTextViewer.getText(),
                        (msg) -> resultArea.append(msg),
                        () -> {
                            // javaConsoleForm.reloadHistoryListTable((Integer) idObj);
                        }).asyncRun(result -> {
                    if (ObjectUtil.isEmpty(result)) {
                        Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "返回内容为空!");
                        return;
                    }
                    String resultText = "";
                    try {
                        if (result instanceof CharSequence) {
                            resultText = result.toString();
                        } else {
                            resultText = JSONUtil.toJsonPrettyStr(result);
                        }
                    } catch (Exception ex) {
                        resultText = ExceptionUtil.stacktraceToString(ex, 500);
                    }

                    if ("输出结果".equals(button.getFunOutMode())) {
                        if (funcTabPanel.isOutTextEnabled()) {
                            resultTextViewer.setText(resultText);
                        } else {
                            resultArea.append("result:\n");
                            resultArea.append(resultText);
                            resultArea.append("\n");
                        }
                    } else {
                        textTextViewer.setText(resultText);
                    }
                    func.resetRunner();
                    this.reloadHistoryListTable(funcTabPanel.getId());
                });
            });

            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem editItem = new JMenuItem("编辑");
            editItem.addActionListener(e -> {
                FuncTabPanelBtnDialog dialog = new FuncTabPanelBtnDialog(button,
                        editButton -> {
                            funcTabPanel.sortButtons();
                            Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "保存成功");
                            this.reloadMenuPanel();
                            this.reloadBtnListTable();
                        }
                );
                dialog.pack();
                dialog.setVisible(true);
            });
            popupMenu.add(editItem);
            // 给按钮添加鼠标监听器以处理右键
            defineButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    showPopup(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    showPopup(e);
                }

                private void showPopup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
            menuPanel.add(defineButton, new GridConstraints(0, i, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        }
        menuPanel.add(addButton, new GridConstraints(0, size + 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        menuPanel.add(editButton, new GridConstraints(0, size + 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        menuPanel.add(buttonSpacer, new GridConstraints(0, size, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));

        menuPanel.revalidate();
        menuPanel.repaint();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        funcRunPanel = new JPanel();
        funcRunPanel.setLayout(new BorderLayout(0, 0));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        funcRunPanel.add(panel1, BorderLayout.CENTER);
        contentSplitPane = new JSplitPane();
        panel1.add(contentSplitPane, BorderLayout.CENTER);
        inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout(0, 0));
        contentSplitPane.setLeftComponent(inputPanel);
        tabbedPane1 = new JTabbedPane();
        contentSplitPane.setRightComponent(tabbedPane1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        tabbedPane1.addTab("输出打印", panel2);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel2.add(scrollPane1, BorderLayout.CENTER);
        resultArea = new JTextArea();
        scrollPane1.setViewportView(resultArea);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        tabbedPane1.addTab("历史记录", panel3);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel3.add(scrollPane2, BorderLayout.CENTER);
        historyTable = new JTable();
        scrollPane2.setViewportView(historyTable);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        tabbedPane1.addTab("按钮列表", panel4);
        final JScrollPane scrollPane3 = new JScrollPane();
        panel4.add(scrollPane3, BorderLayout.CENTER);
        btnTable = new JTable();
        scrollPane3.setViewportView(btnTable);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("描述", panel5);
        final JScrollPane scrollPane4 = new JScrollPane();
        panel5.add(scrollPane4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        remarkTextArea = new JTextArea();
        scrollPane4.setViewportView(remarkTextArea);
        menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayoutManager(1, 1, new Insets(3, 0, 0, 0), -1, -1));
        funcRunPanel.add(menuPanel, BorderLayout.NORTH);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return funcRunPanel;
    }

}
