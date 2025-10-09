package io.github.opengutool.views.script;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.formdev.flatlaf.util.SystemInfo;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import io.github.opengutool.domain.func.GutoolFuncTabPanelDefineButton;
import io.github.opengutool.repository.GutoolPoQueryRepository;
import io.github.opengutool.views.util.ComponentUtil;
import io.github.opengutool.views.util.SystemUtil;
import raven.toast.Notifications;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class FuncTabPanelBtnDialog extends JDialog {
    private JPanel contentPane;
    private JButton saveButton;
    private JButton buttonCancel;
    private JPanel formPanel;
    private JTextField nameTextField;
    private JFormattedTextField orderFormattedTextField;
    private JTextArea remarkTextField;
    private JComboBox<String> funcComboBox;
    private JComboBox<String> funOutModeComboBox;
    private Object[] selecFunc = null;
    private List<Object[]> funcList = new ArrayList<>();

    private Map<Long, Integer> funcIdIndexMap = new HashMap<>();
    private final GutoolFuncTabPanelDefineButton funTabPanelDefineButton;
    private final Consumer<GutoolFuncTabPanelDefineButton> saveConsumer;

    public FuncTabPanelBtnDialog(GutoolFuncTabPanelDefineButton funTabPanelDefineButton, Consumer<GutoolFuncTabPanelDefineButton> saveConsumer) {
        this.funTabPanelDefineButton = funTabPanelDefineButton;
        this.saveConsumer = saveConsumer;
        $$$setupUI$$$();

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 350, 310);
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

        getRootPane().setDefaultButton(saveButton);

        if (Objects.nonNull(funTabPanelDefineButton)) {
            nameTextField.setText(funTabPanelDefineButton.getText());
            remarkTextField.setText(funTabPanelDefineButton.getToolTipText());
            if (CollUtil.isNotEmpty(funcIdIndexMap)) {
                final Integer selectedIndex = funcIdIndexMap.get(funTabPanelDefineButton.getActionTriggerFuncId());
                if (Objects.nonNull(selectedIndex)) {
                    funcComboBox.setSelectedIndex(selectedIndex);
                } else {
                    funcComboBox.setSelectedIndex(0);
                }
            }
            orderFormattedTextField.setValue(funTabPanelDefineButton.getOrder());
        }

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
    }

    private void onOK() {
        String nameText = nameTextField.getText().trim();
        if (StrUtil.isBlankOrUndefined(nameText)) {
            Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "名称不能为空");
            return;
        }
        if (StrUtil.length(nameText) > 5) {
            Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "名称不能超过5个字符");
            return;
        }
        if (Objects.isNull(selecFunc)) {
            Notifications.getInstance().show(Notifications.Type.WARNING, Notifications.Location.TOP_CENTER, "请选择绑定脚本");
            return;
        }
        if (ObjectUtil.isEmpty(funTabPanelDefineButton.getFunOutMode())) {
            funTabPanelDefineButton.setFunOutMode("替换入参");
        }
        funTabPanelDefineButton.setActionTriggerFuncId((Long) selecFunc[0]);
        funTabPanelDefineButton.setText(nameText);
        funTabPanelDefineButton.setToolTipText(remarkTextField.getText());
        funTabPanelDefineButton.setOrder(NumberUtil.parseInt(StrUtil.toString(orderFormattedTextField.getValue()), funTabPanelDefineButton.getOrder()));
        saveConsumer.accept(funTabPanelDefineButton);
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
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
        formPanel.setLayout(new GridLayoutManager(5, 2, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.add(formPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        formPanel.setBorder(BorderFactory.createTitledBorder(null, "按钮属性", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("名称");
        formPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nameTextField = new JTextField();
        formPanel.add(nameTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("描述");
        formPanel.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("排序");
        formPanel.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        formPanel.add(orderFormattedTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        formPanel.add(scrollPane1, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        remarkTextField = new JTextArea();
        remarkTextField.setRows(3);
        scrollPane1.setViewportView(remarkTextField);
        final JLabel label4 = new JLabel();
        label4.setText("绑定脚本");
        formPanel.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        formPanel.add(funcComboBox, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("输出模式");
        formPanel.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        formPanel.add(funOutModeComboBox, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        // TODO: place custom component creation code here
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        orderFormattedTextField = new JFormattedTextField(numberFormat);
        funcComboBox = new JComboBox<>();
        List<Object[]> funcDataObjectList = GutoolPoQueryRepository.selectFuncAllDataObjectList();
        this.funcList = funcDataObjectList;
        for (int i = 0; i < funcDataObjectList.size(); i++) {
            final Object[] funcDataObject = funcDataObjectList.get(i);
            funcComboBox.addItem((String) funcDataObject[1]);
            funcIdIndexMap.put((Long) funcDataObject[0], i);
        }
        funcComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selecFunc = funcDataObjectList.stream()
                        .filter(funcDataObject -> ((String) funcDataObject[1]).equals(funcComboBox.getSelectedItem())).findFirst().orElse(null);
            }
        });
        // funcComboBox.setEditable(true);

        funOutModeComboBox = new JComboBox<>();
        funOutModeComboBox.addItem("替换入参");
        funOutModeComboBox.addItem("输出结果");
        funOutModeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                funTabPanelDefineButton.setFunOutMode((String) funOutModeComboBox.getSelectedItem());
            }
        });
        if ("输出结果".equals(funTabPanelDefineButton.getFunOutMode())) {
            funOutModeComboBox.setSelectedIndex(1);
        } else {
            funOutModeComboBox.setSelectedIndex(0);
        }
    }
}
