/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.util.Set;

/**
 * This class contains the set of common (shared) implementations of
 * {@link TransmogrifierTableModel} methods.
 *
 * @author Eric Trautman
 */
public abstract class AbstractTransmogrifierTableModel
        extends AbstractTableModel
        implements TransmogrifierTableModel {

    private Set<Integer> nestedTableColumns;
    private Integer errorRow;
    private Integer errorColumn;
    private String errorMessage;

    public Set<Integer> getNestedTableColumns() {
        return nestedTableColumns;
    }

    public boolean isNestedTableColumn(int columnIndex) {
        return ((nestedTableColumns != null) &&
                nestedTableColumns.contains(columnIndex));
    }

    /**
     * Sets the nested table column indexes for this table.
     *
     * @param  nestedTableColumns  the indexes of all columns that contain
     *                             nested data tables.
     */
    protected void setNestedTableColumns(Set<Integer> nestedTableColumns) {
        this.nestedTableColumns = nestedTableColumns;
    }

    public boolean isTargetColumn(int columnIndex) {
        return false;
    }

    public Integer getErrorRow() {
        return errorRow;
    }

    public Integer getErrorColumn() {
        return errorColumn;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    protected void setError(Integer row,
                            Integer column,
                            String message) {
        this.errorRow = row;
        this.errorColumn = column;
        this.errorMessage = message;
    }
    
    /**
     * @return a new table model event with the
     *         type {@link #UPDATE_ROW_HEIGHTS}.
     */
    protected TableModelEvent getUpdateEvent() {
        return new TableModelEvent(this,
                                   0,
                                   Integer.MAX_VALUE,
                                   TableModelEvent.ALL_COLUMNS,
                                   UPDATE_ROW_HEIGHTS);
    }

}