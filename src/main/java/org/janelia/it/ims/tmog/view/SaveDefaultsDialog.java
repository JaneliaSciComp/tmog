/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.view.component.DataTable;
import org.janelia.it.ims.tmog.view.component.NarrowOptionPane;
import org.janelia.it.utils.StringUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Set;

/**
 * A dialog window for saving default field sets.
 *
 * @author Eric Trautman
 */
public class SaveDefaultsDialog
        extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField defaultSetName;
    private DataTable dataTable;
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel buttonPanel;
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel dataPanel;
    private JComboBox<String> existingSetsComboBox;
    private DataTableModel defaultsModel;
    private ComboBoxModel<String> existingSetsModel;

    /**
     * Displays a modal dialog using the specified model.
     *
     * @param defaultsModel model containing fields to be saved as defaults.
     * @param dialogParent  parent component for the dialog (dialog will be centered within this component).
     */
    public static void showDialog(DataTableModel defaultsModel,
                                  Component dialogParent) {
        SaveDefaultsDialog dialog = new SaveDefaultsDialog(defaultsModel);
        dialog.pack();

        final Dimension parentSize = dialogParent.getSize();
        final int width = parentSize.width - 40;
        final Dimension dialogSize = dialog.getSize();
        if (dialogSize.width < width) {
            dialog.setSize(width, dialogSize.height);
        }

        dialog.setLocationRelativeTo(dialogParent);

        dialog.setVisible(true);
    }

    /**
     * Constructs a dialog that uses the specified model.
     *
     * @param defaultsModel model containing fields to be saved as defaults.
     */
    public SaveDefaultsDialog(DataTableModel defaultsModel) {
        this.defaultsModel = defaultsModel;
        dataTable.setModel(defaultsModel);

        Set<String> setNames = defaultsModel.getFieldDefaultSetNames();
        String[] setNamesArray = new String[setNames.size()];
        setNamesArray = setNames.toArray(setNamesArray);
        existingSetsModel = new DefaultComboBoxModel<String>(setNamesArray);
        existingSetsModel.setSelectedItem(null);
        existingSetsComboBox.setModel(existingSetsModel);
        existingSetsComboBox.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                String nameText = "";
                Object selectedItem = existingSetsModel.getSelectedItem();
                if (selectedItem instanceof String) {
                    nameText = (String) selectedItem;
                }
                defaultSetName.setText(nameText);
            }
        });

        setTitle("Save Default Set");
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onSave();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        onCancel();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onSave() {
        String name = defaultSetName.getText();

        boolean performSave = false;

        if (StringUtil.isDefined(name)) {

            name = name.trim();

            if (defaultsModel.containsDefaultSet(name)) {

                int overrwiteExistingSet =
                        NarrowOptionPane.showConfirmDialog(
                                this,
                                "A '" + name + "' default set already exists " +
                                "for this project.  Do you wish to replace " +
                                "the information saved for this set?",
                                "Default Set Exists",
                                JOptionPane.YES_NO_OPTION);

                performSave = (overrwiteExistingSet == JOptionPane.YES_OPTION);

            } else {

                performSave = true;

            }

        } else {

            NarrowOptionPane.showMessageDialog(
                    contentPane,
                    "Please specify the name for this set of default values.",
                    "Default Set Not Named",
                    JOptionPane.ERROR_MESSAGE);
            defaultSetName.requestFocus();

        }

        if (performSave) {

            boolean wasSaveSuccessful =
                    defaultsModel.saveRowValuesAsFieldDefaultSet(name, 0);

            if (wasSaveSuccessful) {
                NarrowOptionPane.showMessageDialog(
                        contentPane,
                        "The '" + name +
                        "' default set was successfully saved.",
                        "Default Set Saved",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                NarrowOptionPane.showMessageDialog(
                        contentPane,
                        "The '" + name + "' default set was " +
                        "NOT saved for this row.  Please verify that " +
                        "data has been entered for the row and that you " +
                        "have access to save defaults.",
                        "Default Set Not Saved",
                        JOptionPane.ERROR_MESSAGE);
            }

            dispose();
        }
    }

    private void onCancel() {
        dispose();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT edit this method OR call it in your
     * code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(buttonPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                         GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                         GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        buttonPanel.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        buttonPanel.add(panel1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                    GridConstraints.SIZEPOLICY_CAN_GROW,
                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                    GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("Save");
        panel1.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                 GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                 GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel1.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                     GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                     GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dataPanel = new JPanel();
        dataPanel.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(dataPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                       GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                       GridConstraints.SIZEPOLICY_CAN_GROW,
                                                       GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                       GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Default Set Name :");
        dataPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Default Set Values :");
        dataPanel.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        defaultSetName = new JTextField();
        dataPanel.add(defaultSetName, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                          GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                          GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(400, -1), null, 1, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        dataPanel.add(scrollPane1, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                       GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                       GridConstraints.SIZEPOLICY_WANT_GROW,
                                                       GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                       GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 1, false));
        dataTable = new DataTable();
        dataTable.setPreferredScrollableViewportSize(new Dimension(450, 50));
        scrollPane1.setViewportView(dataTable);
        existingSetsComboBox = new JComboBox();
        dataPanel.add(existingSetsComboBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final JLabel label3 = new JLabel();
        label3.setText("Existing Default Sets :");
        dataPanel.add(label3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        dataPanel.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        label1.setLabelFor(defaultSetName);
        label2.setLabelFor(defaultSetName);
    }

    /** @noinspection ALL */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
