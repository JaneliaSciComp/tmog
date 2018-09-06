/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.AbstractTransmogrifierTableModel;
import org.janelia.it.ims.tmog.TransmogrifierTableModel;
import org.janelia.it.ims.tmog.config.preferences.FieldDefaultSet;
import org.janelia.it.ims.tmog.target.Target;
import org.janelia.it.ims.tmog.view.component.ButtonPanel;

import javax.swing.event.TableModelEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * This model supports a list of field groups with each group containing
 * the same list of field types.
 *
 * @author Eric Trautman
 */
public class DataFieldGroupModel
        extends AbstractTransmogrifierTableModel
        implements DataField {

    private String displayName;
    private Integer minimumRows;
    private Integer maximumRows;
    private Integer displayWidth;
    private List<List<DataField>> fieldRows;
    private TransmogrifierTableModel parent;
    private Boolean isCopyable;

    public DataFieldGroupModel() {
        this.minimumRows = 1;
        this.fieldRows = new ArrayList<List<DataField>>();
        List<DataField> row = new ArrayList<DataField>();
        this.fieldRows.add(row);
        this.isCopyable = null;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @SuppressWarnings("UnusedDeclaration")
    public Integer getMinimumRows() {
        return minimumRows;
    }

    public void setMinimumRows(Integer minimumRows) {
        if (minimumRows == null) {
            this.minimumRows = 1;
        } else if (minimumRows < 1) {
            throw new IllegalArgumentException(
                    "Minimum rows (" + minimumRows +
                    ") must be greater than zero.");
        } else if ((maximumRows != null) && (minimumRows > maximumRows)) {
            throw new IllegalArgumentException(
                    "Minimum rows (" + minimumRows +
                    ") must not be greater than maximum rows (" +
                    maximumRows + ").");
        } else {
            this.minimumRows = minimumRows;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public Integer getMaximumRows() {
        return maximumRows;
    }

    public void setMaximumRows(Integer maximumRows) {
        if ((maximumRows != null) && (maximumRows < minimumRows)) {
            throw new IllegalArgumentException(
                    "Maximum rows (" + maximumRows +
                    ") must not be less than minimum rows (" +
                    minimumRows + ").");
        }
        this.maximumRows = maximumRows;
    }

    public List<List<DataField>> getFieldRows() {
        return fieldRows;
    }

    public void add(DataField field) {
        if (field instanceof DataFieldGroupModel) {
            throw new UnsupportedOperationException(
                    "nested field groups are not currently supported");
        }
        List<DataField> firstRow = getFirstRow();
        firstRow.add(field);
        markTableColumnIfNecessary(field, firstRow.size() - 1);
    }

    public void addRow(int rowIndex) {
        addRow(rowIndex, true);
    }

    public void removeRow(int rowIndex) {
        if (fieldRows.size() > minimumRows) {
            fieldRows.remove(rowIndex);
            this.fireTableDataChanged();
        }
    }

    public void setParent(TransmogrifierTableModel parent) {
        this.parent = parent;
    }

    public Integer getDisplayWidth() {
        return displayWidth;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setDisplayWidth(Integer displayWidth) {
        this.displayWidth = displayWidth;
    }

    public boolean isEditable() {
        return true;
    }

    public boolean isVisible() {
        return true;
    }

    public boolean isCopyable() {
        return isCopyable;
    }

    public boolean isMarkedForTask() {
        return false;
    }

    public DataFieldGroupModel getNewInstance(boolean isCloneRequired) {
        DataFieldGroupModel instance = new DataFieldGroupModel();
        shallowCopyColumns(instance);
        instance.setDisplayName(displayName);
        instance.setMinimumRows(minimumRows);
        instance.setMaximumRows(maximumRows);
        DataField fieldInstance;

        List<DataField> instanceRow;
        int rowIndex = 0;
        for (List<DataField> row : fieldRows) {
            if (rowIndex == 0) {
                instanceRow = instance.getFirstRow(); // empty 1st row created by constructor
            } else {
                instanceRow = new ArrayList<DataField>();
                instance.fieldRows.add(instanceRow);
            }
            for (DataField field : row) {
                fieldInstance = field.getNewInstance(isCloneRequired);
                instanceRow.add(fieldInstance);
            }
            rowIndex++;
        }

        for (int i = instance.getRowCount(); i < minimumRows; i++) {
            instance.addRow(i-1, false);
        }

        // TODO: add support for deeply nested models here if desired later
        //       currently field groups should not contain nested field groups

        if (isCopyable == null) {
            // TODO: consider supporting copying groups with non-copyable fields
            //       need to use parentTable context to grab cell being copied
            boolean allFieldsAllowCopies = true;
            final List<DataField> firstRow = getFirstRow();
            for (DataField field : firstRow) {
                if (! field.isCopyable()) {
                    allFieldsAllowCopies = false;
                    break;
                }
            }
            isCopyable = allFieldsAllowCopies;
        }
        instance.isCopyable = isCopyable;

        return instance;
    }

    public String getCoreValue() {
        throw new UnsupportedOperationException(
                "core value cannot be determined for field groups");
    }

    public String getFileNameValue() {
        throw new UnsupportedOperationException(
                "file name value cannot be determined for field groups");
    }

    public boolean verify() {
        boolean isValid = true;
        setError(null, null, null);

        final int numRows = fieldRows.size();
        int numCols;
        List<DataField> row;
        DataField field;
        for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
            row = fieldRows.get(rowIndex);
            numCols = row.size();
            for (int columnIndex = 0; columnIndex < numCols; columnIndex++) {
                field = row.get(columnIndex);
                if (! field.verify()) {
                    isValid = false;
                    setError(rowIndex, columnIndex, field.getErrorMessage());
                    break;
                }
            }
            if (! isValid) {
                break;
            }
        }

        return isValid;
    }

    public void initializeValue(Target target) {
        for (List<DataField> row : fieldRows) {
            for (DataField field : row) {
                field.initializeValue(target);
            }
        }
    }

    @Override
    public void applyValue(String value) {
        // single string values cannot be applied to this model
    }

    public void applyDefault(FieldDefaultSet defaultSet) {
        final FieldDefaultSet groupSet =
                defaultSet.getFieldDefaultSet(displayName);
        if (groupSet != null) {
            final int lastRowIndex = fieldRows.size() - 1;
            int rowIndex = 0;
            for (FieldDefaultSet rowSet : groupSet.getFieldDefaultSets()) {
                if (rowIndex > lastRowIndex) {
                    addRow(rowIndex, false);
                }
                for (DataField field : fieldRows.get(rowIndex)) {
                    field.applyDefault(rowSet);
                }
                rowIndex++;
            }
        }
    }

    public void addAsDefault(FieldDefaultSet defaultSet) {
        FieldDefaultSet groupSet = new FieldDefaultSet();
        groupSet.setName(displayName);

        int rowIndex = 0;
        for (List<DataField> row : fieldRows) {
            FieldDefaultSet rowSet = new FieldDefaultSet();
            rowSet.setName(String.valueOf(rowIndex));
            for (DataField field : row) {
                field.addAsDefault(rowSet);
            }
            if (rowSet.size() > 0) {
                groupSet.addFieldDefaultSet(rowSet);
            }
            rowIndex++;
        }

        if (groupSet.size() > 0) {
            defaultSet.addFieldDefaultSet(groupSet);
        }
    }

    public int getRowCount() {
        return fieldRows.size();
    }

    public int getColumnCount() {
        final List<DataField> firstRow = getFirstRow();
        int columnCount = firstRow.size();
        if ((maximumRows == null) || (maximumRows > minimumRows)) {
            columnCount = columnCount + NUMBER_OF_BUTTON_COLUMNS;
        }
        return columnCount;
    }

    public Object getValueAt(int rowIndex,
                             int columnIndex) {
        Object value = null;
        List<DataField> row = fieldRows.get(rowIndex);
        final int rowSize = row.size();
        if ((columnIndex > -1) && (columnIndex < rowSize)) {
            value = row.get(columnIndex);
        } else if (columnIndex == rowSize) {
            value = ButtonPanel.ButtonType.FIELD_GROUP_ROW_MENU;
        }
        return value;
    }

    public String getColumnName(int index) {
        String name;
        final List<DataField> firstRow = getFirstRow();
        if ((index >= 0) && (index < firstRow.size())) {
            name = firstRow.get(index).getDisplayName();
        } else {
            name = "";
        }
        return name;
    }

    public Class getColumnClass(int index) {
        Class clazz;
        final List<DataField> firstRow = getFirstRow();
        if (index < firstRow.size()) {
            final DataField columnModel = firstRow.get(index);
            clazz = columnModel.getClass();
        } else {
            clazz = ButtonPanel.ButtonType.class;
        }
        return clazz;
    }

    public boolean isCellEditable(int rowIndex,
                                  int columnIndex) {
        boolean isEditable = true; // button columns must be editable
        List<DataField> row = fieldRows.get(rowIndex);
        if (columnIndex < row.size()) {
            final DataField field = row.get(columnIndex);
            isEditable = field.isEditable();
        }
        return isEditable;
    }

    public void fireTableDataChanged() {
        final TableModelEvent event = getUpdateEvent();
        super.fireTableChanged(event);
        if (parent != null) {
            parent.fireTableChanged(event);
        }
    }
    
    public List<DataField> getFirstRow() {
        return fieldRows.get(0);
    }

    public void copyRow(int fromRowIndex,
                        int toRowIndex) {
        LOG.warn("copy row is not supported within data field groups");
    }

    public void fillDown(int fromRowIndex,
                         int fromColumnIndex) {
        List<DataField> fromRow = fieldRows.get(fromRowIndex);

        if (fromColumnIndex < fromRow.size()) {
            DataField fromField = fromRow.get(fromColumnIndex);
            if (fromField.isCopyable()) {
                final int numberOfRows = fieldRows.size();
                List<DataField> toRow;
                for (int rowIndex = fromRowIndex + 1; rowIndex < numberOfRows;
                     rowIndex++) {
                    toRow = fieldRows.get(rowIndex);
                    toRow.set(fromColumnIndex, fromField.getNewInstance(false));
                }
            }
            this.fireTableDataChanged();
        }
    }

    public boolean isButtonColumn(int columnIndex) {
        final List<DataField> firstRow = getFirstRow();
        final int numberOfDataColumns = firstRow.size();
        final int totalColumns = numberOfDataColumns + NUMBER_OF_BUTTON_COLUMNS;
        return ((columnIndex >= numberOfDataColumns) &&
                (columnIndex < totalColumns));
    }

    public boolean isSelectable(int columnIndex) {
        boolean isSelectable = false;
        final List<DataField> firstRow = getFirstRow();
        final int numberOfDataColumns = firstRow.size();
        if ((columnIndex >= 0) &&
            (columnIndex < numberOfDataColumns)){
            isSelectable = firstRow.get(columnIndex).isEditable();
        }
        return isSelectable;
    }

    private void addRow(int rowIndex,
                        boolean fireEvent) {
        if ((maximumRows == null) || (fieldRows.size() < maximumRows)) {
            List<DataField> firstRow = getFirstRow();
            List<DataField> newRow =
                    new ArrayList<DataField>(firstRow.size());
            DataField newField;
            for (DataField field : firstRow) {
                newField = field.getNewInstance(false);
                newField.initializeValue(null); // clear previous values
                newRow.add(newField);
            }
            fieldRows.add(rowIndex, newRow);
            if (fireEvent) {
                this.fireTableDataChanged();
            }
        }
    }

    /** The logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(DataFieldGroupModel.class);

    private static final int NUMBER_OF_BUTTON_COLUMNS = 1;
}