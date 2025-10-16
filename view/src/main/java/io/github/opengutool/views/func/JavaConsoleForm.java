package io.github.opengutool.views.func;

import cn.hutool.Hutool;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import io.github.opengutool.GutoolApp;
import io.github.opengutool.common.ddd.GutoolDDDFactory;
import io.github.opengutool.domain.formatter.GroovyCodeFormatter;
import io.github.opengutool.domain.func.GutoolFunc;
import io.github.opengutool.domain.func.GutoolFuncContainer;
import io.github.opengutool.domain.func.GutoolFuncTabPanel;
import io.github.opengutool.repository.GutoolPoQueryRepository;
import io.github.opengutool.repository.GutoolPoRepository;
import io.github.opengutool.views.component.textviewer.JavaRSyntaxTextViewer;
import io.github.opengutool.views.component.textviewer.JavaRTextScrollPane;
import io.github.opengutool.views.form.MainWindow;
import io.github.opengutool.views.script.FuncTabPanelDialog;
import io.github.opengutool.views.script.ScriptRunDialog;
import io.github.opengutool.views.script.ScriptRunnerForm;
import io.github.opengutool.views.util.DialogUtil;
import io.github.opengutool.views.util.JTableUtil;
import io.github.opengutool.views.util.UndoUtil;
import io.github.pigmesh.ai.deepseek.core.DeepSeekClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttp;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.fife.rsta.ac.LanguageSupportFactory;
import org.fife.rsta.ac.java.JavaCompletionProvider;
import org.fife.rsta.ac.java.JavaLanguageSupport;
import org.fife.rsta.ac.java.JavaShorthandCompletionCache;
import org.fife.rsta.ac.java.JavaTemplateCompletion;
import org.fife.rsta.ac.java.buildpath.DirLibraryInfo;
import org.fife.rsta.ui.GoToDialog;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.SearchEngine;
import raven.toast.Notifications;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Getter
public class JavaConsoleForm {
    private JPanel javaConsolePanel;
    private JButton run;
    private JButton clean;
    private JPanel leftPanel;
    private JTextArea resultArea;
    private JSplitPane splitPane;
    private JButton format;
    private JPanel rightPanel;
    private JTable funcTable;
    private JButton save;
    private JTabbedPane resultTabbedPane;
    private JTable historyTable;
    private JButton add;
    private JButton findButton;
    private JButton replaceButton;
    private JButton gotoButton;
    private JPanel tablePanel;
    private JSplitPane middleSplitPane;
    private JTabbedPane funcTabbedPane;
    private FindDialog findDialog;
    private ReplaceDialog replaceDialog;
    private GoToDialog gotoDialog;
    private static JavaConsoleForm javaConsoleForm;

    private static Map<String, List<Completion>> COMPLETION_CLASS_METHOD_MAP = new ConcurrentHashMap<>();

    private JavaRSyntaxTextViewer textArea;
    private JavaRTextScrollPane scrollPane;

    private static AtomicBoolean AUTO_COMPLETION_ENABLED = new AtomicBoolean(true);
    private static AtomicBoolean running = new AtomicBoolean(false);

    public JavaConsoleForm() {
        UndoUtil.register(this);
    }

    public static JavaConsoleForm getInstance() {
        if (null == javaConsoleForm) {
            javaConsoleForm = new JavaConsoleForm();
        }
        return javaConsoleForm;
    }

    public static void init() {
        javaConsoleForm = getInstance();
        javaConsoleForm.textArea = new JavaRSyntaxTextViewer();
        javaConsoleForm.textArea.setRequestFocusEnabled(true);
        javaConsoleForm.textArea.requestFocusInWindow();
        LanguageSupportFactory.get().register(javaConsoleForm.textArea);
        // 获取并配置 Java 语言支持
        JavaLanguageSupport javaSupport = (JavaLanguageSupport)
                LanguageSupportFactory.get().getSupportFor(SyntaxConstants.SYNTAX_STYLE_JAVA);
        JavaCompletionProvider provider = javaSupport.getCompletionProvider(javaConsoleForm.textArea);
        DefaultCompletionProvider sourceCompletionProvider = (DefaultCompletionProvider) provider.getDefaultCompletionProvider();
        sourceCompletionProvider.setAutoActivationRules(true, ".");

        JavaShorthandCompletionCache shorthandCompletionCache = new JavaShorthandCompletionCache(sourceCompletionProvider, new DefaultCompletionProvider());
        // 加入 groovy 基本关键字
        for (String kw : new String[]{"def", "println", "printf", "true", "false", "null", "import", "run", "return"}) {
            shorthandCompletionCache.addShorthandCompletion(
                    new JavaTemplateCompletion(sourceCompletionProvider,
                            kw, kw, kw + " ${cursor}",
                            kw, "")
            );
        }

        provider.setShorthandCompletionCache(shorthandCompletionCache);

        javaSupport.setAutoCompleteEnabled(true);
        javaSupport.setParameterAssistanceEnabled(true);
        javaSupport.setShowDescWindow(false);
        javaSupport.setAutoActivationEnabled(true);
        javaSupport.setAutoActivationDelay(60);


        addAutoCompletionToJarManager(javaSupport,
                GutoolApp.class,
                // hutool、apache.commons、guava、okhttp
                Hutool.class, IOUtils.class, Lists.class, OkHttp.class, DeepSeekClient.class,
                // commons-codec、commons-beanutils、commons-collections、commons-lang
                Charsets.class, BeanUtils.class, ListUtils.class, CharUtils.class);
        // 添加自定义的焦点处理
        javaConsoleForm.textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                // 重新启用自动激活
                SwingUtilities.invokeLater(() -> {
                    if (AUTO_COMPLETION_ENABLED.compareAndSet(false, true)) {
                        javaSupport.setAutoActivationEnabled(true);
                    }
                });
            }

            @Override
            public void focusLost(FocusEvent e) {
                // 检查是否是因为IME导致的焦点丢失
                if (!e.isTemporary()) {
                    if (AUTO_COMPLETION_ENABLED.compareAndSet(true, false)) {
                        javaSupport.setAutoActivationEnabled(false);
                    }
                }
            }
        });
        javaConsoleForm.textArea.addInputMethodListener(new InputMethodListener() {
            @Override
            public void inputMethodTextChanged(InputMethodEvent event) {
                if (event.getCommittedCharacterCount() == 0) {
                    if (AUTO_COMPLETION_ENABLED.compareAndSet(true, false)) {
                        javaSupport.setAutoActivationEnabled(false);
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        if (AUTO_COMPLETION_ENABLED.compareAndSet(false, true)) {
                            javaSupport.setAutoActivationEnabled(true);
                            doCompletion();
                        }
                    });
                }
            }

            @Override
            public void caretPositionChanged(InputMethodEvent event) {
                // 处理光标位置变化
            }
        });

        // 添加KeyListener来处理IME取消等情况
        javaConsoleForm.textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyTyped(KeyEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (AUTO_COMPLETION_ENABLED.compareAndSet(false, true)) {
                        javaSupport.setAutoActivationEnabled(true);
                    }
                });
            }
        });
        // 设置一个不自动完成
        AutoCompletion ac = ReflectUtil.invoke(
                LanguageSupportFactory.get().getSupportFor(SyntaxConstants.SYNTAX_STYLE_JAVA),
                "getAutoCompletionFor", javaConsoleForm.textArea);
        ac.setAutoCompleteSingleChoices(false);

        javaConsoleForm.scrollPane = new JavaRTextScrollPane(javaConsoleForm.textArea);
        javaConsoleForm.leftPanel.add(javaConsoleForm.scrollPane, BorderLayout.CENTER);
        javaConsoleForm.splitPane.setDividerLocation((int) (GutoolApp.mainFrame.getWidth() / 2));
        javaConsoleForm.middleSplitPane.setDividerLocation((int) (GutoolApp.mainFrame.getWidth() / 6));

        // 初始化查找/替换对话框
        initSearchDialogs();

        // 添加键盘快捷键
        addKeyboardShortcuts();

        addListeners();

        initFuncListTable();

        // Ctrl+S 保存
        javaConsoleForm.textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "save");
        javaConsoleForm.textArea.getActionMap().put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                javaConsoleForm.saveFunc();
            }
        });
        // Ctrl+Shift+S 格式化并保存
        javaConsoleForm.textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK), "formatsave");
        javaConsoleForm.textArea.getActionMap().put("formatsave", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                javaConsoleForm.codeFormatter();
                javaConsoleForm.saveFunc();
            }
        });


        initFunTabPanel();

        javaConsoleForm.getFuncTabbedPane().addTab("+", funTabEmptyPanel);
        javaConsoleForm.getFuncTabbedPane().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                javaConsoleForm.funTabNewToolTab();
            }
        });


        javaConsoleForm.getResultTabbedPane().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final JTabbedPane resultTabbedPane = javaConsoleForm.getResultTabbedPane();
                int selectedIndex = resultTabbedPane.getSelectedIndex();
                String title = resultTabbedPane.getTitleAt(selectedIndex);
                if ("历史记录".equals(title)) {
                    int funcSelectedRow = javaConsoleForm.getFuncTable().getSelectedRow();
                    if (funcSelectedRow != -1) {
                        // 获取当前选中行的 id
                        Object funcIdValue = javaConsoleForm.getFuncTable().getModel().getValueAt(funcSelectedRow, 0);
                        javaConsoleForm.reloadHistoryListTable((Long) funcIdValue);
                    }
                }
            }
        });

    }

    private static void initFunTabPanel() {
        JTabbedPane funcTabbedPane = javaConsoleForm.getFuncTabbedPane();
        List<GutoolFuncTabPanel> funcTabPanelList = GutoolPoQueryRepository.selectFuncTabPanelAll();
        Map<String, ScriptRunnerForm> funcRunFormMap = new HashMap<>();
        for (GutoolFuncTabPanel tabPanel : funcTabPanelList) {
            ScriptRunnerForm funcRunForm = new ScriptRunnerForm(tabPanel, funcTabbedPane);
            funcTabbedPane.addTab(tabPanel.getName(), funcRunForm.getFuncRunPanel());
            funcRunFormMap.put(tabPanel.getName(), funcRunForm);
        }
        funcTabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int selectedIndex = funcTabbedPane.getSelectedIndex();
                final String selectTitle = funcTabbedPane.getTitleAt(selectedIndex);
                final ScriptRunnerForm funcRunForm = funcRunFormMap.get(selectTitle);
                if (Objects.nonNull(funcRunForm)) {
                    funcRunForm.reloadBtnListTable();
                }

                if ("Script".equals(selectTitle)) {
                    int funcSelectedRow = javaConsoleForm.getFuncTable().getSelectedRow();
                    if (funcSelectedRow != -1) {
                        // 获取当前选中行的 id
                        Object funcIdValue = javaConsoleForm.getFuncTable().getModel().getValueAt(funcSelectedRow, 0);
                        javaConsoleForm.reloadHistoryListTable((Long) funcIdValue);
                    }
                }
            }
        });
    }

    private static volatile boolean isAddingTab = false;
    private static JPanel funTabEmptyPanel = new JPanel();

    public void funTabNewToolTab() {
        if (isAddingTab) return; // 避免递归
        int selectedIndex = funcTabbedPane.getSelectedIndex();
        if (selectedIndex == funcTabbedPane.getTabCount() - 1) {
            isAddingTab = true;
            int plusIndex = funcTabbedPane.indexOfTab("+");
            // 移除
            if (plusIndex == funcTabbedPane.getTabCount() - 1) {
                funcTabbedPane.removeTabAt(plusIndex);
            }
            // new tab
            try {
                FuncTabPanelDialog dialog = new FuncTabPanelDialog(null, funcTabPanel -> {
                    ScriptRunnerForm funcRunForm = new ScriptRunnerForm(funcTabPanel, funcTabbedPane);
                    funcTabbedPane.addTab(funcTabPanel.getName(), funcRunForm.getFuncRunPanel());
                    // 自动选中新加的 tab
                    funcTabbedPane.setSelectedIndex(funcTabbedPane.getTabCount() - 1);
                    isAddingTab = false;
                    if (funcTabbedPane.indexOfTab("+") <= 0) {
                        funcTabbedPane.addTab("+", funTabEmptyPanel);
                    }
                }, funcTabPanel -> {
                    isAddingTab = false;
                    if (funcTabbedPane.indexOfTab("+") <= 0) {
                        funcTabbedPane.addTab("+", funTabEmptyPanel);
                    }
                });
                dialog.pack();
                dialog.setVisible(true);
            } catch (Exception e) {
                log.error("", e);
                isAddingTab = false;
                if (funcTabbedPane.indexOfTab("+") <= 0) {
                    funcTabbedPane.addTab("+", funTabEmptyPanel);
                }
            }
        }
    }

    private static void addAutoCompletionToJarManager(JavaLanguageSupport javaSupport, Class<?>... rootClazz) {
        if (Objects.nonNull(rootClazz) && rootClazz.length > 0) {
            try {
                log.error("add CurrentJre to groovy AutoCompletion");
                javaSupport.getJarManager().addCurrentJreClassFileSource();
            } catch (IOException e) {
                log.error("add CurrentJre to groovy AutoCompletion error:", e);
            }
            for (Class<?> clazz : rootClazz) {
                try {
                    String jarPath = ClassUtil.getLocationPath(clazz);
                    log.info("add jar to groovy AutoCompletion:{} in {}", clazz.getName(), jarPath);
                    File file = FileUtil.file(jarPath);
                    if (file.exists()) {
                        if (file.isDirectory()) {
                            javaSupport.getJarManager().addClassFileSource(new DirLibraryInfo(file));
                        } else {
                            javaSupport.getJarManager().addClassFileSource(file);
                        }
                    }
                } catch (Exception e) {
                    log.error("add jar to groovy AutoCompletion error:{}, ", clazz.getName(), e);
                }
            }
        }
    }

    private static void doCompletion() {
        SwingUtilities.invokeLater(() -> {
            if (javaConsoleForm.textArea.hasFocus() &&
                    AUTO_COMPLETION_ENABLED.get()) {
                // 添加小延迟确保文本已更新
                Timer timer = new Timer(20, e -> {
                    AutoCompletion ac = ReflectUtil.invoke(
                            LanguageSupportFactory.get().getSupportFor(SyntaxConstants.SYNTAX_STYLE_JAVA),
                            "getAutoCompletionFor", javaConsoleForm.textArea);
                    ac.doCompletion();
                });
                timer.setRepeats(false);
                timer.start();
            }
        });
    }


    public void codeFormatter() {
        String code = javaConsoleForm.getTextArea().getText();
        javaConsoleForm.getTextArea().setText(GroovyCodeFormatter.format(code));
    }


    public void saveFunc() {
        JTable funcListTable = javaConsoleForm.getFuncTable();
        int selectedRow = funcListTable.getSelectedRow();

        if (selectedRow == -1) {
            Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "请先选择一个再保存");
            return;
        }

        // 获取当前选中行的 id
        Object idObj = funcListTable.getModel().getValueAt(selectedRow, 0);
        // 获取文本区内容
        String content = javaConsoleForm.getTextArea().getText();

        // 更新数据
        if (GutoolFuncContainer.getFuncById((Long) idObj).updateScript(content)) {
            Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "保存成功");
        } else {
            Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "保存失败");
        }
    }


    private static void initFuncListTable() {
        String[] headerNames = {"id", "名称"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        JTable funcListTable = javaConsoleForm.getFuncTable();
        funcListTable.setDefaultEditor(Object.class, null);
        funcListTable.setModel(model);
        // 隐藏表头
        JTableUtil.hideTableHeader(funcListTable);
        // 隐藏id列
        JTableUtil.hideColumn(funcListTable, 0);


        String[] historyHeaderNames = {"id", "运行脚本", "运行参数", "返回结果", "cost", "status", "启动时间"};
        Object[][] emptyData = new Object[0][historyHeaderNames.length];
        DefaultTableModel historyModel = new DefaultTableModel(emptyData, historyHeaderNames);
        JTable historyListTable = javaConsoleForm.getHistoryTable();
        // historyListTable.setDefaultEditor(Object.class, null);
        historyListTable.setModel(historyModel);
        TableColumn cost = historyListTable.getColumnModel().getColumn(4);
        cost.setMaxWidth(40);
        cost.setMinWidth(40);

        TableColumn status = historyListTable.getColumnModel().getColumn(5);
        status.setMaxWidth(60);
        status.setMinWidth(60);

        // 隐藏表头
        // JTableUtil.hideTableHeader(historyListTable);
        // 隐藏id列
        JTableUtil.hideColumn(historyListTable, 0);


        // 左侧表格增加右键菜单
        JPopupMenu historyListTablePopupMenu = new JPopupMenu();
        JMenuItem deleteMenuItem = new JMenuItem("删除");
        JMenuItem reloadMenuItem = new JMenuItem("刷新");
        JMenuItem clearAllMenuItem = new JMenuItem("清理全部");
        historyListTablePopupMenu.add(deleteMenuItem);
        historyListTablePopupMenu.add(reloadMenuItem);
        historyListTablePopupMenu.addSeparator();
        historyListTablePopupMenu.add(clearAllMenuItem);
        historyListTable.setComponentPopupMenu(null);
        // 添加鼠标监听器
        historyListTable.addMouseListener(new MouseAdapter() {
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
                    int row = historyListTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        // 如果右键点到的是某一行，就选中它（可选）
                        historyListTable.setRowSelectionInterval(row, row);
                        // 弹出菜单
                        historyListTablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        deleteMenuItem.addActionListener(e -> {
            int funcSelectedRow = funcListTable.getSelectedRow();
            int historySelectedRow = historyListTable.getSelectedRow();
            if (funcSelectedRow != -1 && historySelectedRow != -1) {
                // 获取隐藏列的数据（id）
                Object funcIdValue = model.getValueAt(funcSelectedRow, 0);
                // 获取隐藏列的数据（id）
                Object historyIdValue = historyModel.getValueAt(historySelectedRow, 0);
                GutoolPoRepository.deleteFuncRunHistoryById((Long) historyIdValue);
                javaConsoleForm.reloadHistoryListTable((Long) funcIdValue);
            }
        });

        // 刷新
        reloadMenuItem.addActionListener(e -> {
            int funcSelectedRow = funcListTable.getSelectedRow();
            if (funcSelectedRow != -1) {
                // 获取隐藏列的数据（id）
                Object funcIdValue = model.getValueAt(funcSelectedRow, 0);
                javaConsoleForm.reloadHistoryListTable((Long) funcIdValue);
            }
        });

        // 清理全部
        clearAllMenuItem.addActionListener(e -> {
            int funcSelectedRow = funcListTable.getSelectedRow();
            if (funcSelectedRow != -1) {
                // 获取隐藏列的数据（id）
                Object funcIdValue = model.getValueAt(funcSelectedRow, 0);
                DialogUtil.showDialog(javaConsoleForm.getJavaConsolePanel(), "确定要清理该脚本的所有历史记录吗？", "清理所有历史记录",
                        () -> {
                            GutoolPoRepository.deleteAllFuncRunHistoryByFuncId((Long) funcIdValue);
                            javaConsoleForm.reloadHistoryListTable((Long) funcIdValue);
                            Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "清理成功");
                        },
                        () -> {

                        });
            }
        });

        List<Object[]> funcList = GutoolPoQueryRepository.selectFuncAllDataObjectList();
        funcList.forEach(model::addRow);

        // 添加选中事件监听器
        funcListTable.getSelectionModel().addListSelectionListener(e -> {
            // 避免重复事件触发
            if (!e.getValueIsAdjusting()) {
                int selectedRow = funcListTable.getSelectedRow();
                if (selectedRow != -1) {
                    // 获取隐藏列的数据（id）
                    Object idValue = model.getValueAt(selectedRow, 0);
                    // Object nameValue = model.getValueAt(selectedRow, 1);
                    javaConsoleForm.getTextArea().setText(
                            GutoolFuncContainer.getFuncContentById((Long) idValue));
                    javaConsoleForm.reloadHistoryListTable((Long) idValue);
                }
            }
        });


        JPopupMenu funcListTablePopupMenu = new JPopupMenu();
        JMenuItem funcListTableMenuItem = new JMenuItem("重命名");
        funcListTablePopupMenu.add(funcListTableMenuItem);
        funcListTable.setComponentPopupMenu(null);
        // 添加鼠标监听器
        funcListTable.addMouseListener(new MouseAdapter() {
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
                    int row = funcListTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        // 如果右键点到的是某一行，就选中它（可选）
                        funcListTable.setRowSelectionInterval(row, row);
                        // 弹出菜单
                        funcListTablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
        funcListTableMenuItem.addActionListener(e -> {
            int funcRow = funcListTable.getSelectedRow();
            if (funcRow != -1) {
                Object idObj = funcListTable.getValueAt(funcRow, 0);
                Object nameObj = funcListTable.getValueAt(funcRow, 1);
                updateFuncName(funcRow, (Long) idObj, (String) nameObj);
            }
        });
        funcListTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // 双击判断
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int funcRow = funcListTable.rowAtPoint(e.getPoint());
                    int column = funcListTable.columnAtPoint(e.getPoint());
                    if (funcRow >= 0 && column == 1) {
                        Object idObj = funcListTable.getValueAt(funcRow, 0);
                        Object nameObj = funcListTable.getValueAt(funcRow, 1);
                        updateFuncName(funcRow, (Long) idObj, (String) nameObj);
                    }
                }
            }
        });

        if (funcList.size() > 0) {
            // 选中第一个
            funcListTable.setRowSelectionInterval(0, 0);
        }
    }

    private static void updateFuncName(int row, Long idObj, String oldName) {
        JTable funcListTable = javaConsoleForm.getFuncTable();
        DialogUtil.showInputDialog(
                MainWindow.getInstance().getMainPanel(),
                "重命名脚本",
                "请输入新的名称：",
                oldName,
                newName -> {
                    if (StrUtil.isBlankOrUndefined(newName)
                            || StrUtil.isBlankOrUndefined(newName.trim())
                            || newName.trim().equals(oldName)) {
                        return;
                    }
                    newName = newName.trim();
                    GutoolFunc gutoolFunc = GutoolFuncContainer.getFuncById(idObj);
                    if (!gutoolFunc.setName(newName)) {
                        Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "已存在重名脚本");
                        return;
                    }
                    funcListTable.setValueAt(newName, row, 1);
                    Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "保存成功");
                },
                () -> {

                }
        );
    }


    /**
     * 初始化查找/替换对话框
     */
    private static void initSearchDialogs() {
        // 创建查找对话框
        javaConsoleForm.findDialog = new FindDialog(GutoolApp.mainFrame, new SearchListener() {
            @Override
            public void searchEvent(SearchEvent e) {
                SearchEvent.Type type = e.getType();
                if (type == SearchEvent.Type.FIND) {
                    // 执行查找
                    SearchEngine.find(javaConsoleForm.textArea,
                            javaConsoleForm.findDialog.getSearchContext());
                } else if (type == SearchEvent.Type.MARK_ALL) {
                    // 标记所有匹配项
                    SearchEngine.markAll(javaConsoleForm.textArea,
                            javaConsoleForm.findDialog.getSearchContext());
                }
            }

            @Override
            public String getSelectedText() {
                return javaConsoleForm.textArea.getSelectedText();
            }
        });

        // 创建替换对话框
        javaConsoleForm.replaceDialog = new ReplaceDialog(GutoolApp.mainFrame, new SearchListener() {
            @Override
            public void searchEvent(SearchEvent e) {
                SearchEvent.Type type = e.getType();
                if (type == SearchEvent.Type.REPLACE) {
                    // 执行替换
                    SearchEngine.replace(javaConsoleForm.textArea,
                            javaConsoleForm.replaceDialog.getSearchContext());
                } else if (type == SearchEvent.Type.REPLACE_ALL) {
                    // 替换全部
                    SearchEngine.replaceAll(javaConsoleForm.textArea,
                            javaConsoleForm.replaceDialog.getSearchContext());
                } else if (type == SearchEvent.Type.FIND) {
                    // 执行查找
                    SearchEngine.find(javaConsoleForm.textArea,
                            javaConsoleForm.replaceDialog.getSearchContext());
                } else if (type == SearchEvent.Type.MARK_ALL) {
                    // 标记所有匹配项
                    SearchEngine.markAll(javaConsoleForm.textArea,
                            javaConsoleForm.replaceDialog.getSearchContext());
                }
            }

            @Override
            public String getSelectedText() {
                return javaConsoleForm.textArea.getSelectedText();
            }
        });

        // 创建跳转到行对话框
        javaConsoleForm.gotoDialog = new GoToDialog(GutoolApp.mainFrame);
    }

    /**
     * 添加键盘快捷键
     */
    private static void addKeyboardShortcuts() {
        // Ctrl+Shift+F 查找
        javaConsoleForm.textArea.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                "find");
        javaConsoleForm.textArea.getActionMap().put("find", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                javaConsoleForm.showFindDialog();
            }
        });

        // Ctrl+Shift+R 替换
        javaConsoleForm.textArea.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                "replace");
        javaConsoleForm.textArea.getActionMap().put("replace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                javaConsoleForm.showReplaceDialog();
            }
        });

        // Ctrl+Shift+G 跳转到行
        javaConsoleForm.textArea.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                "goto");
        javaConsoleForm.textArea.getActionMap().put("goto", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                javaConsoleForm.showGoToDialog();
            }
        });
    }


    public static void addListeners() {
        JavaConsoleForm javaConsoleForm = JavaConsoleForm.getInstance();

        // 查找按钮监听器
        javaConsoleForm.getFindButton().addActionListener(e -> {
            javaConsoleForm.showFindDialog();
        });

        // 替换按钮监听器
        javaConsoleForm.getReplaceButton().addActionListener(e -> {
            javaConsoleForm.showReplaceDialog();
        });

        // 跳转按钮监听器
        javaConsoleForm.getGotoButton().addActionListener(e -> {
            javaConsoleForm.showGoToDialog();
        });

        javaConsoleForm.getRun().addActionListener((e) -> {
            String code = javaConsoleForm.getTextArea().getText();
            if (StringUtils.isBlank(code)) {
                return;
            }
            JTable funcListTable = javaConsoleForm.getFuncTable();
            int selectedRow = funcListTable.getSelectedRow();
            if (selectedRow == -1) {
                return;
            }
            Object idObj = funcListTable.getModel().getValueAt(selectedRow, 0);

            if (running.get()) {
                Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "正在执行");
                return;
            }
            ScriptRunDialog dialog = new ScriptRunDialog(funcIn -> {
                if (running.compareAndSet(false, true)) {
                    GutoolFunc gutoolFunc = GutoolFuncContainer.getFuncById((Long) idObj);
                    if (gutoolFunc.updateScript(code)) {
                        gutoolFunc.initRunner(
                                null,
                                funcIn,
                                (msg) -> javaConsoleForm.getResultArea().append(msg),
                                () -> {
                                    running.compareAndSet(true, false);
                                }
                        ).asyncRun(result -> {
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
                                javaConsoleForm.getResultArea().append("result:\n");
                                javaConsoleForm.getResultArea().append(resultText);
                                javaConsoleForm.getResultArea().append("\n");
                            }
                            gutoolFunc.resetRunner();
                            javaConsoleForm.reloadHistoryListTable((Long) idObj);
                        });
                    }
                }
            });
            dialog.pack();
            dialog.setVisible(true);
        });

        javaConsoleForm.getClean().addActionListener(e -> {
            if (running.get()) {
                Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "正在执行");
                return;
            }
            javaConsoleForm.getResultArea().setText("");
        });

        javaConsoleForm.getFormat().addActionListener(e -> javaConsoleForm.codeFormatter());

        javaConsoleForm.getAdd().addActionListener(e -> {

            DialogUtil.showInputDialog(
                    MainWindow.getInstance().getMainPanel(),
                    "新建脚本",
                    "请输入名称：",
                    "",
                    name -> {
                        if (StrUtil.isBlankOrUndefined(name) || StrUtil.isBlankOrUndefined(name.trim())) {
                            Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "名称不能为空");
                            return;
                        }
                        name = name.trim();
                        GutoolFunc func = GutoolDDDFactory.create(new GutoolFunc());
                        if (!func.setName(name)) {
                            Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "名称已存在");
                            return;
                        }
                        // 重新加载表格
                        javaConsoleForm.reloadFuncListTable();

                        // 定位到新增的行（假设 name 是唯一的）
                        JTable funcListTable = javaConsoleForm.getFuncTable();
                        DefaultTableModel model = (DefaultTableModel) funcListTable.getModel();
                        for (int i = 0; i < model.getRowCount(); i++) {
                            Object idValue = model.getValueAt(i, 0);
                            if (idValue != null && idValue.toString().equals(func.getId().toString())) {
                                funcListTable.setRowSelectionInterval(i, i);
                                javaConsoleForm.reloadHistoryListTable((Long) idValue);
                                break;
                            }
                        }
                    },
                    () -> {

                    }
            );
        });

        javaConsoleForm.getSave().addActionListener(e -> javaConsoleForm.saveFunc());

        // Ctrl+C 复制
        javaConsoleForm.getFuncTable().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "copy");
        javaConsoleForm.getFuncTable().getActionMap().put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTable funcListTable = javaConsoleForm.getFuncTable();
                int selectedRow = funcListTable.getSelectedRow();
                if (selectedRow == -1) {
                    return;
                }
                Object nameObj = funcListTable.getModel().getValueAt(selectedRow, 1);
                StringSelection selection = new StringSelection(nameObj.toString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);
            }
        });
    }


    /**
     * 显示查找对话框
     */
    public void showFindDialog() {
        if (findDialog != null) {
            findDialog.setVisible(true);
        }
    }

    /**
     * 显示替换对话框
     */
    public void showReplaceDialog() {
        if (replaceDialog != null) {
            replaceDialog.setVisible(true);
        }
    }

    /**
     * 显示跳转到行对话框
     */
    public void showGoToDialog() {
        if (gotoDialog != null) {
            int maxLineNumber = textArea.getLineCount();
            gotoDialog.setMaxLineNumberAllowed(maxLineNumber);
            gotoDialog.setVisible(true);
            int line = gotoDialog.getLineNumber();
            if (line > 0) {
                try {
                    textArea.setCaretPosition(textArea.getLineStartOffset(line - 1));
                } catch (Exception e) {
                    // 忽略异常
                }
            }
        }
    }

    public void reloadFuncListTable() {
        JTable funcListTable = javaConsoleForm.getFuncTable();
        DefaultTableModel model = (DefaultTableModel) funcListTable.getModel();
        // 清空原有数据
        model.setRowCount(0);
        // 重新获取数据并填充
        GutoolPoQueryRepository.selectFuncAllDataObjectList()
                .forEach(model::addRow);
    }

    public void reloadHistoryListTable(Long funcId) {
        DefaultTableModel model = (DefaultTableModel) javaConsoleForm.getHistoryTable().getModel();
        // 清空原有数据
        model.setRowCount(0);
        // 重新获取数据并填充
        GutoolPoQueryRepository.selectFuncRunHistoryDataObjectListByFuncId(funcId)
                .forEach(model::addRow);
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
        javaConsolePanel = new JPanel();
        javaConsolePanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        funcTabbedPane = new JTabbedPane();
        javaConsolePanel.add(funcTabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        funcTabbedPane.addTab("Script", panel1);
        middleSplitPane = new JSplitPane();
        middleSplitPane.setDividerLocation(26);
        panel1.add(middleSplitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        tablePanel = new JPanel();
        tablePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        middleSplitPane.setLeftComponent(tablePanel);
        final JScrollPane scrollPane1 = new JScrollPane();
        tablePanel.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        funcTable = new JTable();
        scrollPane1.setViewportView(funcTable);
        rightPanel = new JPanel();
        rightPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        middleSplitPane.setRightComponent(rightPanel);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 9, new Insets(8, 5, 0, 5), -1, -1));
        rightPanel.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        clean = new JButton();
        clean.setText("清除输出");
        panel2.add(clean, new GridConstraints(0, 8, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        save = new JButton();
        save.setText("保存代码");
        panel2.add(save, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        format = new JButton();
        format.setText("格式化代码");
        panel2.add(format, new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        run = new JButton();
        run.setText("运行代码");
        panel2.add(run, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        add = new JButton();
        add.setText("新建脚本");
        panel2.add(add, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findButton = new JButton();
        findButton.setText("查找");
        panel2.add(findButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        replaceButton = new JButton();
        replaceButton.setHorizontalAlignment(0);
        replaceButton.setHorizontalTextPosition(11);
        replaceButton.setText("替换");
        replaceButton.setVerticalAlignment(0);
        panel2.add(replaceButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        gotoButton = new JButton();
        gotoButton.setText("跳转");
        panel2.add(gotoButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        rightPanel.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDoubleBuffered(true);
        panel3.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout(0, 0));
        splitPane.setLeftComponent(leftPanel);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(panel4);
        resultTabbedPane = new JTabbedPane();
        panel4.add(resultTabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        resultTabbedPane.addTab("输出打印", panel5);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel5.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        resultArea = new JTextArea();
        scrollPane2.setViewportView(resultArea);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        resultTabbedPane.addTab("历史记录", panel6);
        final JScrollPane scrollPane3 = new JScrollPane();
        panel6.add(scrollPane3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        historyTable = new JTable();
        scrollPane3.setViewportView(historyTable);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return javaConsolePanel;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        Component component = getJavaConsolePanel().getComponent(1);
        ((JSplitPane) component).setDividerLocation((int) (GutoolApp.mainFrame.getWidth() / 8));
    }


}
