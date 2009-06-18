/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog;

import org.janelia.it.ims.tmog.config.ProjectConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.ValidValueModel;
import org.janelia.it.ims.tmog.target.Target;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains the data model for renaming a set of files.
 *
 * @author Peter Davies
 * @author Eric Trautman
 */
public class DataTableModel extends AbstractTableModel {

    public static final int TARGET_COLUMN = 2;

    private List<DataTableRow> rows;
    private List<String> columnNames;
    private Map<Integer, Integer> columnToFieldIndexMap;
    private Map<Integer, Integer> fieldToColumnIndexMap;

    public DataTableModel(String targetColumnName,
                          List<? extends Target> targets,
                          ProjectConfiguration config) {

        List<DataField> dataFieldConfigs = config.getFieldConfigurations();

        // set up column names and field mappings
        int numberOfColumns = config.getNumberOfEditableFields() + 3;
        columnNames = new ArrayList<String>(numberOfColumns);
        columnToFieldIndexMap = new HashMap<Integer, Integer>();
        fieldToColumnIndexMap = new HashMap<Integer, Integer>();
        columnNames.add(" "); // checkbox - 1 space needed for header row height
        columnNames.add(" "); // copy button
        columnNames.add(targetColumnName);

        int fieldIndex = 0;
        for (DataField fieldConfig : dataFieldConfigs) {
            if (fieldConfig.isEditable()) {
                int columnIndex = columnNames.size();
                columnNames.add(fieldConfig.getDisplayName());
                columnToFieldIndexMap.put(columnIndex, fieldIndex);
                fieldToColumnIndexMap.put(fieldIndex, columnIndex);
            }
            fieldIndex++;
        }

        // create the model rows
        this.rows = new ArrayList<DataTableRow>(targets.size());
        for (Target target : targets) {
            this.rows.add(new DataTableRow(target, dataFieldConfigs));
        }
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getColumnCount() {
        return columnNames.size();
    }

    public String getColumnName(int index) {
        return columnNames.get(index);
    }

    public Class getColumnClass(int index) {
        Class columnClass = Object.class;
        if ((index < TARGET_COLUMN)) {
            columnClass = JButton.class;
        } else if (rows.size() > 0) {
            Object firstRowField = getValueAt(0, index);
            columnClass = firstRowField.getClass();
        }
        return columnClass;
    }

    public boolean isCellEditable(int rowIndex,
                                  int columnIndex) {
        return (columnIndex != TARGET_COLUMN);
    }

    public Object getValueAt(int rowIndex,
                             int columnIndex) {
        Object value = null;
        DataTableRow row = rows.get(rowIndex);
        if (columnIndex == 0) {
            if (rows.size() > 1) {
                value = row.getRemoveButton();
            }
        } else if (columnIndex == 1) {
            if (rowIndex > 0) {
                value = row.getCopyButton();
            }
        } else if (columnIndex == TARGET_COLUMN) {
            value = row.getTarget();
        } else {
            int fieldIndex = columnToFieldIndexMap.get(columnIndex);
            value = row.getField(fieldIndex);
        }
        return value;
    }

    public void setValueAt(Object aValue,
                           int rowIndex,
                           int columnIndex) {
        DataTableRow row = rows.get(rowIndex);
        if (columnIndex > TARGET_COLUMN) {
            int fieldIndex = columnToFieldIndexMap.get(columnIndex);
            DataField field = (DataField) aValue;
            row.setField(fieldIndex, field);
        }
    }

    public int getColumnIndexForField(int fieldIndex) {
        return fieldToColumnIndexMap.get(fieldIndex);
    }

    public List<DataTableRow> getRows() {
        return rows;
    }

    public void removeRow(int rowIndex) {
        rows.remove(rowIndex);
        this.fireTableDataChanged();
    }

    public void copyRow(int fromRowIndex,
                        int toRowIndex) {
        DataTableRow fromRow = rows.get(fromRowIndex);
        DataTableRow toRow = rows.get(toRowIndex);
        if ((fromRow != null) && (toRow != null)) {
            int fieldCount = toRow.getFieldCount();
            for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                DataField fromField = fromRow.getField(fieldIndex);
                if (fromField.isCopyable()) {
                    toRow.setField(fieldIndex, fromField.getNewInstance(false));
                }
            }
            this.fireTableDataChanged();
        }
    }

    public void fillDown(int fromRowIndex,
                         int fromColumnIndex) {

        if (fromColumnIndex > TARGET_COLUMN) {
            DataTableRow fromRow = rows.get(fromRowIndex);
            final int fieldIndex = columnToFieldIndexMap.get(fromColumnIndex);
            final DataField fromField = fromRow.getField(fieldIndex);
            if (fromField.isCopyable()) {
                final int numberOfRows = rows.size();

                DataTableRow toRow;
                for (int rowIndex = fromRowIndex + 1; rowIndex < numberOfRows;
                     rowIndex++) {
                    toRow = rows.get(rowIndex);
                    toRow.setField(fieldIndex, fromField.getNewInstance(false));
                }
            }
            this.fireTableDataChanged();
        }
    }

    public String getLongestValue(int columnIndex) {
        final int numRows = this.getRowCount();
        int longestLength = 0;
        String longestValue = "";

        String value;
        int length;
        for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
            Object model = this.getValueAt(rowIndex, columnIndex);
            if (model instanceof ValidValueModel) {
                ValidValueModel vvModel = (ValidValueModel) model;
                longestValue = vvModel.getLongestDisplayName();
                break;
            } else if (model instanceof DataField){
                // default longest length to configured witdh
                if (rowIndex == 0) {
                    Integer width = ((DataField) model).getDisplayWidth();
                    if (width != null) {
                        longestLength = width;
                        longestValue = String.format("%0" + width + "d", 0);
                    }
                }
                value = ((DataField) model).getCoreValue();
            } else if (model instanceof Target) {
                value = ((Target) model).getName();
            } else {
                value = "";
            }

            length = value.length();
            if (length > longestLength) {
                longestLength = length;
                longestValue = value;
            }
        }

        return longestValue;
    }

    public static int getFirstFieldColumn() {
        return TARGET_COLUMN + 1;
    }

    public void removeSuccessfullyCopiedFiles(List<Integer> failedCopyRowIndices) {

        final int numberOfRows = rows.size();
        ArrayList<Integer> rowsToDelete = new ArrayList<Integer>(numberOfRows);
        for (int rowIndex = numberOfRows - 1; rowIndex >= 0; rowIndex--) {
            if (!failedCopyRowIndices.contains(rowIndex)) {
                rowsToDelete.add(rowIndex);
            }
        }

        for (int rowIndex : rowsToDelete) {
            rows.remove(rowIndex);
        }

        this.fireTableDataChanged();
    }
}
