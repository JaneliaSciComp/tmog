/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.view.component;

import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.field.ButtonEditor;
import org.janelia.it.ims.tmog.field.ButtonRenderer;
import org.janelia.it.ims.tmog.field.TargetRenderer;
import org.janelia.it.ims.tmog.field.ValidValueEditor;
import org.janelia.it.ims.tmog.field.ValidValueModel;
import org.janelia.it.ims.tmog.field.ValidValueRenderer;
import org.janelia.it.ims.tmog.field.VerifiedFieldEditor;
import org.janelia.it.ims.tmog.field.VerifiedFieldModel;
import org.janelia.it.ims.tmog.field.VerifiedFieldRenderer;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.task.TaskProgressInfo;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * This component supports the display of a data table driven by
 * a configurable model.
 *
 * @author Eric Trautman
 */
public class DataTable extends JTable {

    /** {@link javax.swing.plaf.basic.BasicTableUI.Actions} */
    private static final String NEXT_COLUMN_CELL = "selectNextColumnCell";
    private static final String PREVIOUS_COLUMN_CELL =
            "selectPreviousColumnCell";

    /**
     * Constructs a default table that is initialized with a default
     * data model, a default column model, and a default selection
     * model.
     */
    public DataTable() {
        super();

        setDefaultRenderer(FileTarget.class, new TargetRenderer());
        setDefaultRenderer(JButton.class, new ButtonRenderer());
        setDefaultRenderer(ValidValueModel.class,
                           new ValidValueRenderer());
        setDefaultRenderer(VerifiedFieldModel.class,
                           new VerifiedFieldRenderer());

        setDefaultEditor(JButton.class, new ButtonEditor());
        setDefaultEditor(ValidValueModel.class,
                         new ValidValueEditor(this));
        setDefaultEditor(VerifiedFieldModel.class,
                         new VerifiedFieldEditor(this));

        getTableHeader().setReorderingAllowed(false);

        // TODO: is there a better way to handle keyboard navigation focus issues by using setSurrendersFocusOnKeystroke?

        ActionMap actionMap = getActionMap();
        final Action nextColumnCellAction = actionMap.get(NEXT_COLUMN_CELL);
        Action wrappedNextColumnCellAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                nextColumnCellAction.actionPerformed(e);
                if (getSelectedColumn() == 0) {
                    changeSelection(getSelectedRow(),
                                    DataTableModel.getFirstFieldColumn(),
                                    false,
                                    false);
                }
            }
        };
        actionMap.put(NEXT_COLUMN_CELL, wrappedNextColumnCellAction);

        final Action previousColumnCellAction =
                actionMap.get(PREVIOUS_COLUMN_CELL);
        Action wrappedPreviousColumnCellAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                previousColumnCellAction.actionPerformed(e);
                if (getSelectedColumn() < DataTableModel.getFirstFieldColumn()) {
                    int selectedRow = getSelectedRow();
                    if (selectedRow > 0) {
                        TableModel model = getModel();
                        changeSelection(selectedRow - 1,
                                        model.getColumnCount() - 1,
                                        false,
                                        false);
                    } else {
                        changeSelection(selectedRow,
                                        DataTableModel.getFirstFieldColumn(),
                                        false,
                                        false);
                    }
                }
            }
        };
        actionMap.put(PREVIOUS_COLUMN_CELL, wrappedPreviousColumnCellAction);

        addKeyListener(getKeyListener());
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks,
                                        KeyEvent e,
                                        int condition,
                                        boolean pressed) {

        Component editor = this.getEditorComponent();
        if (editor instanceof JComboBox) {
            int keyCode = e.getKeyCode();
            if (! pressed) {
                if ((keyCode == KeyEvent.VK_TAB) ||
                    (keyCode == KeyEvent.VK_LEFT) ||
                    (keyCode == KeyEvent.VK_RIGHT) ||
                    (keyCode == KeyEvent.VK_UP) ||
                    (keyCode == KeyEvent.VK_DOWN)) {
                    JComboBox comboBox = (JComboBox) editor;

                    if (! comboBox.isPopupVisible()) {
                        comboBox.requestFocusInWindow();
                        return false;
                    }
                }
            }
        }

        return super.processKeyBinding(ks, e, condition, pressed);
    }

    public void editAndRequestFocusForSelectedCell() {
        int row = getSelectedRow();
        int column = getSelectedColumn();
        editCellAt(row, column);
        Component editor = getEditorComponent();
        if (editor != null) {
            editor.requestFocusInWindow();
        }
    }

    /**
     * Resizes this table's column widths based upon the table's data model.
     */
    public void sizeTable() {
        TableModel model = getModel();
        if (model instanceof DataTableModel) {
            DataTableModel dataModel = (DataTableModel) model;

            final int numColumns = dataModel.getColumnCount();
            if (numColumns > 0) {
                final int cellMargin = 5;
                TableColumnModel colModel = getColumnModel();
                colModel.setColumnMargin(cellMargin);
                TableColumn tableColumn;
                // strictly size the button columns
                final int copyButtonHeight = 20;
                final int copyButtonWidth = 20;
                for (int columnIndex = 0;
                     columnIndex < DataTableModel.TARGET_COLUMN;
                     columnIndex++) {

                    tableColumn = colModel.getColumn(columnIndex);
                    int preferredWidth = copyButtonWidth + (2 * cellMargin);
                    tableColumn.setMinWidth(preferredWidth);
                    tableColumn.setMaxWidth(preferredWidth);
                    tableColumn.setPreferredWidth(preferredWidth);
                }

                // set preferred sizes for all other columns
                for (int columnIndex = DataTableModel.TARGET_COLUMN;
                     columnIndex < numColumns;
                     columnIndex++) {

                    tableColumn = colModel.getColumn(columnIndex);
                    String longestVal = dataModel.getLongestValue(columnIndex);
                    if (longestVal.length() > 0) {
                        JLabel text = new JLabel(longestVal);
                        text.setFont(getFont());
                        Dimension dimension = text.getPreferredSize();
                        int preferredWidth = dimension.width + (2 * cellMargin);
                        tableColumn.setPreferredWidth(preferredWidth);
                    }
                }

                int preferredHeight = copyButtonHeight + (2 * cellMargin);
                setRowHeight(preferredHeight);
                setRowMargin(cellMargin);
            }
        }
    }

    /**
     * Displays the specified message in a dialog and requests focus
     * for the associated error cell.
     *
     * @param  message      error message to display.
     * @param  rowIndex     index of the row that contains the error cell.
     * @param  columnIndex  index of the column that contains the error cell.
     */
    public void displayErrorDialog(String message,
                                   int rowIndex,
                                   int columnIndex) {

        changeSelection(rowIndex, columnIndex, false, false);

        NarrowOptionPane.showMessageDialog(this,
                                           message, // field to display
                                           "Invalid Entry", // title
                                           JOptionPane.ERROR_MESSAGE);

        editAndRequestFocusForSelectedCell();
    }

    /**
     * Updates table row highlighting and (optionally) progress bar components
     * based upon the specified task progress information.
     *
     * @param  info               the latest task progress information.
     */
    public void updateProgress(TaskProgressInfo info) {
        changeSelection(info.getLastRowProcessed(), 1, false, false);
    }

    /**
     * @return a listener that will handle shortcut key events
     *         for editing this table's cells.
     */
    public KeyListener getKeyListener() {
        return new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    int code = e.getKeyCode();
                    // only check for control keys when a cell is being edited
                    if (e.isControlDown() && (cellEditor != null)) {
                        if (code == KeyEvent.VK_D) {
                            if (cellEditor.stopCellEditing()) {
                                DataTableModel model =
                                        (DataTableModel) getModel();
                                int row = getSelectedRow();
                                int column = getSelectedColumn();
                                model.fillDown(row, column);
                                changeSelection(row, column, false, false);
                            }
                        } else if (code == KeyEvent.VK_R) {
                            cellEditor.cancelCellEditing();
                            DataTableModel model =
                                    (DataTableModel) getModel();
                            int row = getSelectedRow();
                            int column = getSelectedColumn();
                            int previousRow = row - 1;
                            if (previousRow >= 0) {
                                model.copyRow(previousRow, row);
                                changeSelection(row, column, false, false);
                            }
                        }
                    }
                }
            };
    }
}
