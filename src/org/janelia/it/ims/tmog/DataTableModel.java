/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog;

import org.janelia.it.ims.tmog.config.ProjectConfiguration;
import org.janelia.it.ims.tmog.config.preferences.FieldDefaultSet;
import org.janelia.it.ims.tmog.config.preferences.ProjectPreferences;
import org.janelia.it.ims.tmog.config.preferences.TransmogrifierPreferences;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.DataFieldGroupModel;
import org.janelia.it.ims.tmog.target.Target;
import org.janelia.it.ims.tmog.view.component.ButtonPanel;

import javax.swing.event.TableModelEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class contains the data model for renaming a set of files.
 *
 * @author Peter Davies
 * @author Eric Trautman
 */
public class DataTableModel extends AbstractTransmogrifierTableModel {

    public static final int TARGET_COLUMN = 2;

    private ProjectConfiguration config;
    private List<DataRow> rows;
    private List<String> columnNames;
    private Map<Integer, Integer> columnToFieldIndexMap;
    private Map<Integer, Integer> fieldToColumnIndexMap;

    public DataTableModel(String targetColumnName,
                          List<? extends Target> targets,
                          ProjectConfiguration config) {

        this.config = config;
        List<DataField> dataFieldConfigs = config.getFieldConfigurations();

        // set up column names and field mappings
        int numberOfColumns = config.getNumberOfVisibleFields() + 3;
        columnNames = new ArrayList<String>(numberOfColumns);
        columnToFieldIndexMap = new HashMap<Integer, Integer>();
        fieldToColumnIndexMap = new HashMap<Integer, Integer>();
        columnNames.add(" "); // checkbox - 1 space needed for header row height
        columnNames.add(" "); // copy button
        columnNames.add(targetColumnName);

        Set<Integer> nestedColumns = new LinkedHashSet<Integer>();
        int fieldIndex = 0;
        for (DataField fieldConfig : dataFieldConfigs) {
            if (fieldConfig.isVisible()) {
                int columnIndex = columnNames.size();
                columnNames.add(fieldConfig.getDisplayName());
                columnToFieldIndexMap.put(columnIndex, fieldIndex);
                fieldToColumnIndexMap.put(fieldIndex, columnIndex);
                if (fieldConfig instanceof DataFieldGroupModel) {
                    nestedColumns.add(columnIndex);
                }
            }
            fieldIndex++;
        }
        setNestedTableColumns(nestedColumns);

        // create the model rows
        this.rows = new ArrayList<DataRow>(targets.size());
        for (Target target : targets) {
            DataRow dataRow = new DataRow(target);
            for (DataField dataFieldConfig : dataFieldConfigs) {
                DataField newFieldInstance =
                        dataFieldConfig.getNewInstance(false);
                dataRow.addField(newFieldInstance);
                newFieldInstance.initializeValue(target);
                if (newFieldInstance instanceof DataFieldGroupModel) {
                    ((DataFieldGroupModel) newFieldInstance).setParent(this);
                }
            }
            this.rows.add(dataRow);
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
            columnClass = ButtonPanel.ButtonType.class;
        } else if (rows.size() > 0) {
            Object firstRowField = getValueAt(0, index);
            columnClass = firstRowField.getClass();
        }
        return columnClass;
    }

    public boolean isCellEditable(int rowIndex,
                                  int columnIndex) {
        boolean isEditable = true; // button columns must be editable
        if (columnIndex == TARGET_COLUMN) {
            isEditable = false;
        } else if (columnIndex > TARGET_COLUMN) {
            final DataRow row = rows.get(rowIndex);
            final int fieldIndex = columnToFieldIndexMap.get(columnIndex);
            final DataField field = row.getField(fieldIndex);
            isEditable = field.isEditable();
        }
        return isEditable;
    }

    public void fireTableDataChanged() {
        final TableModelEvent event = getUpdateEvent();
        super.fireTableChanged(event);
    }

    public Object getValueAt(int rowIndex,
                             int columnIndex) {
        Object value = null;
        DataRow row = rows.get(rowIndex);
        if (columnIndex == 0) {
            if (rows.size() > 1) {
                value = ButtonPanel.ButtonType.EXCLUDE_TARGET;
            }
        } else if (columnIndex == 1) {
            value = ButtonPanel.ButtonType.ROW_MENU;
        } else if (columnIndex == TARGET_COLUMN) {
            value = row.getTarget();
        } else {
            Integer fieldIndex = columnToFieldIndexMap.get(columnIndex);
            if (fieldIndex != null) {
                value = row.getField(fieldIndex);
            }
        }
        return value;
    }

    public void setValueAt(Object aValue,
                           int rowIndex,
                           int columnIndex) {
        DataRow row = rows.get(rowIndex);
        if (columnIndex > TARGET_COLUMN) {
            int fieldIndex = columnToFieldIndexMap.get(columnIndex);
            DataField field = (DataField) aValue;
            row.setField(fieldIndex, field);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    public int getColumnIndexForField(int fieldIndex) {
        return fieldToColumnIndexMap.get(fieldIndex);
    }

    public List<DataRow> getRows() {
        return rows;
    }

    public boolean verify() {
        boolean isValid = true;
        setError(null, null, null);

        final int numRows = rows.size();
        int numFields;
        DataRow row;
        DataField field;
        for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
            row = rows.get(rowIndex);
            numFields = row.getFieldCount();
            for (int fieldIndex = 0; fieldIndex < numFields; fieldIndex++) {
                field = row.getField(fieldIndex);
                if (! field.verify()) {
                    isValid = false;

                    final int columnIndex = getColumnIndexForField(fieldIndex);
                    final Target rowTarget = row.getTarget();

                    StringBuilder message = new StringBuilder();
                    message.append("The ");
                    message.append(field.getDisplayName());

                    if (field instanceof DataFieldGroupModel) {
                        DataFieldGroupModel dfgm = (DataFieldGroupModel) field;
                        Object nestedErrorField =
                                dfgm.getValueAt(dfgm.getErrorRow(),
                                                dfgm.getErrorColumn());
                        if (nestedErrorField instanceof DataField) {
                            message.append(": ");
                            message.append(((DataField) nestedErrorField).getDisplayName());
                        }
                    }
                    message.append(" value for ");
                    message.append(rowTarget.getName());
                    message.append(" is invalid.  ");
                    message.append(field.getErrorMessage());
                                                                    
                    setError(rowIndex, columnIndex, message.toString());
                    break;
                }
            }
            if (! isValid) {
                break;
            }
        }

        return isValid;
    }

    public void addRow(int rowIndex) {
        throw new UnsupportedOperationException(
                "dynamic addition of rows not supported for main data table");
    }

    public void removeRow(int rowIndex) {
        rows.remove(rowIndex);
        this.fireTableDataChanged();
    }

    public void copyRow(int fromRowIndex,
                        int toRowIndex) {
        DataRow fromRow = rows.get(fromRowIndex);
        DataRow toRow = rows.get(toRowIndex);
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
            DataRow fromRow = rows.get(fromRowIndex);
            final int fieldIndex = columnToFieldIndexMap.get(fromColumnIndex);
            final DataField fromField = fromRow.getField(fieldIndex);
            if (fromField.isCopyable()) {
                final int numberOfRows = rows.size();

                DataRow toRow;
                for (int rowIndex = fromRowIndex + 1; rowIndex < numberOfRows;
                     rowIndex++) {
                    toRow = rows.get(rowIndex);
                    toRow.setField(fieldIndex, fromField.getNewInstance(false));
                }
            }
            this.fireTableDataChanged();
        }
    }

    @Override
    public boolean isTargetColumn(int columnIndex) {
        return (columnIndex == TARGET_COLUMN);
    }

    public boolean isButtonColumn(int columnIndex) {
        return ((columnIndex >= 0) && (columnIndex < TARGET_COLUMN));
    }

    public boolean isSelectable(int columnIndex) {
        boolean isSelectable = false;
        final int numberOfColumns = getColumnCount();
        if ((columnIndex > TARGET_COLUMN) &&
            (columnIndex < numberOfColumns)){
            Object value = getValueAt(0, columnIndex);
            if (value instanceof DataField) {
                isSelectable = ((DataField) value).isEditable();
            }   
        }
        return isSelectable;
    }

    public Set<String> getFieldDefaultSetNames() {
        final ProjectPreferences prefs = getPreferences();
        return prefs.getFieldDefaultSetNames();
    }

    public boolean canEditFieldDefaultSets() {
        final TransmogrifierPreferences tmogPrefs =
                TransmogrifierPreferences.getInstance();
        return ((tmogPrefs != null) && tmogPrefs.canWrite());
    }

    public void applyFieldDefaultSet(String defaultSetName,
                                     int rowIndex) {

        final ProjectPreferences prefs = getPreferences();
        final FieldDefaultSet defaultSet =
                prefs.getFieldDefaultSet(defaultSetName);

        if (defaultSet != null) {
            DataRow row = rows.get(rowIndex);
            for (DataField field : row.getFields()) {
                field.applyDefault(defaultSet);
            }
        }

        this.fireTableDataChanged();
    }

    public boolean saveRowValuesAsFieldDefaultSet(String defaultSetName,
                                                  int rowIndex) {

        boolean wasSaveSuccessful = false;

        FieldDefaultSet defaultSet = new FieldDefaultSet();
        defaultSet.setName(defaultSetName);

        DataRow row = rows.get(rowIndex);
        for (DataField field : row.getFields()) {
            field.addAsDefault(defaultSet);
        }

        if (defaultSet.size() > 0) {
            TransmogrifierPreferences tmogPrefs =
                    TransmogrifierPreferences.getInstance();
            if (tmogPrefs != null) {
                ProjectPreferences prefs = getPreferences();
                prefs.addFieldDefaultSet(defaultSet);
                tmogPrefs.addProjectPreferences(prefs);
                wasSaveSuccessful = tmogPrefs.save();
            }
        }

        return wasSaveSuccessful;
    }

    public boolean removeFieldDefaultSet(String defaultSetName) {

        boolean wasRemoveSuccessful = false;

        TransmogrifierPreferences tmogPrefs =
                TransmogrifierPreferences.getInstance();
        if (tmogPrefs != null) {
            ProjectPreferences projectPrefs = getPreferences();
            projectPrefs.removeFieldDefaultSet(defaultSetName);
            wasRemoveSuccessful = tmogPrefs.save();
        }

        return wasRemoveSuccessful;
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

    private ProjectPreferences getPreferences() {
        ProjectPreferences projectPrefs = null;

        final String projectName = config.getName();
        final TransmogrifierPreferences tmogPrefs =
                TransmogrifierPreferences.getInstance();
        if (tmogPrefs != null) {
            projectPrefs = tmogPrefs.getPreferences(projectName);
        }

        if (projectPrefs == null) {
            projectPrefs = new ProjectPreferences();
            projectPrefs.setName(projectName);
        }

        return projectPrefs;
    }
}
