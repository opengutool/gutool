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
import io.github.opengutool.domain.func.GutoolFuncTabPanelDefineCron;
import io.github.opengutool.domain.scheduler.GutoolCronTaskScheduler;
import io.github.opengutool.repository.GutoolPoQueryRepository;
import io.github.opengutool.repository.GutoolPoRepository;
import io.github.opengutool.repository.po.GutoolFuncRunHistoryPo;
import io.github.opengutool.views.component.textviewer.JsonRSyntaxTextViewer;
import io.github.opengutool.views.component.textviewer.JsonRTextScrollPane;
import io.github.opengutool.views.util.JTableUtil;
import io.github.opengutool.views.util.UndoUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import raven.toast.Notifications;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
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
    private JTable cronTable;
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
    private GutoolCronTaskScheduler cronTaskScheduler;
    private final String panelType;

    public ScriptRunnerForm(GutoolFuncTabPanel funcTabPanel, JTabbedPane funcTabbedPane) {

        UndoUtil.register(this);

        this.funcTabPanel = funcTabPanel;
        this.panelType = funcTabPanel.getDefine().getType();
        remarkTextArea.setText(funcTabPanel.getRemark());

        // 根据类型初始化定时任务调度器
        if ("cron".equals(this.panelType)) {
            this.cronTaskScheduler = new GutoolCronTaskScheduler(funcTabPanel,
                    msg -> resultArea.append(msg),
                    result -> {
                        this.reloadHistoryListTable(funcTabPanel.getId());
                        this.reloadCronTable();
                    });
        }

        // 根据类型初始化不同的输入界面
        initializeInputPanel();

        // 设置焦点
        if (textTextViewer != null) {
            textTextViewer.setRequestFocusEnabled(true);
            textTextViewer.requestFocusInWindow();
        }


        // 根据类型设置添加按钮功能
        setupAddButtonAction();

        // editButton.setIcon(FontIcon.of(Codicons.EDIT, 18));
        editButton.setText("编辑面板");
        // editButton.setToolTipText("编辑面板信息");
        editButton.addActionListener(e -> {
            final String oldName = funcTabPanel.getName();
            FuncTabPanelDialog dialog = new FuncTabPanelDialog(funcTabPanel, update -> {
                remarkTextArea.setText(funcTabPanel.getRemark());
                this.reloadInputPanel();
                this.reloadMenuPanel();
                this.updateTabbedPane();
            }, update -> {

            });
            dialog.pack();
            dialog.setVisible(true);
        });

        initMenuPanel();
        updateTabbedPane();
        contentSplitPane.setDividerLocation((int) (GutoolApp.mainFrame.getWidth() / 1.5));

        // 启动定时任务
        startCronTasks();

        // 只有非定时任务类型才添加文档监听器
        if (textTextViewer != null && textTextViewer.getDocument() != null) {
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
                    if (textTextViewer != null) {
                        funcTabPanel.setFuncIn(textTextViewer.getText());
                    }
                }
            });
        }
        this.reloadInputPanel();
        this.initHistoryTable(funcTabPanel);
        this.initBtnTable();
    }


    private void reloadInputPanel() {
        inputPanel.removeAll();

        if ("cron".equals(panelType)) {
            // 定时任务类型 - 显示任务列表
            inputPanel.add(this.initCronTable(), BorderLayout.CENTER);
        } else {
            // 默认/按钮/HTTP类型 - 显示文本输入区域
            if (textTextViewerScrollPane != null) {
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
            }
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
        JMenuItem clearAllMenuItem = new JMenuItem("清理全部");
        historyListTablePopupMenu.add(deleteMenuItem);
        historyListTablePopupMenu.add(reloadMenuItem);
        historyListTablePopupMenu.addSeparator();
        historyListTablePopupMenu.add(clearAllMenuItem);
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

        // 清理全部
        clearAllMenuItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this.funcRunPanel,
                    "确定要清理该面板的所有历史记录吗？",
                    "确认清理",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                GutoolPoRepository.deleteAllFuncRunHistoryByTabPanelId(funcTabPanel.getId());
                this.reloadHistoryListTable(funcTabPanel.getId());
                Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "清理成功");
            }
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

    private void initMenuPanel() {
        if ("cron".equals(panelType)) {
            menuPanel.removeAll();
            setupCronMenuPanel();
            menuPanel.revalidate();
            menuPanel.repaint();
        } else {
            reloadMenuPanel();
        }
    }


    private void reloadMenuPanel() {
        if ("default".equals(panelType)) {
            menuPanel.removeAll();
            setupButtonMenuPanel();
            menuPanel.revalidate();
            menuPanel.repaint();
        }
    }

    /**
     * 设置按钮类型的菜单面板
     */
    private void setupButtonMenuPanel() {
        List<GutoolFuncTabPanelDefineButton> defineButtons = funcTabPanel.getButtons();
        defineButtons.sort(Comparator.comparingInt(GutoolFuncTabPanelDefineButton::getOrder));
        int size = defineButtons.size();
        menuPanel.setLayout(new GridLayoutManager(1, size + 3, new Insets(8, 10, 3, 10), -1, -1));

        for (int i = 0; i < defineButtons.size(); i++) {
            GutoolFuncTabPanelDefineButton button = defineButtons.get(i);
            JButton defineButton = new JButton();
            defineButton.setText(button.getText());
            defineButton.setToolTipText(button.getToolTipText());
            defineButton.addActionListener(e -> executeButtonScript(button));

            setupButtonPopupMenu(defineButton, button);
            menuPanel.add(defineButton, new GridConstraints(0, i, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        }
        menuPanel.add(addButton, new GridConstraints(0, size + 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        menuPanel.add(editButton, new GridConstraints(0, size + 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        menuPanel.add(buttonSpacer, new GridConstraints(0, size, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * 设置定时任务类型的菜单面板
     */
    private void setupCronMenuPanel() {
        menuPanel.setLayout(new GridLayoutManager(1, 3, new Insets(8, 10, 3, 10), -1, -1));
        menuPanel.add(buttonSpacer, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        menuPanel.add(addButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        menuPanel.add(editButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * 执行按钮脚本
     */
    private void executeButtonScript(GutoolFuncTabPanelDefineButton button) {
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
                } else if (StrUtil.isNotBlank(resultText)) {
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
    }

    /**
     * 设置按钮的右键菜单
     */
    private void setupButtonPopupMenu(JButton defineButton, GutoolFuncTabPanelDefineButton button) {
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
    }

    /**
     * 根据类型设置添加按钮功能
     */
    private void setupAddButtonAction() {
        if ("cron".equals(panelType)) {
            // 定时任务类型 - 添加按钮用于添加定时任务
            addButton.setText("添加任务");
            addButton.setToolTipText("添加定时任务");
            addButton.addActionListener(e -> {
                FuncTablePanelCronDialog dialog = new FuncTablePanelCronDialog(
                        new GutoolFuncTabPanelDefineCron(),
                        cron -> {
                            funcTabPanel.addCrontab(cron);
                            Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "保存成功");
                            this.reloadCronTable();
                            this.reloadCronTasks(); // 重新加载定时任务
                        }
                );
                dialog.pack();
                dialog.setVisible(true);
            });
        } else {
            // 默认/按钮/HTTP类型 - 添加按钮用于添加功能按钮
            addButton.setText("添加按钮");
            addButton.setToolTipText("添加功能按钮");
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
        }
    }

    /**
     * 根据类型初始化输入面板
     */
    private void initializeInputPanel() {

        if ("cron".equals(panelType)) {
            // 定时任务类型，不显示文本输入区域
            textTextViewer = null;
            textTextViewerScrollPane = null;
        } else {
            // 默认/按钮/HTTP类型，显示文本输入区域
            textTextViewer = new JsonRSyntaxTextViewer();
            textTextViewerScrollPane = new JsonRTextScrollPane(textTextViewer);
            textTextViewer.setText(funcTabPanel.getDefine().getFuncIn());
        }
    }

    /**
     * 初始化定时任务表格
     */
    private JScrollPane initCronTable() {
        final JScrollPane scrollPane1 = new JScrollPane();
        cronTable = new JTable();
        scrollPane1.setViewportView(cronTable);
        String[] cronTableHeaderNames = {"Cron表达式", "描述", "绑定脚本", "下次执行", "排序", "启用状态"};
        Object[][] cronTableEmptyData = new Object[0][cronTableHeaderNames.length];
        DefaultTableModel cronTableModel = new DefaultTableModel(cronTableEmptyData, cronTableHeaderNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 所有单元格都不可编辑
            }
        };
        cronTable.setModel(cronTableModel);

        // 设置表头属性
        cronTable.getTableHeader().setReorderingAllowed(false);
        cronTable.setAutoCreateRowSorter(true);
        cronTable.setRowHeight(25);
        cronTable.setPreferredScrollableViewportSize(new Dimension(500, 200));

        // 确保表格可以处理点击事件
        cronTable.setCellSelectionEnabled(true);
        cronTable.setRowSelectionAllowed(true);
        cronTable.setColumnSelectionAllowed(true);

        // 设置列宽
        TableColumn cronColumn = cronTable.getColumnModel().getColumn(0);
        cronColumn.setPreferredWidth(120);

        TableColumn descColumn = cronTable.getColumnModel().getColumn(1);
        descColumn.setPreferredWidth(150);

        TableColumn scriptColumn = cronTable.getColumnModel().getColumn(2);
        scriptColumn.setPreferredWidth(150);

        TableColumn nextColumn = cronTable.getColumnModel().getColumn(3);
        nextColumn.setPreferredWidth(150);

        TableColumn orderColumn = cronTable.getColumnModel().getColumn(4);
        orderColumn.setPreferredWidth(60);
        orderColumn.setMaxWidth(60);
        orderColumn.setMinWidth(60);

        TableColumn enabledColumn = cronTable.getColumnModel().getColumn(5);
        enabledColumn.setPreferredWidth(100);
        enabledColumn.setMaxWidth(100);
        enabledColumn.setMinWidth(100);

        // 为启用状态列设置按钮渲染器（不需要编辑器，我们通过鼠标监听器直接处理）
        enabledColumn.setCellRenderer(new CronEnabledButtonRenderer());


        // 添加右键菜单
        JPopupMenu cronTablePopupMenu = new JPopupMenu();
        JMenuItem runMenuItem = new JMenuItem("立即运行");
        JMenuItem editMenuItem = new JMenuItem("编辑");
        JMenuItem deleteMenuItem = new JMenuItem("删除");
        JMenuItem reloadMenuItem = new JMenuItem("刷新");

        cronTablePopupMenu.add(runMenuItem);
        cronTablePopupMenu.add(editMenuItem);
        cronTablePopupMenu.add(deleteMenuItem);
        cronTablePopupMenu.addSeparator();
        cronTablePopupMenu.add(reloadMenuItem);

        // 添加鼠标监听器
        cronTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int column = cronTable.columnAtPoint(e.getPoint());
                int row = cronTable.rowAtPoint(e.getPoint());

                // 检查是否点击了启用状态列（第5列，索引为5）
                if (column == 5 && row >= 0 && row < cronTable.getRowCount()) {
                    // 直接从启用状态列获取 cron 对象
                    GutoolFuncTabPanelDefineCron cron = (GutoolFuncTabPanelDefineCron) cronTable.getValueAt(row, 5);
                    if (cron != null) {
                        // 切换启用状态
                        boolean newEnabled = !(cron.getEnabled() != null ? cron.getEnabled() : true);
                        cron.setEnabled(newEnabled);

                        // 通过调用 sortCrontab 触发自动保存
                        funcTabPanel.sortCrontab();

                        Notifications.getInstance().show(Notifications.Type.SUCCESS,
                                Notifications.Location.TOP_CENTER,
                                newEnabled ? "已启用" : "已禁用");

                        reloadCronTasks();
                        reloadCronTable();

                        return;
                    }
                }

                // 处理右键菜单
                if (SwingUtilities.isRightMouseButton(e)) {
                    showPopup(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showPopup(e);
                }
            }

            private void showPopup(MouseEvent e) {
                int row = cronTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    cronTable.setRowSelectionInterval(row, row);
                    // 根据是否有选中项来启用/禁用编辑和删除菜单
                    boolean hasSelection = row >= 0 && row < cronTable.getRowCount();
                    runMenuItem.setEnabled(hasSelection);
                    editMenuItem.setEnabled(hasSelection);
                    deleteMenuItem.setEnabled(hasSelection);
                    cronTablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        runMenuItem.addActionListener(e -> {
            int selectedRow = cronTable.getSelectedRow();
            if (selectedRow != -1 && selectedRow < funcTabPanel.getCrontab().size()) {
                GutoolFuncTabPanelDefineCron cron = funcTabPanel.getCrontab().get(selectedRow);
                executeCronTask(cron);
            }
        });

        editMenuItem.addActionListener(e -> {
            int selectedRow = cronTable.getSelectedRow();
            if (selectedRow != -1 && selectedRow < funcTabPanel.getCrontab().size()) {
                // 根据行号直接获取对应的定时任务对象
                FuncTablePanelCronDialog dialog = new FuncTablePanelCronDialog(
                        funcTabPanel.getCrontab().get(selectedRow),
                        cron -> {
                            funcTabPanel.sortCrontab();
                            Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "保存成功");
                            this.reloadCronTable();
                            this.reloadCronTasks(); // 重新加载定时任务
                        }
                );
                dialog.pack();
                dialog.setVisible(true);
            }
        });

        deleteMenuItem.addActionListener(e -> {
            int selectedRow = cronTable.getSelectedRow();
            if (selectedRow != -1 && selectedRow < funcTabPanel.getCrontab().size()) {
                int confirm = JOptionPane.showConfirmDialog(
                        cronTable,
                        "确定要删除这个定时任务吗？",
                        "确认删除",
                        JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    // 根据行号直接删除对应的定时任务对象
                    funcTabPanel.removeCron(funcTabPanel.getCrontab().get(selectedRow));
                    Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "删除成功");
                    reloadCronTable();
                    this.reloadCronTasks(); // 重新加载定时任务
                }
            }
        });

        reloadMenuItem.addActionListener(e -> reloadCronTable());

        // 初始加载数据
        reloadCronTable();

        return scrollPane1;
    }

    /**
     * 根据类型更新标签页显示
     */
    private void updateTabbedPane() {

        // 查找"按钮列表"标签页的索引
        int buttonListTabIndex = -1;
        for (int i = 0; i < tabbedPane1.getTabCount(); i++) {
            if ("按钮列表".equals(tabbedPane1.getTitleAt(i))) {
                buttonListTabIndex = i;
                break;
            }
        }
        if ("cron".equals(panelType) && buttonListTabIndex != -1) {
            tabbedPane1.removeTabAt(buttonListTabIndex);
        }
    }

    /**
     * 重新加载定时任务表格数据
     */
    public void reloadCronTable() {
        DefaultTableModel model = (DefaultTableModel) cronTable.getModel();
        model.setRowCount(0);

        List<Object[]> funcDataObjectList = GutoolPoQueryRepository.selectFuncAllDataObjectList();

        for (GutoolFuncTabPanelDefineCron cron : funcTabPanel.getCrontab()) {
            Object[] data = new Object[6];
            data[0] = cron.getCronExpression();
            data[1] = cron.getDescription();
            data[2] = funcDataObjectList.stream()
                    .filter(funcData -> ((Long) funcData[0]).equals(cron.getCronTriggerFuncId()))
                    .map(funcData -> ((String) funcData[1]))
                    .findFirst()
                    .orElse("未知脚本");
            data[3] = cron.getNextExecutionTime() != null ?
                    cron.getNextExecutionTime().toString() : "未计算";
            data[4] = cron.getOrder();
            data[5] = cron;
            model.addRow(data);
        }
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

    /**
     * Cron 启用状态按钮渲染器
     */
    private class CronEnabledButtonRenderer extends JButton implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof GutoolFuncTabPanelDefineCron) {
                GutoolFuncTabPanelDefineCron cron = (GutoolFuncTabPanelDefineCron) value;
                boolean enabled = cron.getEnabled() != null ? cron.getEnabled() : true;

                setText(enabled ? "启用" : "禁用");
                setToolTipText("点击切换启用/禁用状态");

                // 根据状态设置按钮颜色
                if (enabled) {
                    setBackground(new Color(76, 175, 80)); // 绿色
                    setForeground(Color.WHITE);
                } else {
                    setBackground(new Color(244, 67, 54)); // 红色
                    setForeground(Color.WHITE);
                }

                setFocusPainted(false);
                setBorderPainted(false);
                setOpaque(true);
                setCursor(new Cursor(Cursor.HAND_CURSOR));

                // 添加悬停效果
                if (hasFocus) {
                    setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                } else if (isSelected) {
                    setBorder(BorderFactory.createLineBorder(table.getSelectionBackground(), 2));
                } else {
                    setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
                }
            } else {
                setText("未知");
                setBackground(Color.LIGHT_GRAY);
                setForeground(Color.BLACK);
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }

            return this;
        }
    }

    /**
     * 编辑 cron 任务
     */
    private void editCronTask(GutoolFuncTabPanelDefineCron cron) {
        FuncTablePanelCronDialog dialog = new FuncTablePanelCronDialog(
                cron,
                updatedCron -> {
                    funcTabPanel.sortCrontab();
                    Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "保存成功");
                    reloadInputPanel(); // 重新加载任务列表
                    reloadCronTable(); // 同时更新右侧表格
                    this.reloadCronTasks(); // 重新加载定时任务
                }
        );
        dialog.pack();
        dialog.setVisible(true);
    }

    /**
     * 删除 cron 任务
     */
    private void deleteCronTask(GutoolFuncTabPanelDefineCron cron) {
        int confirm = JOptionPane.showConfirmDialog(
                this.funcRunPanel,
                "确定要删除这个定时任务吗？",
                "确认删除",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            funcTabPanel.removeCron(cron);
            Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "删除成功");
            reloadInputPanel(); // 重新加载任务列表
            reloadCronTable(); // 同时更新右侧表格
            this.reloadCronTasks(); // 重新加载定时任务
        }
    }

    /**
     * 执行定时任务
     */
    private void executeCronTask(GutoolFuncTabPanelDefineCron cron) {
        if (!cron.getEnabled()) {
            Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "任务已禁用，无法执行");
            return;
        }

        GutoolFunc func = GutoolFuncContainer.getFuncById(cron.getCronTriggerFuncId());
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

        // 运行脚本
        func.initRunner(funcTabPanel,
                "",
                (msg) -> resultArea.append(msg),
                () -> {
                    // 执行完成后的回调
                }).asyncRun(result -> {

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
            if (StrUtil.isNotBlank(resultText)) {
                resultArea.append("result:\n");
                resultArea.append(resultText);
                resultArea.append("\n");
            }

            func.resetRunner();
            // 刷新历史记录表格
            this.reloadHistoryListTable(funcTabPanel.getId());
            // 更新定时任务表格中的执行时间
            this.reloadCronTable();
        });
    }

    /**
     * 启动定时任务
     */
    private void startCronTasks() {
        if ("cron".equals(panelType) && cronTaskScheduler != null) {
            // 启动定时任务
            cronTaskScheduler.startTasks();
        }
    }

    /**
     * 停止定时任务
     */
    private void stopCronTasks() {
        if ("cron".equals(panelType) && cronTaskScheduler != null) {
            cronTaskScheduler.stopTasks();
        }
    }

    /**
     * 重新加载定时任务
     */
    public void reloadCronTasks() {
        stopCronTasks();
        startCronTasks();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        stopCronTasks();
        if (cronTaskScheduler != null) {
            cronTaskScheduler.shutdown();
            cronTaskScheduler = null;
        }
    }

}
