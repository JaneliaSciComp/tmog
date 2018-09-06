/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog;

import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;
import java.util.Set;

/**
 * This interface defines the methods supported by all table models.
 *
 * @author Eric Trautman
 */
public interface TransmogrifierTableModel extends TableModel {

    /**
     * Identifies model updates that require table row heights to be
     * recalculated.  The higher number was chosen to ensure that it does
     * not conflict with other events defined in {@link TableModelEvent}. 
     */
    public static final int UPDATE_ROW_HEIGHTS = 99;

    /**
     * Verfies that the contents of all fields in the model are valid.
     * If the field contents are not valid, the {@link #getErrorMessage}
     * method can be called to retrieve detailed error information.
     *
     * @return true if the contents are valid; otherwise false.
     */
    public boolean verify();

    /**
     * @return the index of the row in this model containing the field that
     *         has failed verification or null if no such field has been
     *         identified.
     */
    public Integer getErrorRow();

    /**
     * @return the index of the column in this model containing the field that
     *         has failed verification or null if no such field has been
     *         identified.
     */
    public Integer getErrorColumn();

    /**
     * Returns a detailed error message if the {@link #verify} method has been
     * called and a field in this model is not valid.
     *
     * @return a detailed error message if verification failed; otherwise null.
     */
    public String getErrorMessage();

    /**
     * Adds a row at the specified location.
     *
     * @param  rowIndex  location for add - any existing rows at or after
     *                   this location will be shifted down 1 row.
     */
    public void addRow(int rowIndex);

    /**
     * Removes the row at the specified location.
     *
     * @param  rowIndex  location of row to remove.
     */
    public void removeRow(int rowIndex);

    /**
     * Copies data from all the cells in one row to another.
     *
     * @param  fromRowIndex  source row to copy from.
     * @param  toRowIndex    target row to copy to.
     */
    public void copyRow(int fromRowIndex,
                        int toRowIndex);

    /**
     * Copies data from one cell to all cells below it in the same column.
     *
     * @param  fromRowIndex     row of the source cell.
     * @param  fromColumnIndex  column of the source cell.
     */
    public void fillDown(int fromRowIndex,
                         int fromColumnIndex);

    /**
     * @return the indexes of the columns that use an auto complete editor.
     */
    public Set<Integer> getAutoCompleteColumns();

    /**
     * @return the indexes of the columns that contain nested tables.
     */
    public Set<Integer> getNestedTableColumns();

    /**
     * @param  columnIndex  column to check.
     *
     * @return true if the specified column contains nested tables;
     *         otherwise false.
     */
    public boolean isNestedTableColumn(int columnIndex);

    /**
     * @param  columnIndex  column to check.
     *
     * @return true if the specified column contains the target;
     *         otherwise false.
     */
    public boolean isTargetColumn(int columnIndex);

    /**
     * @param  columnIndex  column to check.
     *
     * @return true if the specified column contains buttons; otherwise false.
     */
    public boolean isButtonColumn(int columnIndex);

    /**
     * @param  columnIndex  column to check.
     *
     * @return true if the specified column can be selected for editing;
     *         otherwise false.
     */
    public boolean isSelectable(int columnIndex);

    /**
     * Forwards the given notification event to all
     * <code>TableModelListeners</code> that registered
     * themselves as listeners for this table model.
     *
     * @param e  the event to be forwarded
     */
    public void fireTableChanged(TableModelEvent e);

}