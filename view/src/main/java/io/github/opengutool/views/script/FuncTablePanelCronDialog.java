package io.github.opengutool.views.script;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.formdev.flatlaf.util.SystemInfo;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import io.github.opengutool.domain.func.GutoolFuncTabPanelDefineCron;
import io.github.opengutool.repository.GutoolPoQueryRepository;
import io.github.opengutool.views.dialog.CronExampleDialog;
import io.github.opengutool.views.util.ComponentUtil;
import io.github.opengutool.views.util.SystemUtil;
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
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 定时任务配置对话框
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/30
 */
public class FuncTablePanelCronDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField cronExpressionField;
    private JButton validateButton;
    private JTextArea descriptionArea;
    private JComboBox<String> scriptComboBox;
    private JCheckBox enabledCheckBox;
    private JLabel nextExecutionValueLabel;
    private JSpinner orderSpinner;
    private JButton exampleButton;

    private final GutoolFuncTabPanelDefineCron cron;
    private final Consumer<GutoolFuncTabPanelDefineCron> saveConsumer;
    private List<Object[]> funcDataList;

    public FuncTablePanelCronDialog(GutoolFuncTabPanelDefineCron cron, Consumer<GutoolFuncTabPanelDefineCron> saveConsumer) {
        this.cron = cron;
        this.saveConsumer = saveConsumer;
        setTitle("定时任务配置");
        // 初始化界面
        $$$setupUI$$$();
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 400, 320);
        setContentPane(contentPane);

        setModal(true);
        setResizable(false);
        if (SystemUtil.isMacOs() && SystemInfo.isMacFullWindowContentSupported) {
            this.getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            this.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            this.getRootPane().putClientProperty("apple.awt.fullscreenable", true);
            this.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
            GridLayoutManager gridLayoutManager = (GridLayoutManager) contentPane.getLayout();
            gridLayoutManager.setMargin(new Insets(28, 10, 0, 10));
        }


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

        // 加载当前定时任务数据
        if (cron != null) {
            cronExpressionField.setText(cron.getCronExpression());
            descriptionArea.setText(cron.getDescription());
            enabledCheckBox.setSelected(cron.getEnabled());
            orderSpinner.setValue(cron.getOrder() != null ? cron.getOrder() : 0);

            // 设置选中的脚本
            if (cron.getCronTriggerFuncId() != null) {
                for (int i = 1; i < scriptComboBox.getItemCount(); i++) {
                    String scriptName = scriptComboBox.getItemAt(i);
                    Object[] funcData = funcDataList.stream()
                            .filter(data -> scriptName.equals(data[1]))
                            .findFirst()
                            .orElse(null);
                    if (funcData != null && cron.getCronTriggerFuncId().equals(funcData[0])) {
                        scriptComboBox.setSelectedIndex(i);
                        break;
                    }
                }
            }
            updateNextExecutionTime();
        }
    }

    private void setupListeners() {
        buttonOK.addActionListener(e -> onSave());
        buttonCancel.addActionListener(e -> onCancel());
        validateButton.addActionListener(e -> validateCronExpression());

        // Cron表达式变化时更新下次执行时间
        cronExpressionField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateNextExecutionTime();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateNextExecutionTime();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateNextExecutionTime();
            }
        });

        // Cron示例按钮事件 - 显示选择对话框
        exampleButton.addActionListener(e -> showCronExampleDialog());

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

    private void validateCronExpression() {
        String expression = cronExpressionField.getText().trim();
        if (StrUtil.isBlank(expression)) {
            Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "请输入Cron表达式");
            return;
        }

        try {
            CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);

            CronParser parser = new CronParser(cronDefinition);
            Cron cron = parser.parse(expression);

            // 验证是否能正常计算执行时间
            ExecutionTime executionTime = ExecutionTime.forCron(cron);
            ZonedDateTime now = ZonedDateTime.now();
            executionTime.nextExecution(now);

            Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.TOP_CENTER, "Cron表达式有效");
            updateNextExecutionTime();
        } catch (Exception e) {
            Notifications.getInstance().show(Notifications.Type.ERROR, Notifications.Location.TOP_CENTER, "Cron表达式无效: " + e.getMessage());
        }
    }

    private void updateNextExecutionTime() {
        String expression = cronExpressionField.getText().trim();
        if (StrUtil.isBlank(expression)) {
            nextExecutionValueLabel.setText("未设置");
            return;
        }

        try {
            CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);

            CronParser parser = new CronParser(cronDefinition);
            Cron cron = parser.parse(expression);

            // 使用 ExecutionTime 来计算下次执行时间
            ExecutionTime executionTime = ExecutionTime.forCron(cron);
            ZonedDateTime now = ZonedDateTime.now();
            Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(now);

            nextExecutionValueLabel.setText(nextExecution.map(zonedDateTime ->
                    LocalDateTimeUtil.format(zonedDateTime.toLocalDateTime(), "yyyy-MM-dd HH:mm:ss")).orElse("无下次执行时间"));
        } catch (Exception e) {
            nextExecutionValueLabel.setText("表达式无效");
        }
    }

    private void onSave() {
        String expression = cronExpressionField.getText().trim();
        if (StrUtil.isBlank(expression)) {
            Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "请输入Cron表达式");
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
        cron.setCronExpression(expression);
        cron.setDescription(descriptionArea.getText());
        cron.setCronTriggerFuncId(scriptId);
        cron.setEnabled(enabledCheckBox.isSelected());
        cron.setOrder((Integer) orderSpinner.getValue());

        saveConsumer.accept(cron);
        dispose();
    }

    private void showCronExampleDialog() {
        CronExampleDialog exampleDialog = new CronExampleDialog(this, expression -> {
            cronExpressionField.setText(expression);
            updateNextExecutionTime();
        });
        exampleDialog.setVisible(true);
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
        panel1.setLayout(new GridLayoutManager(6, 4, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel1.setBorder(BorderFactory.createTitledBorder(null, "定时属性", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("Cron表达式");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cronExpressionField = new JTextField();
        panel1.add(cronExpressionField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        validateButton = new JButton();
        validateButton.setText("验证");
        panel1.add(validateButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("描述");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(1, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(400, 60), null, 0, false));
        descriptionArea = new JTextArea();
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        scrollPane1.setViewportView(descriptionArea);
        final JLabel label3 = new JLabel();
        label3.setText("绑定脚本");
        panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scriptComboBox = new JComboBox();
        panel1.add(scriptComboBox, new GridConstraints(2, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("启用");
        panel1.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        enabledCheckBox = new JCheckBox();
        enabledCheckBox.setText("");
        panel1.add(enabledCheckBox, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("下次执行");
        panel1.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nextExecutionValueLabel = new JLabel();
        nextExecutionValueLabel.setText("未设置");
        panel1.add(nextExecutionValueLabel, new GridConstraints(4, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("排序");
        panel1.add(label6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        orderSpinner = new JSpinner();
        panel1.add(orderSpinner, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        exampleButton = new JButton();
        exampleButton.setText("示例");
        panel1.add(exampleButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("确定");
        panel3.add(buttonOK, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("取消");
        panel3.add(buttonCancel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
