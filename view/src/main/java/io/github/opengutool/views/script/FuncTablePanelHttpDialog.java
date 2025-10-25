package io.github.opengutool.views.script;

import cn.hutool.core.util.StrUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import io.github.opengutool.views.util.MacWindowUtil;
import io.github.opengutool.domain.func.GutoolFuncTabPanelDefineHttp;
import io.github.opengutool.repository.GutoolPoQueryRepository;
import io.github.opengutool.views.UiConsts;
import io.github.opengutool.views.util.ComponentUtil;
import raven.toast.Notifications;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * HTTP服务配置对话框
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/10/20
 */
public class FuncTablePanelHttpDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox<String> methodComboBox;
    private JTextField pathField;
    private JTextArea descriptionArea;
    private JComboBox<String> scriptComboBox;
    private JCheckBox enabledCheckBox;
    private JComboBox<String> contentTypeComboBox;
    private JSpinner orderSpinner;
    private JButton testButton;

    private final GutoolFuncTabPanelDefineHttp httpConfig;
    private final Consumer<GutoolFuncTabPanelDefineHttp> saveConsumer;
    private List<Object[]> funcDataList;

    public FuncTablePanelHttpDialog(GutoolFuncTabPanelDefineHttp httpConfig, Consumer<GutoolFuncTabPanelDefineHttp> saveConsumer) {
        this.httpConfig = httpConfig;
        this.saveConsumer = saveConsumer;
        setTitle("HTTP服务配置");
        setIconImage(UiConsts.IMAGE_LOGO_64);
        // 初始化界面
        $$$setupUI$$$();
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 550, 380);
        setContentPane(contentPane);

        setModal(true);
        setResizable(false);
        MacWindowUtil.configureMacFullscreenContent(this);
        MacWindowUtil.configureMacInsets(contentPane, MacWindowUtil.DIALOG_WINDOW_INSETS);

        // 加载数据和设置监听器
        loadData();
        setupListeners();

        getRootPane().setDefaultButton(buttonOK);
    }

    private void loadData() {
        // 加载脚本列表
        funcDataList = GutoolPoQueryRepository.selectFuncAllDataObjectList();
        scriptComboBox.removeAllItems();
        scriptComboBox.addItem("请选择脚本");
        for (Object[] funcData : funcDataList) {
            String scriptName = (String) funcData[1];
            scriptComboBox.addItem(scriptName);
        }

        // 初始化HTTP方法选项
        methodComboBox.removeAllItems();
        methodComboBox.addItem("GET");
        methodComboBox.addItem("POST");
        methodComboBox.addItem("PUT");
        methodComboBox.addItem("DELETE");
        methodComboBox.addItem("PATCH");

        // 初始化内容类型选项
        contentTypeComboBox.removeAllItems();
        contentTypeComboBox.addItem("application/json");
        contentTypeComboBox.addItem("text/plain");
        contentTypeComboBox.addItem("text/html");
        contentTypeComboBox.addItem("application/xml");
        contentTypeComboBox.addItem("application/x-www-form-urlencoded");

        // 加载当前HTTP配置数据
        if (httpConfig != null) {
            methodComboBox.setSelectedItem(httpConfig.getMethod() != null ? httpConfig.getMethod() : "GET");
            pathField.setText(httpConfig.getPath() != null ? httpConfig.getPath() : "/");
            descriptionArea.setText(httpConfig.getDescription());
            enabledCheckBox.setSelected(httpConfig.getEnabled() != null ? httpConfig.getEnabled() : true);
            contentTypeComboBox.setSelectedItem(httpConfig.getContentType() != null ? httpConfig.getContentType() : "application/json");
            orderSpinner.setValue(httpConfig.getOrder() != null ? httpConfig.getOrder() : 0);

            // 设置选中的脚本
            if (httpConfig.getHttpTriggerFuncId() != null) {
                for (int i = 1; i < scriptComboBox.getItemCount(); i++) {
                    String scriptName = scriptComboBox.getItemAt(i);
                    Object[] funcData = funcDataList.stream()
                            .filter(data -> scriptName.equals(data[1]))
                            .findFirst()
                            .orElse(null);
                    if (funcData != null && httpConfig.getHttpTriggerFuncId().equals(funcData[0])) {
                        scriptComboBox.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
    }

    private void setupListeners() {
        buttonOK.addActionListener(e -> onSave());
        buttonCancel.addActionListener(e -> onCancel());
        testButton.addActionListener(e -> onTestPath());

        // 路径变化时进行基本验证
        pathField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validatePath();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validatePath();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validatePath();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void validatePath() {
        String path = pathField.getText().trim();
        if (StrUtil.isNotBlank(path) && !path.startsWith("/")) {
            pathField.setForeground(Color.RED);
        } else {
            pathField.setForeground(Color.BLACK);
        }
    }

    private void onTestPath() {
        String path = pathField.getText().trim();
        if (StrUtil.isBlank(path)) {
            Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "请输入路径");
            return;
        }

        if (!path.startsWith("/")) {
            Notifications.getInstance().show(Notifications.Type.ERROR, Notifications.Location.TOP_CENTER, "路径必须以 / 开头");
            return;
        }

        // 这里可以添加更多路径验证逻辑
        Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "路径格式正确");
    }

    private void onSave() {
        String path = pathField.getText().trim();
        if (StrUtil.isBlank(path)) {
            Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "请输入路径");
            return;
        }

        if (!path.startsWith("/")) {
            Notifications.getInstance().show(Notifications.Type.ERROR, Notifications.Location.TOP_CENTER, "路径必须以 / 开头");
            return;
        }

        int selectedIndex = scriptComboBox.getSelectedIndex();
        if (selectedIndex <= 0) {
            Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "请选择绑定脚本");
            return;
        }

        // 获取脚本ID
        String selectedScriptName = scriptComboBox.getItemAt(selectedIndex);
        Long scriptId = null;
        for (Object[] funcData : funcDataList) {
            if (selectedScriptName.equals(funcData[1])) {
                scriptId = (Long) funcData[0];
                break;
            }
        }

        if (scriptId == null) {
            Notifications.getInstance().show(Notifications.Type.ERROR, Notifications.Location.TOP_CENTER, "无法获取脚本ID");
            return;
        }

        // 保存数据
        httpConfig.setMethod((String) methodComboBox.getSelectedItem());
        httpConfig.setPath(path);
        httpConfig.setDescription(descriptionArea.getText());
        httpConfig.setHttpTriggerFuncId(scriptId);
        httpConfig.setEnabled(enabledCheckBox.isSelected());
        httpConfig.setContentType((String) contentTypeComboBox.getSelectedItem());
        httpConfig.setOrder((Integer) orderSpinner.getValue());

        saveConsumer.accept(httpConfig);
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(8, 4, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel1.setBorder(BorderFactory.createTitledBorder(null, "HTTP服务属性", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("描述");
        panel1.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(2, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(400, 60), null, 0, false));
        descriptionArea = new JTextArea();
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        scrollPane1.setViewportView(descriptionArea);
        final JLabel label2 = new JLabel();
        label2.setText("绑定脚本");
        panel1.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scriptComboBox = new JComboBox();
        panel1.add(scriptComboBox, new GridConstraints(3, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("内容类型");
        panel1.add(label3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        contentTypeComboBox = new JComboBox();
        panel1.add(contentTypeComboBox, new GridConstraints(4, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("启用");
        panel1.add(label4, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        enabledCheckBox = new JCheckBox();
        enabledCheckBox.setText("");
        panel1.add(enabledCheckBox, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("排序");
        panel1.add(label5, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        orderSpinner = new JSpinner();
        panel1.add(orderSpinner, new GridConstraints(6, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("路径");
        panel1.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pathField = new JTextField();
        pathField.setText("/");
        panel1.add(pathField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        testButton = new JButton();
        testButton.setText("测试路径");
        panel1.add(testButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("HTTP方法");
        panel1.add(label7, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        methodComboBox = new JComboBox();
        panel1.add(methodComboBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("取消");
        panel3.add(buttonCancel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("确定");
        panel3.add(buttonOK, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        contentPane.add(spacer2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
