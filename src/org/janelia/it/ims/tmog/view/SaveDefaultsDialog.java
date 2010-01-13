/**
 * Copyright 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view;

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
    private JComboBox existingSetsComboBox;
    private DataTableModel defaultsModel;
    private ComboBoxModel existingSetsModel;

    /**
     * Displays a modal dialog using the specified model.
     *
     * @param  defaultsModel  model containing fields to be saved as defaults.
     * @param  dialogParent   parent component for the dialog
     *                        (dialog will be centered within this component).
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
     * @param  defaultsModel  model containing fields to be saved as defaults.
     */
    public SaveDefaultsDialog(DataTableModel defaultsModel) {
        this.defaultsModel = defaultsModel;
        dataTable.setModel(defaultsModel);

        Set<String> setNames = defaultsModel.getFieldDefaultSetNames();
        String[] setNamesArray = new String[setNames.size()];
        setNamesArray = setNames.toArray(setNamesArray);
        existingSetsModel = new DefaultComboBoxModel(setNamesArray);
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
                    getParent(),
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
                        getParent(),
                        "The '" + name +
                        "' default set was successfully saved.",
                        "Default Set Saved",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                NarrowOptionPane.showMessageDialog(
                        getParent(),
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

}
