/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.TransmogrifierTableModel;
import org.janelia.it.ims.tmog.view.component.ButtonPanel;
import org.janelia.it.ims.tmog.view.component.ButtonPanel.ButtonType;
import org.janelia.it.ims.tmog.view.component.DataTable;
import org.janelia.it.ims.tmog.view.component.NarrowOptionPane;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class supports the editing of button cells
 * within the file table.
 *
 * @author Eric Trautman
 */
public class ButtonEditor extends AbstractCellEditor
        implements TableCellEditor {

    private Component dialogParent;
    private Map<ButtonType, ButtonPanel> typeToPanelMap;
    private ButtonType buttonType;
    private JPopupMenu menu;

    public ButtonEditor() {
        this.dialogParent = null;
        this.typeToPanelMap = new HashMap<ButtonType, ButtonPanel>();
        for (ButtonType buttonType : ButtonType.values()) {
            this.typeToPanelMap.put(buttonType, new ButtonPanel(buttonType));
        }
    }

    public Object getCellEditorValue() {
        return buttonType;
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {

        Component editorComponent = null;

        if (table instanceof DataTable) {
            dialogParent = ((DataTable) table).getDialogParent();
        } else {
            dialogParent = null;
        }

        if (value instanceof ButtonType) {
            buttonType = (ButtonType) value;
            TableModel tableModel = table.getModel();
            TransmogrifierTableModel model =
                    (TransmogrifierTableModel) tableModel;

            if (ButtonType.EXCLUDE_TARGET.equals(buttonType)) {

                stopCellEditing();
                model.removeRow(row);
                
            } else {

                switch (buttonType) {
                    case FIELD_GROUP_ROW_MENU:
                        this.menu = buildFieldGroupMenu(model, row);
                        break;
                    case ROW_MENU:
                        if (model instanceof DataTableModel) {
                            this.menu = buildRowMenu((DataTableModel) model,
                                                     row);
                        } else {
                            this.menu = new JPopupMenu(buttonType.getToolTip());
                        }
                        break;
                    default:
                        this.menu = new JPopupMenu(buttonType.getToolTip());
                        break;
                }

                editorComponent = typeToPanelMap.get(buttonType);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        showMenu();
                    }
                });
            }
        }

        return editorComponent;
    }

    @Override
    public boolean stopCellEditing() {
        this.buttonType = null;
        this.menu = null;
        return super.stopCellEditing();
    }

    private void showMenu() {
        if (menu != null) {
            ButtonPanel panel = typeToPanelMap.get(buttonType);
            if (panel != null) {
                JButton button = panel.getButton();
                if (button != null) {
                    menu.show(button, button.getX(), button.getY());
                }
            }
        }
        stopCellEditing();
    }

    private JPopupMenu buildRowMenu(DataTableModel model,
                                    int row) {
        JPopupMenu popup = new JPopupMenu("Editing Short Cuts");
        JMenuItem menuItem;
        if (row > 0) {
            menuItem = new JMenuItem(
                    new CopyRowMenuAction("Copy values from previous row",
                                          model,
                                          row,
                                          (row - 1)));
            popup.add(menuItem);
        }

        if (row < (model.getRowCount() - 1)) {
            menuItem = new JMenuItem(
                    new CopyRowMenuAction("Copy values from following row",
                                          model,
                                          row,
                                          (row + 1)));
            popup.add(menuItem);
        }

        Set<String> defaultSetNames = model.getFieldDefaultSetNames();
        if (defaultSetNames.size() > 0) {
            popup.addSeparator();
            for (String defaultSetName : defaultSetNames) {
                menuItem = new JMenuItem(
                        new ApplyRowDefaultMenuAction(defaultSetName,
                                                      model,
                                                      row));
                popup.add(menuItem);
            }
        }

        if (model.canEditFieldDefaultSets()) {
            popup.addSeparator();
            menuItem = new JMenuItem(
                    new SaveRowDefaultMenuAction(model,
                                                 row));
            popup.add(menuItem);

            if (defaultSetNames.size() > 0) {
                menuItem = new JMenuItem(
                        new RemoveExistingDefaultMenuAction(model,
                                                            row));
                popup.add(menuItem);
            }
        }

        return popup;
    }

    private JPopupMenu buildFieldGroupMenu(TransmogrifierTableModel model,
                                           int row) {
        JPopupMenu popup = new JPopupMenu("Field Group Options");
        JMenuItem menuItem;
        if (row > 0) {
            menuItem = new JMenuItem(
                    new CopyRowMenuAction("Copy values from previous row",
                                          model,
                                          row,
                                          (row - 1)));
            popup.add(menuItem);
        }

        if (row < (model.getRowCount() - 2)) {
            menuItem = new JMenuItem(
                    new CopyRowMenuAction("Copy values from following row",
                                          model,
                                          row,
                                          (row + 1)));
            popup.add(menuItem);
        }

        menuItem = new JMenuItem(
                new AddRowMenuAction("Insert a new row before this row",
                                      model,
                                      row));
        popup.add(menuItem);

        menuItem = new JMenuItem(
                new AddRowMenuAction("Insert a new row after this row",
                                      model,
                                      (row + 1)));
        popup.add(menuItem);

        if (model.getRowCount() > 1) {
            menuItem = new JMenuItem(
                    new RemoveRowMenuAction(model,
                                            row));
            popup.add(menuItem);
        }

        return popup;
    }

    private abstract class MenuAction extends AbstractAction {
        protected TransmogrifierTableModel model;
        protected int row;

        public MenuAction(String name,
                          TransmogrifierTableModel model,
                          int row) {
            super(name);
            this.model = model;
            this.row = row;
        }
    }

    private abstract class DataTableMenuAction extends MenuAction {

        public DataTableMenuAction(String name,
                                   DataTableModel model,
                                   int row) {
            super(name, model, row);
        }

        public DataTableModel getModel() {
            return (DataTableModel) model;
        }
    }

    private class CopyRowMenuAction extends MenuAction {
        private int sourceRow;

        public CopyRowMenuAction(String name,
                                 TransmogrifierTableModel model,
                                 int row,
                                 int sourceRow) {
            super(name, model, row);
            this.sourceRow = sourceRow;
        }

        public void actionPerformed(ActionEvent e) {
            model.copyRow(sourceRow, row);
        }
    }

    private class AddRowMenuAction extends MenuAction {

        public AddRowMenuAction(String name,
                                TransmogrifierTableModel model,
                                int row) {
            super(name, model, row);
        }

        public void actionPerformed(ActionEvent e) {
            model.addRow(row);
        }
    }

    private class RemoveRowMenuAction extends MenuAction {

        public RemoveRowMenuAction(TransmogrifierTableModel model,
                                   int row) {
            super("Remove this row", model, row);
        }

        public void actionPerformed(ActionEvent e) {
            model.removeRow(row);
        }
    }

    private class ApplyRowDefaultMenuAction extends DataTableMenuAction {

        private String defaultSetName;

        public ApplyRowDefaultMenuAction(String defaultSetName,
                                         DataTableModel model,
                                         int row) {
            super("Copy '" + defaultSetName + "' default values", model, row);
            this.defaultSetName = defaultSetName;
        }

        public void actionPerformed(ActionEvent e) {
            DataTableModel dataTableModel = getModel();
            dataTableModel.applyFieldDefaultSet(defaultSetName, row);
        }
    }

    private class SaveRowDefaultMenuAction extends DataTableMenuAction {

        public SaveRowDefaultMenuAction(DataTableModel model,
                                        int row) {
            super("Save row values as default set", model, row);
        }

        public void actionPerformed(ActionEvent e) {
            String defaultSetName = JOptionPane.showInputDialog(
                    dialogParent,
                    "Please specify the name for this set of default values:",
                    "Save Default Value Set",
                    JOptionPane.QUESTION_MESSAGE);

            if (defaultSetName != null) {
                DataTableModel dataTableModel = getModel();
                boolean wasSaveSuccessful =
                        dataTableModel.saveRowValuesAsFieldDefaultSet(
                                defaultSetName, row);

                if (wasSaveSuccessful) {
                    NarrowOptionPane.showMessageDialog(
                            dialogParent,
                            "The '" + defaultSetName +
                            "' default set was successfully saved.",
                            "Default Set Saved",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    NarrowOptionPane.showMessageDialog(
                            dialogParent,
                            "The '" + defaultSetName + "' default set was " +
                            "NOT saved for this row.  Please verify that " +
                            "data has been entered for the row and that you " +
                            "have access to save defaults.",
                            "Default Set Not Saved",
                            JOptionPane.ERROR_MESSAGE);
                }
            }

        }
    }

    private class RemoveExistingDefaultMenuAction extends DataTableMenuAction {

        public RemoveExistingDefaultMenuAction(DataTableModel model,
                                               int row) {
            super("Remove existing default set", model, row);
        }

        public void actionPerformed(ActionEvent e) {

            DataTableModel dataTableModel = getModel();
            final Set<String> setNames =
                    dataTableModel.getFieldDefaultSetNames();
            if (setNames.size() > 0) {
                String[] setNamesArray = new String[setNames.size()];
                setNamesArray = setNames.toArray(setNamesArray);

                final Object defaultSetName = JOptionPane.showInputDialog(
                        dialogParent,
                        "Please identify the default set you wish to remove:",
                        "Remove Default Value Set",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        setNamesArray,
                        null);

                if (defaultSetName instanceof String) {
                    boolean wasRemoveSuccessful =
                            dataTableModel.removeFieldDefaultSet(
                                    (String) defaultSetName);
                    if (wasRemoveSuccessful) {
                        NarrowOptionPane.showMessageDialog(
                                dialogParent,
                                "The '" + defaultSetName +
                                "' default set was successfully removed.",
                                "Default Set Removed",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        NarrowOptionPane.showMessageDialog(
                                dialogParent,
                                "The '" + defaultSetName + "' default set " +
                                "was NOT removed.  Please verify that " +
                                "that you have access to edit defaults.",
                                "Default Set Not Removed",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

        }
    }
}
