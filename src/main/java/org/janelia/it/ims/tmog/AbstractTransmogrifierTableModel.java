/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog;

import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.DataFieldGroupModel;
import org.janelia.it.ims.tmog.field.ValidValueModel;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.util.LinkedHashSet;
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

    private Set<Integer> autoCompleteColumns;
    private Set<Integer> nestedTableColumns;
    private Integer errorRow;
    private Integer errorColumn;
    private String errorMessage;

    protected AbstractTransmogrifierTableModel() {
        this.autoCompleteColumns = new LinkedHashSet<Integer>();
        this.nestedTableColumns = new LinkedHashSet<Integer>();
    }

    public Set<Integer> getAutoCompleteColumns() {
        return autoCompleteColumns;
    }

    public Set<Integer> getNestedTableColumns() {
        return nestedTableColumns;
    }

    public boolean isNestedTableColumn(int columnIndex) {
        return nestedTableColumns.contains(columnIndex);
    }

    /**
     * Checks the specified column field and marks it
     * (as nested, auto complete, ...) if special processing needs to
     * be performed on the column later.
     *
     * @param  columnField  field for the column.
     * @param  columnIndex  index of the column.
     */
    protected void markTableColumnIfNecessary(DataField columnField,
                                              int columnIndex) {
        if ((columnField instanceof ValidValueModel) &&
            ((ValidValueModel) columnField).isAutoComplete()) {
            autoCompleteColumns.add(columnIndex);
        }
        if (columnField instanceof DataFieldGroupModel) {
            ((DataFieldGroupModel) columnField).setParent(this);
            nestedTableColumns.add(columnIndex);
        }
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

    /**
     * Shallow copies this instance's special column information to
     * the specified instance.
     *
     * @param  that  new instance to which this instance's column
     *               information should be copied.
     */
    protected void shallowCopyColumns(AbstractTransmogrifierTableModel that) {
        that.autoCompleteColumns = this.autoCompleteColumns;
        that.nestedTableColumns = this.nestedTableColumns;
    }
}