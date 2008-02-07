/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer;

import org.apache.log4j.Logger;
import org.janelia.it.ims.imagerenamer.config.ProjectConfiguration;
import org.janelia.it.ims.imagerenamer.config.output.OutputDirectoryConfiguration;
import org.janelia.it.ims.imagerenamer.field.RenameField;
import org.janelia.it.ims.imagerenamer.field.ValidValueModel;
import org.janelia.it.ims.imagerenamer.filefilter.LNumberComparator;
import org.janelia.it.ims.imagerenamer.plugin.ExternalDataException;
import org.janelia.it.ims.imagerenamer.plugin.ExternalSystemException;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRow;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRowValidator;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains the data model for renaming a set of files.
 *
 * @author Peter Davies
 * @author Eric Trautman
 */
public class FileTableModel extends AbstractTableModel {

    /**
     * The logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(FileTableModel.class);

    public static final int FILE_COLUMN = 2;

    private List<FileTableRow> rows;
    private List<String> columnNames;
    private Map<Integer, Integer> columnToFieldIndexMap;
    private Map<Integer, Integer> fieldToColumnIndexMap;
    private ProjectConfiguration projectConfig;

    public FileTableModel(File[] files,
                          ProjectConfiguration config) {

        if (files == null) {
            files = new File[0];
        }
        this.projectConfig = config;

        // sort files based on L-Numbers
        Arrays.sort(files, new LNumberComparator());
        List<RenameField> renameFieldConfigs = config.getFieldConfigurations();

        // set up column names and field mappings
        int numberOfColumns = config.getNumberOfEditableFields() + 3;
        columnNames = new ArrayList<String>(numberOfColumns);
        columnToFieldIndexMap = new HashMap<Integer, Integer>();
        fieldToColumnIndexMap = new HashMap<Integer, Integer>();
        columnNames.add(" "); // checkbox - 1 space needed for header row height
        columnNames.add(" "); // copy button
        columnNames.add("File Name");

        int fieldIndex = 0;
        for (RenameField fieldConfig : renameFieldConfigs) {
            if (fieldConfig.isEditable()) {
                int columnIndex = columnNames.size();
                columnNames.add(fieldConfig.getDisplayName());
                columnToFieldIndexMap.put(columnIndex, fieldIndex);
                fieldToColumnIndexMap.put(fieldIndex, columnIndex);
            }
            fieldIndex++;
        }

        // create the model rows
        this.rows = new ArrayList<FileTableRow>(files.length);
        for (File file : files) {
            this.rows.add(new FileTableRow(file, renameFieldConfigs));
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
        if ((index < FILE_COLUMN)) {
            columnClass = JButton.class;
        } else if (index == FILE_COLUMN) {
            columnClass = File.class;
        } else if (rows.size() > 0) {
            Object firstRowField = getValueAt(0, index);
            columnClass = firstRowField.getClass();
        }
        return columnClass;
    }

    public boolean isCellEditable(int rowIndex,
                                  int columnIndex) {
        return (columnIndex != FILE_COLUMN);
    }

    public Object getValueAt(int rowIndex,
                             int columnIndex) {
        Object value = null;
        FileTableRow row = rows.get(rowIndex);
        if (columnIndex == 0) {
            if (rows.size() > 1) {
                value = row.getRemoveFileButton();
            }
        } else if (columnIndex == 1) {
            if (rowIndex > 0) {
                value = row.getCopyButton();
            }
        } else if (columnIndex == FILE_COLUMN) {
            value = row.getFile();
        } else {
            int fieldIndex = columnToFieldIndexMap.get(columnIndex);
            value = row.getField(fieldIndex);
        }
        return value;
    }

    public void setValueAt(Object aValue,
                           int rowIndex,
                           int columnIndex) {
        FileTableRow row = rows.get(rowIndex);
        if (columnIndex > FILE_COLUMN) {
            int fieldIndex = columnToFieldIndexMap.get(columnIndex);
            RenameField field = (RenameField) aValue;
            row.setField(fieldIndex, field);
        }
    }

    public List<FileTableRow> getRows() {
        return rows;
    }

    public void removeRow(int rowIndex) {
        rows.remove(rowIndex);
        this.fireTableDataChanged();
    }

    public void copyRow(int fromRowIndex,
                        int toRowIndex) {
        FileTableRow fromRow = rows.get(fromRowIndex);
        FileTableRow toRow = rows.get(toRowIndex);
        int fieldCount = toRow.getFieldCount();
        for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
            RenameField fromField = fromRow.getField(fieldIndex);
            if (fromField.isEditable()) {
                toRow.setField(fieldIndex, fromField.getNewInstance());
            }
        }
        this.fireTableDataChanged();
    }

    public String getLongestValue(int columnIndex) {
        final int numRows = this.getRowCount();
        int longestLength = 0;
        String longestValue = "";

        if (columnIndex == FILE_COLUMN) {
            String displayValue;
            int length;
            for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
                Object model = this.getValueAt(rowIndex, columnIndex);
                if (model instanceof File) {
                    File file = (File) model;
                    displayValue = file.getName();
                    length = displayValue.length();
                    if (length > longestLength) {
                        longestLength = length;
                        longestValue = displayValue;
                    }
                }
            }
        } else if (columnIndex > FILE_COLUMN) {
            Object model = this.getValueAt(0, columnIndex);
            if (model instanceof ValidValueModel) {
                ValidValueModel vvModel = (ValidValueModel) model;
                longestValue = vvModel.getLongestDisplayName();
            }
        }
        return longestValue;
    }

    public static int getFirstFieldColumn() {
        return FILE_COLUMN + 1;
    }

    public boolean validateAllFields(JTable fileTable,
                                     List<RenameFieldRowValidator> externalValidators,
                                     Component dialogParent,
                                     File baseOutputDirectory) {
        boolean isValid = true;

        OutputDirectoryConfiguration odCfg = projectConfig.getOutputDirectory();
        boolean isOutputDirectoryAlreadyValidated = odCfg.isDerivedForSession();
        File outputDirectory = null;
        String outputDirectoryPath;
        final int numberOfRows = rows.size();
        for (int rowIndex = 0;
             isValid && (rowIndex < numberOfRows);
             rowIndex++) {

            FileTableRow row = rows.get(rowIndex);
            File rowFile = row.getFile();
            RenameField[] rowFields = row.getFields();

            // validate syntax based on renamer configuration
            for (int fieldIndex = 0; fieldIndex < rowFields.length; fieldIndex++)
            {
                RenameField field = rowFields[fieldIndex];
                if (!field.verify()) {
                    isValid = false;
                    String message = "The " + field.getDisplayName() +
                                     " value for the file " + rowFile.getName() +
                                     " is invalid.  " + field.getErrorMessage();
                    int columnIndex = fieldToColumnIndexMap.get(fieldIndex);
                    displayErrorDialog(dialogParent,
                                       message,
                                       fileTable,
                                       rowIndex,
                                       columnIndex);
                    break;
                }
            }

            // only perform output directory validation if field validation
            if (isValid) {
                if (isOutputDirectoryAlreadyValidated) {
                    outputDirectory = baseOutputDirectory;
                } else {
                    // setup and validate the directories for each file
                    outputDirectoryPath = odCfg.getDerivedPath(rowFile,
                                                               rowFields);
                    outputDirectory = new File(outputDirectoryPath);
                    String outputFailureMsg =
                            OutputDirectoryConfiguration.createAndValidateDirectory(
                                    outputDirectory);
                    if (outputFailureMsg != null) {
                        isValid = false;
                        displayErrorDialog(dialogParent,
                                           outputFailureMsg,
                                           fileTable,
                                           rowIndex,
                                           2);
                    }
                }
            }

            // only perform external validation if internal validation succeeds
            if (isValid) {
                String externalErrorMsg = null;
                try {
                    for (RenameFieldRowValidator validator : externalValidators) {
                        validator.validate(new RenameFieldRow(rowFile,
                                                              rowFields,
                                                              outputDirectory));
                    }
                } catch (ExternalDataException e) {
                    externalErrorMsg = e.getMessage();
                    LOG.info("external validation failed", e);
                } catch (ExternalSystemException e) {
                    externalErrorMsg = e.getMessage();
                    LOG.error(e.getMessage(), e);
                }

                if (externalErrorMsg != null) {
                    isValid = false;
                    displayErrorDialog(dialogParent,
                                       externalErrorMsg,
                                       fileTable,
                                       rowIndex,
                                       2);
                }
            }

        }
        return isValid;
    }

    private void displayErrorDialog(Component dialogParent,
                                    String message,
                                    JTable fileTable,
                                    int rowIndex,
                                    int columnIndex) {

        fileTable.changeSelection(rowIndex, columnIndex, false, false);

        JOptionPane.showMessageDialog(dialogParent,
                                      message, // field to display
                                      "Invalid Entry", // title
                                      JOptionPane.ERROR_MESSAGE);

        fileTable.requestFocus();
        fileTable.editCellAt(rowIndex, columnIndex);
        Component editor = fileTable.getEditorComponent();
        if (editor != null) {
            editor.requestFocus();
        }
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
