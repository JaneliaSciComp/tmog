/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.view.component;

import org.janelia.it.ims.imagerenamer.DataTableModel;
import org.janelia.it.ims.imagerenamer.field.ButtonEditor;
import org.janelia.it.ims.imagerenamer.field.ButtonRenderer;
import org.janelia.it.ims.imagerenamer.field.FileRenderer;
import org.janelia.it.ims.imagerenamer.field.ValidValueEditor;
import org.janelia.it.ims.imagerenamer.field.ValidValueModel;
import org.janelia.it.ims.imagerenamer.field.ValidValueRenderer;
import org.janelia.it.ims.imagerenamer.field.VerifiedFieldEditor;
import org.janelia.it.ims.imagerenamer.field.VerifiedFieldModel;
import org.janelia.it.ims.imagerenamer.field.VerifiedFieldRenderer;
import org.janelia.it.ims.imagerenamer.task.TaskProgressInfo;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * This component supports the display of a data table driven by
 * a configurable model.
 *
 * @author Eric Trautman
 */
public class DataTable extends JTable {

    /**
     * Constructs a default table that is initialized with a default
     * data model, a default column model, and a default selection
     * model.
     */
    public DataTable() {
        super();

        setDefaultRenderer(File.class, new FileRenderer());
        setDefaultRenderer(JButton.class, new ButtonRenderer());
        setDefaultRenderer(ValidValueModel.class,
                           new ValidValueRenderer());
        setDefaultRenderer(VerifiedFieldModel.class,
                           new VerifiedFieldRenderer());

        setDefaultEditor(JButton.class, new ButtonEditor());
        setDefaultEditor(ValidValueModel.class,
                         new ValidValueEditor());
        setDefaultEditor(VerifiedFieldModel.class,
                         new VerifiedFieldEditor(this));

        getTableHeader().setReorderingAllowed(false);

        addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                if ((code == KeyEvent.VK_TAB) || code == KeyEvent.VK_RIGHT ||
                    code == KeyEvent.VK_LEFT || code == KeyEvent.VK_UP ||
                    code == KeyEvent.VK_DOWN) {
                    requestFocusForFileTableEditor(
                            getEditorComponent());
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                requestFocusForFileTableEditor(getEditorComponent());
            }
        });
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

        JOptionPane.showMessageDialog(this,
                                      message, // field to display
                                      "Invalid Entry", // title
                                      JOptionPane.ERROR_MESSAGE);

        requestFocus();
        editCellAt(rowIndex, columnIndex);
        Component editor = getEditorComponent();
        if (editor != null) {
            editor.requestFocus();
        }
    }

    /**
     * Updates table row highlighting and (optionally) progress bar components
     * based upon the specified task progress information.
     *
     * @param  info               the latest task progress information.
     * @param  taskProgressBar    progress bar to update (or null).
     * @param  taskProgressLabel  progress label to update (or null).
     * @param  sessionIcon        session icon to update (or null).
     */
    public void updateProgress(TaskProgressInfo info,
                               JProgressBar taskProgressBar,
                               JLabel taskProgressLabel,
                               SessionIcon sessionIcon) {

        int lastRowProcessed = info.getLastRowProcessed();
        changeSelection(lastRowProcessed, 1, false, false);

        if ((sessionIcon != null) && (lastRowProcessed == 0)) {
            // once the first copy has started, change tab icon
            sessionIcon.setToProcessing();
        }
        if (taskProgressBar != null) {
            taskProgressBar.setValue(info.getPercentOfTaskCompleted());
        }
        if (taskProgressLabel != null) {
            taskProgressLabel.setText(info.getMessage());
        }
    }

    private void requestFocusForFileTableEditor(Component editor) {
        if (editor != null) {
            editor.requestFocus();
        } else {
            int row = getSelectedRow();
            changeSelection(row,
                            DataTableModel.getFirstFieldColumn(),
                            false,
                            false);
        }
    }

}
