/*
 * Copyright 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view.component;

import org.janelia.it.ims.tmog.TransmogrifierTableModel;

import javax.swing.table.TableCellEditor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * This listener handles keyboard shortcut events for a data table.
 *
 * @author Eric Trautman
 */
public class DataTableKeyListener
        extends KeyAdapter {

    private DataTable dataTable;

    public DataTableKeyListener(DataTable dataTable) {
        this.dataTable = dataTable;
    }

    public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();
            TableCellEditor cellEditor = dataTable.getCellEditor();
            // only check for control keys when a cell is being edited
            if (e.isControlDown() && (cellEditor != null)) {
                if (code == KeyEvent.VK_D) {
                    if (cellEditor.stopCellEditing()) {
                        TransmogrifierTableModel model =
                                (TransmogrifierTableModel) dataTable.getModel();
                        int row = dataTable.getSelectedRow();
                        int column = dataTable.getSelectedColumn();
                        model.fillDown(row, column);
                        dataTable.changeSelection(row, column, false, false);
                    }
                } else if (code == KeyEvent.VK_R) {
                    cellEditor.cancelCellEditing();
                    TransmogrifierTableModel model =
                            (TransmogrifierTableModel) dataTable.getModel();
                    int row = dataTable.getSelectedRow();
                    int column = dataTable.getSelectedColumn();
                    int previousRow = row - 1;
                    if (previousRow >= 0) {
                        model.copyRow(previousRow, row);
                        dataTable.changeSelection(row, column, false, false);
                    }
                }
            }
        }
    }