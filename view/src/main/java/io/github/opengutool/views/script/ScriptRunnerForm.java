package io.github.opengutool.views.script;

import cn.hutool.core.date.DateUtil;
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
import io.github.opengutool.domain.func.GutoolFuncTabPanelDefineHttp;
import io.github.opengutool.domain.http.GutoolHttpServer;
import io.github.opengutool.domain.scheduler.GutoolCronTaskScheduler;
import io.github.opengutool.repository.GutoolPoQueryRepository;
import io.github.opengutool.repository.GutoolPoRepository;
import io.github.opengutool.repository.po.GutoolFuncRunHistoryPo;
import io.github.opengutool.views.component.textviewer.JsonRSyntaxTextViewer;
import io.github.opengutool.views.component.textviewer.JsonRTextScrollPane;
import io.github.opengutool.views.util.DialogUtil;
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
    private JTable httpConfigTable;
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
    private GutoolHttpServer httpServer;
    private JButton serverToggleButton; // HTTP服务器启动/停止按钮
    private JLabel serverInfoLabel; // 服务器信息标签
    private JButton cronToggleButton; // 定时任务启动/停止按钮
    private JLabel cronInfoLabel; // 定时任务信息标签
    private final String panelType;

    public ScriptRunnerForm(GutoolFuncTabPanel funcTabPanel, JTabbedPane funcTabbedPane) {

        $$$setupUI$$$();
        UndoUtil.register(this);

        this.funcTabPanel = funcTabPanel;
        this.panelType = funcTabPanel.getDefine().getType();
        remarkTextArea.setText(funcTabPanel.getRemark());

        if ("cron".equals(this.panelType)) {
            this.cronTaskScheduler = new GutoolCronTaskScheduler(funcTabPanel, msg -> resultArea.append(msg), result -> {
                this.reloadHistoryListTable(funcTabPanel.getId());
                this.reloadCronTable();
            });
        } else if ("http".equals(this.panelType)) {
            this.httpServer = new GutoolHttpServer(funcTabPanel, msg -> resultArea.append(msg), result -> {
                this.reloadHistoryListTable(funcTabPanel.getId());
                this.reloadHttpConfigTable();
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
                this.reloadHttpServer();
            }, update -> {

            });
            dialog.pack();
            dialog.setVisible(true);
        });

        initMenuPanel();
        updateTabbedPane();
        contentSplitPane.setDividerLocation((int) (GutoolApp.mainFrame.getWidth() / 1.5));


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

        // 启动定时任务
        startCronTasks();

        // 启动HTTP服务
        startHttpServer();

        this.initHistoryTable(funcTabPanel);
        this.initBtnTable();

        // 初始化 cron 状态显示
        if ("cron".equals(panelType)) {
            updateCronToggleButton();
            reloadCronInfoLabel();
        }
    }


    private void reloadInputPanel() {
        inputPanel.removeAll();

        if ("cron".equals(panelType)) {
            // 定时任务类型 - 显示任务列表
            inputPanel.add(this.initCronTable(), BorderLayout.CENTER);
        } else if ("http".equals(panelType)) {
            // HTTP服务类型 - 显示HTTP配置列表
            inputPanel.add(this.initHttpConfigTable(), BorderLayout.CENTER);
        } else {
            // 默认/按钮/HTTP类型 - 显示文本输入区域
            if (textTextViewerScrollPane != null) {
                if (funcTabPanel.getOutTextEnabled()) {
                    if (Objects.isNull(resultTextViewer)) {
                        resultTextViewer = new JsonRSyntaxTextViewer();
                    }
                    if (Objects.isNull(resultTextViewerScrollPane)) {
                        resultTextViewerScrollPane = new JsonRTextScrollPane(resultTextViewer);
                    }
                    JSplitPane resultSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, textTextViewerScrollPane, resultTextViewerScrollPane);
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
            DialogUtil.showDialog(this.funcRunPanel, "确定要清理该面板的所有历史记录吗？", "清理所有历史记录", () -> {
                GutoolPoRepository.deleteAllFuncRunHistoryByTabPanelId(funcTabPanel.getId());
                this.reloadHistoryListTable(funcTabPanel.getId());
                Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "清理成功");
            }, () -> {

            });
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
        if ("default".equals(panelType)) {
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
                data[3] = funcDataObjectList.stream().filter(funcDataObject -> ((Long) funcDataObject[0]).equals(button.getActionTriggerFuncId())).map(funcDataObject -> ((String) funcDataObject[1])).findFirst().orElse("未知脚本请重新绑定");
                data[4] = button.getFunOutMode();
                model.addRow(data);
            }
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
        if ("cron".equals(panelType) || "http".equals(panelType)) {
            menuPanel.removeAll();
            setupCronAndHttpMenuPanel();
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
     * 更新服务器信息标签
     */
    private void reloadServerInfoLabel() {
        if (httpServer != null && funcTabPanel != null) {
            String status = httpServer.getServerStatus();
            // 根据状态设置不同的颜色
            Color statusColor;
            switch (status) {
                case "运行中":
                    statusColor = new Color(76, 175, 80); // 绿色
                    break;
                case "已停止":
                case "失败":
                    statusColor = new Color(244, 67, 54); // 红色
                    break;
                case "启动中":
                case "停止中":
                    statusColor = new Color(255, 152, 0); // 橙色
                    break;
                default:
                    statusColor = new Color(158, 158, 158); // 灰色
                    break;
            }

            serverInfoLabel.setText(String.format("HTTP服务器状态: %s | 监听端口: %s | 接口数量: %d", status, httpServer.getPort(), funcTabPanel.getHttpConfigs().stream().filter(GutoolFuncTabPanelDefineHttp::getEnabled).count()));
            serverInfoLabel.setForeground(statusColor);
        } else {
            serverInfoLabel.setText("HTTP服务器未初始化");
            serverInfoLabel.setForeground(Color.GRAY);
        }
        reloadHttpConfigTable();
    }

    /**
     * 更新定时任务信息标签
     */
    private void reloadCronInfoLabel() {
        if (cronTaskScheduler != null && funcTabPanel != null) {
            String status = cronTaskScheduler.getSchedulerStatus();
            // 根据状态设置不同的颜色
            Color statusColor;
            switch (status) {
                case "运行中":
                    statusColor = new Color(76, 175, 80); // 绿色
                    break;
                case "已停止":
                case "失败":
                    statusColor = new Color(244, 67, 54); // 红色
                    break;
                case "启动中":
                case "停止中":
                    statusColor = new Color(255, 152, 0); // 橙色
                    break;
                default:
                    statusColor = new Color(158, 158, 158); // 灰色
                    break;
            }

            int enabledTaskCount = funcTabPanel.getCrontab().stream()
                    .filter(cron -> cron.getEnabled() != null ? cron.getEnabled() : true)
                    .toList()
                    .size();
            cronInfoLabel.setText(String.format("定时任务状态: %s | 任务总数: %d | 启用任务: %d", status, funcTabPanel.getCrontab().size(), enabledTaskCount));
            cronInfoLabel.setForeground(statusColor);
        } else {
            cronInfoLabel.setText("定时任务调度器未初始化");
            cronInfoLabel.setForeground(Color.GRAY);
        }
        reloadCronTable();
    }

    /**
     * 创建服务器启动/停止按钮
     */
    private JButton createServerToggleButton() {
        JButton serverToggleButton = new JButton();

        serverToggleButton.addActionListener(e -> {
            if (httpServer != null) {
                if (httpServer.isRunning()) {
                    funcTabPanel.setAutoEnabled(false);
                    stopHttpServer();
                } else {
                    funcTabPanel.setAutoEnabled(true);
                    startHttpServer();
                }
            }
        });

        return serverToggleButton;
    }

    /**
     * 创建定时任务启动/停止按钮
     */
    private JButton createCronToggleButton() {
        JButton cronToggleButton = new JButton();

        cronToggleButton.addActionListener(e -> {
            if (cronTaskScheduler != null) {
                if (cronTaskScheduler.isRunning()) {
                    funcTabPanel.setAutoEnabled(false);
                    stopCronTasks();
                } else {
                    funcTabPanel.setAutoEnabled(true);
                    startCronTasks();
                }
            }
        });

        return cronToggleButton;
    }

    /**
     * 更新服务器切换按钮状态
     */
    private void updateServerToggleButton() {
        if (Objects.isNull(serverToggleButton)) {
            return;
        }
        if (httpServer != null && httpServer.isRunning()) {
            serverToggleButton.setText("停止服务");
            serverToggleButton.setToolTipText("停止HTTP服务器");
            serverToggleButton.setBackground(new Color(244, 67, 54)); // 红色
            serverToggleButton.setForeground(Color.WHITE);
        } else {
            serverToggleButton.setText("启动服务");
            serverToggleButton.setToolTipText("启动HTTP服务器");
            serverToggleButton.setBackground(new Color(76, 175, 80)); // 绿色
            serverToggleButton.setForeground(Color.WHITE);
        }
        serverToggleButton.setFocusPainted(false);
        serverToggleButton.setBorderPainted(false);
        serverToggleButton.setOpaque(true);
    }

    /**
     * 更新定时任务切换按钮状态
     */
    private void updateCronToggleButton() {
        if (Objects.isNull(cronToggleButton)) {
            return;
        }
        if (cronTaskScheduler != null && cronTaskScheduler.isRunning()) {
            cronToggleButton.setText("停止任务");
            cronToggleButton.setToolTipText("停止定时任务");
            cronToggleButton.setBackground(new Color(244, 67, 54)); // 红色
            cronToggleButton.setForeground(Color.WHITE);
        } else {
            cronToggleButton.setText("启动任务");
            cronToggleButton.setToolTipText("启动定时任务");
            cronToggleButton.setBackground(new Color(76, 175, 80)); // 绿色
            cronToggleButton.setForeground(Color.WHITE);
        }
        cronToggleButton.setFocusPainted(false);
        cronToggleButton.setBorderPainted(false);
        cronToggleButton.setOpaque(true);
    }

    /**
     * 设置定时任务类型的菜单面板
     */
    private void setupCronAndHttpMenuPanel() {
        if ("http".equals(panelType)) {
            // HTTP类型 - 5个按钮：状态、空白、启动/停止、添加接口、编辑面板
            menuPanel.setLayout(new GridLayoutManager(1, 5, new Insets(8, 10, 3, 10), -1, -1));
            // 状态
            serverInfoLabel = new JLabel();
            serverInfoLabel.setFont(serverInfoLabel.getFont().deriveFont(Font.BOLD, 12f));
            menuPanel.add(serverInfoLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
            menuPanel.add(buttonSpacer, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
            serverToggleButton = createServerToggleButton();
            menuPanel.add(serverToggleButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
            menuPanel.add(addButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
            menuPanel.add(editButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        } else if ("cron".equals(panelType)) {
            // Cron类型 - 5个按钮：状态、空白、启动/停止、添加任务、编辑面板
            menuPanel.setLayout(new GridLayoutManager(1, 5, new Insets(8, 10, 3, 10), -1, -1));
            // 状态
            cronInfoLabel = new JLabel();
            cronInfoLabel.setFont(cronInfoLabel.getFont().deriveFont(Font.BOLD, 12f));
            menuPanel.add(cronInfoLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
            menuPanel.add(buttonSpacer, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
            cronToggleButton = createCronToggleButton();
            menuPanel.add(cronToggleButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
            menuPanel.add(addButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
            menuPanel.add(editButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        } else {
            // 默认类型 - 3个按钮：空白、添加、编辑面板
            menuPanel.setLayout(new GridLayoutManager(1, 3, new Insets(8, 10, 3, 10), -1, -1));
            menuPanel.add(buttonSpacer, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
            menuPanel.add(addButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
            menuPanel.add(editButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        }
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
        func.initRunner(funcTabPanel, textTextViewer.getText(), (msg) -> resultArea.append(msg), () -> {
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
                if (funcTabPanel.getOutTextEnabled()) {
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
            FuncTabPanelBtnDialog dialog = new FuncTabPanelBtnDialog(button, editButton -> {
                funcTabPanel.sortButtons();
                Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "保存成功");
                this.reloadMenuPanel();
                this.reloadBtnListTable();
            });
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
                FuncTablePanelCronDialog dialog = new FuncTablePanelCronDialog(new GutoolFuncTabPanelDefineCron(), cron -> {
                    funcTabPanel.addCrontab(cron);
                    Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "保存成功");
                    this.reloadCronTable();
                    this.reloadCronTasks(); // 重新加载定时任务
                });
                dialog.pack();
                dialog.setVisible(true);
            });
        } else if ("http".equals(panelType)) {
            // HTTP类型添加接口
            addButton.setText("添加接口");
            addButton.setToolTipText("添加定时任务");
            addButton.addActionListener(e -> {
                FuncTablePanelHttpDialog dialog = new FuncTablePanelHttpDialog(new GutoolFuncTabPanelDefineHttp(), cron -> {
                    funcTabPanel.addHttpConfig(cron);
                    Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "保存成功");
                    this.reloadHttpConfigTable();
                    try {
                        httpServer.restart();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
                dialog.pack();
                dialog.setVisible(true);
            });
        } else {
            // 默认/按钮 - 添加按钮用于添加功能按钮
            addButton.setText("添加按钮");
            addButton.setToolTipText("添加功能按钮");
            addButton.addActionListener(e -> {
                GutoolFuncTabPanelDefineButton addBtn = new GutoolFuncTabPanelDefineButton();
                addBtn.setOrder(funcTabPanel.getButtons().size());
                FuncTabPanelBtnDialog dialog = new FuncTabPanelBtnDialog(addBtn, defineButton -> {
                    funcTabPanel.addButton(defineButton);
                    Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "保存成功");
                    this.reloadMenuPanel();
                    this.reloadBtnListTable();
                });
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
        JMenuItem editMenuItem = new JMenuItem("编辑");
        JMenuItem deleteMenuItem = new JMenuItem("删除");

        cronTablePopupMenu.add(editMenuItem);
        cronTablePopupMenu.add(deleteMenuItem);

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

                        Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, newEnabled ? "已启用" : "已禁用");

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
                    editMenuItem.setEnabled(hasSelection);
                    deleteMenuItem.setEnabled(hasSelection);
                    cronTablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });


        editMenuItem.addActionListener(e -> {
            int selectedRow = cronTable.getSelectedRow();
            if (selectedRow != -1 && selectedRow < funcTabPanel.getCrontab().size()) {
                // 根据行号直接获取对应的定时任务对象
                FuncTablePanelCronDialog dialog = new FuncTablePanelCronDialog(funcTabPanel.getCrontab().get(selectedRow), cron -> {
                    funcTabPanel.sortCrontab();
                    Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "保存成功");
                    this.reloadCronTable();
                    this.reloadCronTasks(); // 重新加载定时任务
                });
                dialog.pack();
                dialog.setVisible(true);
            }
        });

        deleteMenuItem.addActionListener(e -> {
            int selectedRow = cronTable.getSelectedRow();
            if (selectedRow != -1 && selectedRow < funcTabPanel.getCrontab().size()) {
                DialogUtil.showDialog(cronTable, "确定要删除这个定时任务吗？", "删除定时任务", () -> {
                    funcTabPanel.removeCron(funcTabPanel.getCrontab().get(selectedRow));
                    Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "删除成功");
                    reloadCronTable();
                    this.reloadCronTasks();
                }, () -> {

                });
            }
        });

        // 初始加载数据
        reloadCronTable();

        return scrollPane1;
    }

    private JScrollPane initHttpConfigTable() {
        final JScrollPane scrollPane = new JScrollPane();
        httpConfigTable = new JTable();
        scrollPane.setViewportView(httpConfigTable);

        String[] httpTableHeaderNames = {"HTTP方法", "路径", "描述", "绑定脚本", "最后访问", "排序", "启用状态"};
        Object[][] httpTableEmptyData = new Object[0][httpTableHeaderNames.length];
        DefaultTableModel httpTableModel = new DefaultTableModel(httpTableEmptyData, httpTableHeaderNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 所有单元格都不可编辑
            }
        };
        httpConfigTable.setModel(httpTableModel);

        // 设置表头属性
        httpConfigTable.getTableHeader().setReorderingAllowed(false);
        httpConfigTable.setAutoCreateRowSorter(true);
        httpConfigTable.setRowHeight(25);
        httpConfigTable.setPreferredScrollableViewportSize(new Dimension(600, 200));

        // 确保表格可以处理点击事件
        httpConfigTable.setCellSelectionEnabled(true);
        httpConfigTable.setRowSelectionAllowed(true);
        httpConfigTable.setColumnSelectionAllowed(true);

        // 设置列宽
        TableColumn methodColumn = httpConfigTable.getColumnModel().getColumn(0);
        methodColumn.setPreferredWidth(80);
        methodColumn.setMaxWidth(80);
        methodColumn.setMinWidth(80);

        TableColumn pathColumn = httpConfigTable.getColumnModel().getColumn(1);
        pathColumn.setPreferredWidth(150);

        TableColumn descColumn = httpConfigTable.getColumnModel().getColumn(2);
        descColumn.setPreferredWidth(150);

        TableColumn scriptColumn = httpConfigTable.getColumnModel().getColumn(3);
        scriptColumn.setPreferredWidth(150);

        TableColumn lastAccessColumn = httpConfigTable.getColumnModel().getColumn(4);
        lastAccessColumn.setPreferredWidth(150);

        TableColumn orderColumn = httpConfigTable.getColumnModel().getColumn(5);
        orderColumn.setPreferredWidth(60);
        orderColumn.setMaxWidth(60);
        orderColumn.setMinWidth(60);

        TableColumn enabledColumn = httpConfigTable.getColumnModel().getColumn(6);
        enabledColumn.setPreferredWidth(100);
        enabledColumn.setMaxWidth(100);
        enabledColumn.setMinWidth(100);

        // 为启用状态列设置按钮渲染器
        enabledColumn.setCellRenderer(new HttpEnabledButtonRenderer());

        // 创建右键菜单
        JPopupMenu httpTablePopupMenu = new JPopupMenu();
        JMenuItem editHttpMenuItem = new JMenuItem("编辑HTTP配置");
        JMenuItem deleteHttpMenuItem = new JMenuItem("删除HTTP配置");

        httpTablePopupMenu.add(editHttpMenuItem);
        httpTablePopupMenu.add(deleteHttpMenuItem);

        // 添加鼠标监听器
        httpConfigTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int column = httpConfigTable.columnAtPoint(e.getPoint());
                int row = httpConfigTable.rowAtPoint(e.getPoint());

                // 检查是否点击了启用状态列（第7列，索引为7）
                if (column == 7 && row >= 0 && row < httpConfigTable.getRowCount()) {
                    GutoolFuncTabPanelDefineHttp httpConfig = (GutoolFuncTabPanelDefineHttp) httpConfigTable.getValueAt(row, 7);
                    if (httpConfig != null) {
                        // 切换启用状态
                        boolean newEnabled = !(httpConfig.getEnabled() != null ? httpConfig.getEnabled() : true);
                        httpConfig.setEnabled(newEnabled);

                        // 通过调用 sortHttpConfigs 触发自动保存
                        funcTabPanel.sortHttpConfigs();

                        Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, newEnabled ? "已启用" : "已禁用");

                        reloadHttpConfigTable();
                        // 重启HTTP服务器以应用新配置
                        reloadHttpServer();

                        return;
                    }
                }

                // 处理右键菜单
                if (SwingUtilities.isRightMouseButton(e)) {
                    showHttpPopup(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showHttpPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showHttpPopup(e);
                }
            }

            private void showHttpPopup(MouseEvent e) {
                int row = httpConfigTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    httpConfigTable.setRowSelectionInterval(row, row);
                    // 根据是否有选中项来启用/禁用编辑和删除菜单
                    boolean hasSelection = row >= 0 && row < httpConfigTable.getRowCount();
                    editHttpMenuItem.setEnabled(hasSelection);
                    deleteHttpMenuItem.setEnabled(hasSelection);
                    httpTablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        editHttpMenuItem.addActionListener(e -> {
            int selectedRow = httpConfigTable.getSelectedRow();
            if (selectedRow != -1 && selectedRow < funcTabPanel.getHttpConfigs().size()) {
                GutoolFuncTabPanelDefineHttp httpConfig = funcTabPanel.getHttpConfigs().get(selectedRow);
                FuncTablePanelHttpDialog dialog = new FuncTablePanelHttpDialog(httpConfig, updatedConfig -> {
                    funcTabPanel.sortHttpConfigs();
                    Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "保存成功");
                    reloadHttpConfigTable();
                    // 重启HTTP服务器以应用新配置
                    reloadHttpServer();
                });
                dialog.pack();
                dialog.setVisible(true);
            }
        });

        deleteHttpMenuItem.addActionListener(e -> {
            int selectedRow = httpConfigTable.getSelectedRow();
            if (selectedRow != -1 && selectedRow < funcTabPanel.getHttpConfigs().size()) {
                GutoolFuncTabPanelDefineHttp httpConfig = funcTabPanel.getHttpConfigs().get(selectedRow);
                funcTabPanel.removeHttpConfig(httpConfig);
                Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "删除成功");
                reloadHttpConfigTable();
                // 重启HTTP服务器以应用新配置
                reloadHttpServer();
            }
        });

        // 初始加载数据
        reloadHttpConfigTable();

        return scrollPane;
    }

    /**
     * 根据类型更新标签页显示
     */
    private void updateTabbedPane() {
        int buttonListTabIndex = -1;
        for (int i = 0; i < tabbedPane1.getTabCount(); i++) {
            if ("按钮列表".equals(tabbedPane1.getTitleAt(i))) {
                buttonListTabIndex = i;
                break;
            }
        }
        if (!"default".equals(panelType) && buttonListTabIndex != -1) {
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
            data[2] = funcDataObjectList.stream().filter(funcData -> ((Long) funcData[0]).equals(cron.getCronTriggerFuncId())).map(funcData -> ((String) funcData[1])).findFirst().orElse("未知脚本");
            data[3] = cron.getNextExecutionTime() != null ? DateUtil.format(cron.getNextExecutionTime(), "yyyy-MM-dd HH:mm:ss") : "未计算";
            data[4] = cron.getOrder();
            data[5] = cron;
            model.addRow(data);
        }
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
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
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
     * HTTP 启用状态按钮渲染器
     */
    private class HttpEnabledButtonRenderer extends JButton implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof GutoolFuncTabPanelDefineHttp) {
                GutoolFuncTabPanelDefineHttp httpConfig = (GutoolFuncTabPanelDefineHttp) value;
                boolean enabled = httpConfig.getEnabled() != null ? httpConfig.getEnabled() : true;

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
     * 重新加载HTTP配置表格数据
     */
    public void reloadHttpConfigTable() {
        DefaultTableModel model = (DefaultTableModel) httpConfigTable.getModel();
        model.setRowCount(0);

        List<Object[]> funcDataObjectList = GutoolPoQueryRepository.selectFuncAllDataObjectList();
        for (GutoolFuncTabPanelDefineHttp httpConfig : funcTabPanel.getHttpConfigs()) {
            Object[] data = new Object[8];
            data[0] = httpConfig.getMethod() != null ? httpConfig.getMethod() : "GET";
            data[1] = httpConfig.getPath() != null ? httpConfig.getPath() : "/";
            data[2] = httpConfig.getDescription() != null ? httpConfig.getDescription() : "";
            data[3] = funcDataObjectList.stream().filter(funcData -> ((Long) funcData[0]).equals(httpConfig.getHttpTriggerFuncId())).map(funcData -> ((String) funcData[1])).findFirst().orElse("未绑定脚本");
            data[4] = httpConfig.getLastAccessTime() != null ? DateUtil.format(httpConfig.getLastAccessTime(), "yyyy-MM-dd HH:mm:ss") : "未访问";
            data[5] = httpConfig.getOrder() != null ? httpConfig.getOrder() : 0;
            data[6] = httpConfig;
            model.addRow(data);
        }
    }

    /**
     * 启动定时任务
     */
    private void startCronTasks() {
        if ("cron".equals(panelType) && cronTaskScheduler != null) {
            if (funcTabPanel.getDefine().getAutoEnabled()
                    && funcTabPanel.getCrontab().stream()
                    .anyMatch(GutoolFuncTabPanelDefineCron::getEnabled)) {
                cronTaskScheduler.startTasks();
            }
            // 更新按钮状态
            updateCronToggleButton();
            // 更新状态标签
            reloadCronInfoLabel();
        }
    }

    /**
     * 启动HTTP服务
     */
    private void startHttpServer() {
        if ("http".equals(panelType) && httpServer != null) {
            try {
                if (funcTabPanel.getDefine().getAutoEnabled()
                        && funcTabPanel.getHttpConfigs().stream()
                        .anyMatch(GutoolFuncTabPanelDefineHttp::getEnabled)) {
                    httpServer.start();
                    resultArea.append("HTTP服务器已启动，端口: " + httpServer.getPort() + "\n");
                }
                // 更新按钮状态
                updateServerToggleButton();
            } catch (Exception e) {
                resultArea.append("启动HTTP服务器失败: " + e.getMessage() + "\n");
                log.error("启动HTTP服务器失败", e);
            }
            reloadServerInfoLabel();
        }
    }

    /**
     * 停止定时任务
     */
    private void stopCronTasks() {
        if ("cron".equals(panelType) && cronTaskScheduler != null) {
            cronTaskScheduler.stopTasks();
            // 更新按钮状态
            updateCronToggleButton();
            // 更新状态标签
            reloadCronInfoLabel();
        }
    }

    /**
     * 停止HTTP服务
     */
    private void stopHttpServer() {
        if ("http".equals(panelType) && httpServer != null) {
            try {
                httpServer.stop();
                resultArea.append("HTTP服务器已停止\n");
                // 更新按钮状态
                updateServerToggleButton();
            } catch (Exception e) {
                resultArea.append("停止HTTP服务器失败: " + e.getMessage() + "\n");
                log.error("停止HTTP服务器失败", e);
            }
            reloadServerInfoLabel();
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
     * 重启HTTP服务
     */
    public void reloadHttpServer() {
        if ("http".equals(panelType) && httpServer != null) {
            try {
                httpServer.restart();
                resultArea.append("HTTP服务器已重启，端口: " + httpServer.getPort() + "\n");
            } catch (Exception e) {
                resultArea.append("重启HTTP服务器失败: " + e.getMessage() + "\n");
                log.error("重启HTTP服务器失败", e);
            }
            reloadServerInfoLabel();
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        stopCronTasks();
        stopHttpServer();
        if (cronTaskScheduler != null) {
            cronTaskScheduler.shutdown();
            cronTaskScheduler = null;
        }
        httpServer = null;
        serverToggleButton = null;
        serverInfoLabel = null;
        cronToggleButton = null;
        cronInfoLabel = null;
    }

}
