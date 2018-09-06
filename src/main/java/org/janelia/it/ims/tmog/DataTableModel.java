/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class contains the data model for renaming a set of files.
 *
 * @author Peter Davies
 * @author Eric Trautman
 */
public class DataTableModel extends AbstractTransmogrifierTableModel {

    public static final int NOT_DISPLAYED = -1;

    /**
     * Index of the column containing the exclude target button.
     * If the column should not be displayed, its value should be
     * {@link #NOT_DISPLAYED}.
     */
    private int excludeColumnIndex;

    /**
     * Index of the column containing the row menu (actions) button,
     * If the column should not be displayed, its value should be
     * {@link #NOT_DISPLAYED}.
     */
    private int rowMenuColumnIndex;

    /**
     * Index of the column containing the copy previous row button,
     * If the column should not be displayed, its value should be
     * {@link #NOT_DISPLAYED}.
     */
    private int copyPreviousColumnIndex;

    /**
     * Index of the column containing the target name,
     * If the column should not be displayed, its value should be
     * {@link #NOT_DISPLAYED}.
     */
    private int targetColumnIndex;

    /**
     * The configuration for the project associated with this model.
     */
    private ProjectConfiguration projectConfiguration;

    private List<DataRow> rows;
    private List<String> columnNames;
    private Map<Integer, Integer> columnToFieldIndexMap;
    private Map<Integer, Integer> fieldToColumnIndexMap;

    /**
     * Constructs a "save defaults dialog" model with fields copied
     * from the specified data row.
     *
     * @param  row                   data row selected for saving defaults.
     * @param  projectConfiguration  configuration for the current project.
     */
    public DataTableModel(DataRow row,
                          ProjectConfiguration projectConfiguration) {

        // disable menu and target columns
        this.excludeColumnIndex = NOT_DISPLAYED;
        this.rowMenuColumnIndex = NOT_DISPLAYED;
        this.copyPreviousColumnIndex = NOT_DISPLAYED;
        this.targetColumnIndex = NOT_DISPLAYED;

        this.projectConfiguration = projectConfiguration;
        this.columnNames = new ArrayList<String>();
        this.columnToFieldIndexMap = new HashMap<Integer, Integer>();
        this.fieldToColumnIndexMap = new HashMap<Integer, Integer>();

        // create new instances so that default edits do not affect real data
        final Target target = row.getTarget();
        DataRow rowInstance = new DataRow(target);
        int columnIndex = 0;
        for (DataField field : row.getFields()) {
            if (field.isEditable()) {
                columnNames.add(field.getDisplayName());
                columnToFieldIndexMap.put(columnIndex, columnIndex);
                fieldToColumnIndexMap.put(columnIndex, columnIndex);
                DataField fieldInstance = field.getNewInstance(true);
                rowInstance.addField(fieldInstance);
                markTableColumnIfNecessary(fieldInstance, columnIndex);
                columnIndex++;
            }
        }

        this.rows = new ArrayList<DataRow>(1);
        this.rows.add(rowInstance);
    }

    /**
     * Constructs a standard data model.
     *
     * @param  targetColumnName      column (header) name for the table's
     *                               target column.
     *
     * @param  targets               list of targets for which data should be
     *                               collected.
     *
     * @param  projectConfiguration  configuration for the current project that
     *                               defines what fields should be collected
     *                               for each target.
     */
    public DataTableModel(String targetColumnName,
                          List<? extends Target> targets,
                          ProjectConfiguration projectConfiguration) {

        this.excludeColumnIndex = 0;
        this.rowMenuColumnIndex = 1;

        if (projectConfiguration.isCopyPreviousButtonVisible()) {
            this.copyPreviousColumnIndex = 2;
            this.targetColumnIndex = 3;
        } else {
            this.copyPreviousColumnIndex = NOT_DISPLAYED;
            this.targetColumnIndex = 2;
        }
        this.projectConfiguration = projectConfiguration;
        List<DataField> dataFieldConfigs =
                projectConfiguration.getFieldConfigurations();

        // set up column names and field mappings
        int numberOfColumns =
                projectConfiguration.getNumberOfVisibleFields() +
                this.targetColumnIndex + 1;
        columnNames = new ArrayList<String>(numberOfColumns);
        columnToFieldIndexMap = new HashMap<Integer, Integer>();
        fieldToColumnIndexMap = new HashMap<Integer, Integer>();

        // empty button headers need single space column names for row height
        for (int i = 0; i < targetColumnIndex; i++) {
            columnNames.add(" ");
        }
        columnNames.add(targetColumnName);

        int fieldIndex = 0;
        for (DataField fieldConfig : dataFieldConfigs) {
            if (fieldConfig.isVisible()) {
                int columnIndex = columnNames.size();
                columnNames.add(fieldConfig.getDisplayName());
                columnToFieldIndexMap.put(columnIndex, fieldIndex);
                fieldToColumnIndexMap.put(fieldIndex, columnIndex);
                markTableColumnIfNecessary(fieldConfig, columnIndex);
            }
            fieldIndex++;
        }

        // create the model rows
        this.rows = new ArrayList<DataRow>(targets.size());
        for (Target target : targets) {
            DataRow dataRow = new DataRow(target, this);
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

    /**
     * @return the index of the target column or {@link #NOT_DISPLAYED}
     *         if the target column should not be displayed for this model.
     */
    public int getTargetColumnIndex() {
        return targetColumnIndex;
    }

    /**
     * @return the configuration for the project associated with this model.
     */
    public ProjectConfiguration getProjectConfiguration() {
        return projectConfiguration;
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
        if ((index < targetColumnIndex)) {
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
        if (columnIndex == targetColumnIndex) {
            isEditable = false;
        } else if (columnIndex > targetColumnIndex) {
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
        if (columnIndex == excludeColumnIndex) {
            if (rows.size() > 1) {
                value = ButtonPanel.ButtonType.EXCLUDE_TARGET;
            }
        } else if (columnIndex == rowMenuColumnIndex) {
            value = ButtonPanel.ButtonType.ROW_MENU;
        } else if (columnIndex == copyPreviousColumnIndex) {
            if (rowIndex > 0) {
                value = ButtonPanel.ButtonType.COPY_PREVIOUS_ROW;
            }
        } else if (columnIndex == targetColumnIndex) {
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
        if (columnIndex > targetColumnIndex) {
            int fieldIndex = columnToFieldIndexMap.get(columnIndex);
            DataField field = (DataField) aValue;
            row.setField(fieldIndex, field);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    /**
     * @return the list of data rows for this model.
     */
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

                    final Target rowTarget = row.getTarget();

                    StringBuilder message = new StringBuilder();
                    message.append("The ");
                    message.append(field.getDisplayName());

                    if (field instanceof DataFieldGroupModel) {
                        DataFieldGroupModel dfgm = (DataFieldGroupModel) field;
                        // column could be null if error field is not visible
                        final Integer errorColumn = dfgm.getErrorColumn();
                        if (errorColumn != null) {
                            Object nestedErrorField =
                                    dfgm.getValueAt(dfgm.getErrorRow(),
                                                    errorColumn);
                            if (nestedErrorField instanceof DataField) {
                                message.append(": ");
                                message.append(((DataField) nestedErrorField).getDisplayName());
                            }
                        }
                    }
                    message.append(" value for ");
                    message.append(rowTarget.getName());
                    message.append(" is invalid.  ");
                    message.append(field.getErrorMessage());
                                                                    
                    setError(rowIndex,
                             getColumnIndexForField(fieldIndex),
                             message.toString());
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

    /**
     * @param  rowIndex  index for the desired row.
     * 
     * @return target for the specified row.
     */
    public Target getTargetForRow(int rowIndex) {
        DataRow row = rows.get(rowIndex);
        return row.getTarget();
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

        if (fromColumnIndex > targetColumnIndex) {
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
        return (columnIndex == targetColumnIndex);
    }

    public boolean isButtonColumn(int columnIndex) {
        return ((columnIndex >= 0) && (columnIndex < targetColumnIndex));
    }

    public boolean isSelectable(int columnIndex) {
        boolean isSelectable = false;
        final int numberOfColumns = getColumnCount();
        if ((columnIndex > targetColumnIndex) &&
            (columnIndex < numberOfColumns)){
            Object value = getValueAt(0, columnIndex);
            if (value instanceof DataField) {
                isSelectable = ((DataField) value).isEditable();
            }   
        }
        return isSelectable;
    }

    /**
     * @return the project name for this model.
     */
    public String getProjectName() {
        return projectConfiguration.getName();
    }

    /**
     * @return the names of the default field sets configured for
     *         this model's project.
     */
    public Set<String> getFieldDefaultSetNames() {
        Set<String> names;
        ProjectPreferences projectPreferences =
                TransmogrifierPreferences.getProjectPreferences(
                        projectConfiguration.getName());
        if (projectPreferences == null) {
            names = new HashSet<String>();
        } else {
            names = new TreeSet<String>(
                    projectPreferences.getFieldDefaultSetNames());
        }
        return names;
    }

    /**
     * @return true if the current user is allowed to edit default field sets
     *         for this model's project; otherwise false.
     */
    public boolean canEditFieldDefaultSets() {
        final TransmogrifierPreferences tmogPreferences =
                TransmogrifierPreferences.getInstance();
        return tmogPreferences.canWrite();
    }

    /**
     * Applies values from the specified default field set to
     * the specified row in this model.
     *
     * @param  defaultSetName  name of the default field set.
     * @param  rowIndex        index of row to update.
     */
    public void applyFieldDefaultSet(String defaultSetName,
                                     int rowIndex) {

        ProjectPreferences projectPreferences =
                TransmogrifierPreferences.getProjectPreferences(
                        projectConfiguration.getName());
        if (projectPreferences != null) {
            final FieldDefaultSet defaultSet =
                    projectPreferences.getFieldDefaultSet(defaultSetName);

            if (defaultSet != null) {
                DataRow row = rows.get(rowIndex);
                for (DataField field : row.getFields()) {
                    field.applyDefault(defaultSet);
                }
                this.fireTableDataChanged();
            }
        }
    }

    /**
     * Saves the values entered for the specified row as a default field
     * set for this model's project.
     *
     * @param  defaultSetName  name of the default field set.
     * @param  rowIndex        index of row to save.
     *
     * @return true if the values were saved successfully; otherwise false.
     */
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
            TransmogrifierPreferences tmogPreferences =
                    TransmogrifierPreferences.getInstance();
            if (tmogPreferences.canWrite()) {
                ProjectPreferences projectPreferences =
                        TransmogrifierPreferences.getProjectPreferences(
                                projectConfiguration.getName());
                projectPreferences.addFieldDefaultSet(defaultSet);
                wasSaveSuccessful = tmogPreferences.save();
            }
        }

        return wasSaveSuccessful;
    }

    /**
     * @param  defaultSetName  name of the default field set.
     *
     * @return true if a set with the specified name is defined for this
     *         model's project; otherwise false.
     */
    public boolean containsDefaultSet(String defaultSetName) {
        ProjectPreferences projectPreferences =
                TransmogrifierPreferences.getProjectPreferences(
                        projectConfiguration.getName());
        return projectPreferences.containsDefaultSet(defaultSetName);
    }

    /**
     * Removes the specified default field set.
     *
     * @param  defaultSetName  name of set to remove.
     *
     * @return true if the set was removed successfully; otherwise false.
     */
    public boolean removeFieldDefaultSet(String defaultSetName) {

        boolean wasRemoveSuccessful = false;

        TransmogrifierPreferences tmogPreferences =
                TransmogrifierPreferences.getInstance();
        if (tmogPreferences.canWrite()) {
            ProjectPreferences projectPreferences =
                    TransmogrifierPreferences.getProjectPreferences(
                            projectConfiguration.getName());
            projectPreferences.removeFieldDefaultSet(defaultSetName);
            wasRemoveSuccessful = tmogPreferences.save();
        }

        return wasRemoveSuccessful;
    }

    /**
     * Removes all rows not in the specified list from this model.
     *
     * @param  failedCopyRowIndices  list of row indicies whose copy failed.
     */
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

    private Integer getColumnIndexForField(int fieldIndex) {
        return fieldToColumnIndexMap.get(fieldIndex);
    }
}
