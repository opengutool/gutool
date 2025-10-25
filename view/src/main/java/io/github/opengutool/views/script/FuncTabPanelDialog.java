package io.github.opengutool.views.script;

import cn.hutool.core.util.StrUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import io.github.opengutool.common.ddd.GutoolDDDFactory;
import io.github.opengutool.domain.func.GutoolFuncTabPanel;
import io.github.opengutool.views.UiConsts;
import io.github.opengutool.views.util.ComponentUtil;
import io.github.opengutool.views.util.MacWindowUtil;
import raven.toast.Notifications;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class FuncTabPanelDialog extends JDialog {
    private JPanel contentPane;
    private JButton saveButton;
    private JButton buttonCancel;
    private JPanel formPanel;
    private JTextField nameTextField;
    private JTextArea remarkTextArea;
    private JCheckBox outTextCheckBox;
    private JComboBox typeComboBox;
    private JLabel portLabel;
    private JSpinner portSpinner;
    private JLabel threadLabel;
    private JSpinner threadSpinner;

    private final GutoolFuncTabPanel funcTabPanel;
    private final Consumer<GutoolFuncTabPanel> saveConsumer;
    private final Consumer<GutoolFuncTabPanel> cancelConsumer;

    // 类型映射：中文显示 -> 英文存储
    private static final Map<String, String> TYPE_DISPLAY_TO_STORAGE = new HashMap<>();
    private static final Map<String, String> TYPE_STORAGE_TO_DISPLAY = new HashMap<>();

    static {
        TYPE_DISPLAY_TO_STORAGE.put("按钮触发", "default");
        TYPE_DISPLAY_TO_STORAGE.put("定时触发", "cron");
        TYPE_DISPLAY_TO_STORAGE.put("请求触发", "http");

        TYPE_STORAGE_TO_DISPLAY.put("default", "按钮触发");
        TYPE_STORAGE_TO_DISPLAY.put("cron", "定时触发");
        TYPE_STORAGE_TO_DISPLAY.put("http", "请求触发");
    }

    public FuncTabPanelDialog(GutoolFuncTabPanel funcTabPanel, Consumer<GutoolFuncTabPanel> saveConsumer, Consumer<GutoolFuncTabPanel> cancelConsumer) {
        this.funcTabPanel = funcTabPanel;
        this.saveConsumer = saveConsumer;
        this.cancelConsumer = cancelConsumer;

        setTitle("面板配置");
        setIconImage(UiConsts.IMAGE_LOGO_64);

        $$$setupUI$$$();

        SpinnerNumberModel portModel = (SpinnerNumberModel) portSpinner.getModel();
        portModel.setMaximum(65535);
        portModel.setMinimum(0);

        SpinnerNumberModel threadModel = (SpinnerNumberModel) threadSpinner.getModel();
        threadModel.setMaximum(50);
        threadModel.setMinimum(1);
        threadModel.setValue(5); // 默认5个线程

        if (Objects.nonNull(funcTabPanel)) {
            nameTextField.setText(funcTabPanel.getName());
            remarkTextArea.setText(funcTabPanel.getRemark());
        }
        if (Objects.nonNull(funcTabPanel) && Objects.nonNull(funcTabPanel.getDefine())) {
            outTextCheckBox.setSelected(funcTabPanel.getDefine().getOutTextEnabled());
            // 设置端口
            portSpinner.setValue(funcTabPanel.getDefine().getPort() < 0 || funcTabPanel.getDefine().getPort() > 65535 ? 8080 : funcTabPanel.getDefine().getPort());
            // 设置线程数量
            if (funcTabPanel.getDefine().getThreadPoolSize() != null && funcTabPanel.getDefine().getThreadPoolSize() > 0) {
                threadSpinner.setValue(funcTabPanel.getDefine().getThreadPoolSize());
            }
            // 设置类型选择 - 将英文存储值转换为中文显示
            String type = funcTabPanel.getDefine().getType();
            if (StrUtil.isNotBlank(type)) {
                String displayType = TYPE_STORAGE_TO_DISPLAY.get(type);
                if (StrUtil.isNotBlank(displayType)) {
                    typeComboBox.setSelectedItem(displayType);
                }
            }
        }
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 350, 290);
        setContentPane(contentPane);
        setResizable(false);
        setModal(true);
        MacWindowUtil.configureMacFullscreenContent(this);
        MacWindowUtil.configureMacInsets(contentPane, MacWindowUtil.DIALOG_WINDOW_INSETS);

        getRootPane().setDefaultButton(saveButton);

        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
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

        // typeComboBox 选择变化监听器
        typeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedDisplayType = (String) typeComboBox.getSelectedItem();
                String selectedType = TYPE_DISPLAY_TO_STORAGE.get(selectedDisplayType);
                if (Objects.nonNull(funcTabPanel) && Objects.nonNull(funcTabPanel.getDefine())) {
                    funcTabPanel.getDefine().setType(selectedType);
                }

                // 根据类型控制结果区选项的可见性
                updateResultAreaVisibility(selectedType);
            }
        });

        // 如果是编辑模式（已存在ID），则禁用类型选择
        if (Objects.nonNull(funcTabPanel) && Objects.nonNull(funcTabPanel.getId())) {
            typeComboBox.setEnabled(false);
        }

        // 初始化时设置结果区选项的可见性
        if (Objects.nonNull(funcTabPanel) && Objects.nonNull(funcTabPanel.getDefine())) {
            updateResultAreaVisibility(funcTabPanel.getDefine().getType());
        } else {
            // 新建对话框时，默认隐藏端口和线程数量输入
            portLabel.setVisible(false);
            portSpinner.setVisible(false);
            threadLabel.setVisible(false);
            threadSpinner.setVisible(false);
        }
    }

    /**
     * 根据类型控制结果区选项、端口输入和线程数量输入的可见性
     *
     * @param type 面板类型
     */
    private void updateResultAreaVisibility(String type) {
        // 定时触发和请求触发类型不需要结果区选项
        boolean isResultAreaNeeded = "default".equals(type);
        // 只有HTTP类型需要端口输入
        boolean isPortNeeded = "http".equals(type);
        // 只有定时触发类型需要线程数量输入
        boolean isThreadNeeded = "cron".equals(type);

        // 找到结果区标签和复选框
        JLabel resultAreaLabel = null;
        for (Component component : formPanel.getComponents()) {
            if (component instanceof JLabel && "结果区".equals(((JLabel) component).getText())) {
                resultAreaLabel = (JLabel) component;
                break;
            }
        }

        // 设置结果区标签和复选框的可见性
        if (resultAreaLabel != null) {
            resultAreaLabel.setVisible(isResultAreaNeeded);
        }
        outTextCheckBox.setVisible(isResultAreaNeeded);

        // 设置端口相关组件的可见性
        portLabel.setVisible(isPortNeeded);
        portSpinner.setVisible(isPortNeeded);

        // 设置线程数量相关组件的可见性
        threadLabel.setVisible(isThreadNeeded);
        threadSpinner.setVisible(isThreadNeeded);
    }

    private void onOK() {
        String nameText = nameTextField.getText().trim();
        if (StrUtil.length(nameText) > 15) {
            Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "名称不能超过15个字符");
            return;
        }

        String selectedDisplayType = (String) typeComboBox.getSelectedItem();
        String selectedType = TYPE_DISPLAY_TO_STORAGE.get(selectedDisplayType);

        // HTTP类型需要验证端口
        if ("http".equals(selectedType)) {
            Integer port = (Integer) portSpinner.getValue();
            if (port < 0 || port > 65535) {
                Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "端口号必须在0-65535之间");
                return;
            }
        }

        String remarkText = remarkTextArea.getText();
        GutoolFuncTabPanel current = funcTabPanel;
        if (Objects.isNull(current) || Objects.isNull(current.getId())) {
            // insert
            current = GutoolDDDFactory.create(new GutoolFuncTabPanel());
        }

        // 对于非按钮触发类型，结果区选项默认为false
        boolean outTextValue = outTextCheckBox.isSelected() && "default".equals(selectedType);
        current.setAll(nameText, remarkText, outTextValue, selectedType);

        // 设置端口（只有HTTP类型才设置端口）
        if ("http".equals(selectedType)) {
            final Integer httpPort = (Integer) portSpinner.getValue();
            if (!Objects.equals(httpPort, current.getDefine().getPort())) {
                current.setHttpPort(httpPort);
            }
        }

        // 设置线程数量（只有定时触发类型才设置线程数量）
        if ("cron".equals(selectedType)) {
            final Integer threadPoolSize = (Integer) threadSpinner.getValue();
            if (!Objects.equals(threadPoolSize, current.getDefine().getThreadPoolSize())) {
                current.setThreadPoolSize(threadPoolSize);
            }
        }

        Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "保存成功");

        saveConsumer.accept(current);
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        cancelConsumer.accept(funcTabPanel);
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
        createUIComponents();
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("取消");
        panel2.add(buttonCancel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveButton = new JButton();
        saveButton.setText("确定");
        panel2.add(saveButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        formPanel = new JPanel();
        formPanel.setLayout(new GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(formPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        formPanel.setBorder(BorderFactory.createTitledBorder(null, "面板属性", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("名称");
        formPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nameTextField = new JTextField();
        formPanel.add(nameTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("描述");
        formPanel.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        formPanel.add(scrollPane1, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        remarkTextArea = new JTextArea();
        remarkTextArea.setRows(4);
        scrollPane1.setViewportView(remarkTextArea);
        final JLabel label3 = new JLabel();
        label3.setText("类型");
        formPanel.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        formPanel.add(typeComboBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        portLabel = new JLabel();
        portLabel.setText("端口");
        formPanel.add(portLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        portSpinner = new JSpinner();
        formPanel.add(portSpinner, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threadLabel = new JLabel();
        threadLabel.setText("线程数量");
        formPanel.add(threadLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threadSpinner = new JSpinner();
        formPanel.add(threadSpinner, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("结果区");
        formPanel.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        outTextCheckBox = new JCheckBox();
        outTextCheckBox.setText("");
        formPanel.add(outTextCheckBox, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        contentPane.add(spacer2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    private void createUIComponents() {
        // 面板类型 - 使用中文描述
        typeComboBox = new JComboBox<>();
        typeComboBox.addItem("按钮触发");
        typeComboBox.addItem("定时触发");
        typeComboBox.addItem("请求触发");

    }
}
