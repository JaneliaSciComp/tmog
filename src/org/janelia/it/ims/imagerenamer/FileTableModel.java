/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer;

import org.janelia.it.ims.imagerenamer.config.RenameConfiguration;
import org.janelia.it.ims.imagerenamer.field.FileModificationTimeModel;
import org.janelia.it.ims.imagerenamer.field.RenameField;
import org.janelia.it.ims.imagerenamer.field.ValidValueModel;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRowValidator;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRow;
import org.janelia.it.ims.imagerenamer.plugin.ExternalDataException;
import org.janelia.it.ims.imagerenamer.plugin.ExternalSystemException;
import org.apache.log4j.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.awt.Component;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

/**
 * This class contains the data model for renaming a set of files.
 *
 * @author Peter Davies
 * @author Eric Trautman
 */
public class FileTableModel extends AbstractTableModel {

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(FileTableModel.class);

    private RenameConfiguration config;
    private String[] columnNames = {};
    private File[] files;
    private JButton[] copyButtons;
    private RenameField[][] fields;     //Row major order
    private Map<Integer, Integer> columnToFieldIndexMap;
    private Map<Integer, Integer> fieldToColumnIndexMap;

    public FileTableModel(File[] files,
                          RenameConfiguration config) {

        this.files = files;
        Arrays.sort(this.files); // file name order is not guaranteed, so sort
        this.config = config;
        makeFields(files);
    }

    public int getRowCount() {
        return fields.length;
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public String getColumnName(int index) {
        return columnNames[index];
    }

    public Class getColumnClass(int index) {
        Class columnClass = Object.class;
        if (fields != null) {
            Object firstRowObject = getValueAt(0, index);
            if (firstRowObject != null) {
                columnClass = firstRowObject.getClass();
            }
        }
        return columnClass;
    }

    public boolean isCellEditable(int rowIndex,
                                  int columnIndex) {
        return (columnIndex != 1);
    }

    public Object getValueAt(int rowIndex,
                             int columnIndex) {
        Object value;
        if (columnIndex == 0) {
            value = copyButtons[rowIndex];
        } else if (columnIndex == 1) {
            value = files[rowIndex];
        } else {
            int fieldIndex = columnToFieldIndexMap.get(columnIndex);
            value = fields[rowIndex][fieldIndex];
        }
        return value;
    }

    public void setValueAt(Object aValue,
                           int rowIndex,
                           int columnIndex) {
        if (columnIndex > 0) {
            int fieldIndex = columnToFieldIndexMap.get(columnIndex);
            fields[rowIndex][fieldIndex] = (RenameField) aValue;
        }
    }

    public File[] getFiles() {
        return files;
    }

    public RenameField[][] getFields() {
        return fields;
    }

    public String getLongestValue(int columnIndex) {
        final int numRows = this.getRowCount();
        int longestLength = 0;
        String longestValue = "";

        if (columnIndex == 1) {
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
        } else if (columnIndex > 1) {
            Object model = this.getValueAt(0, columnIndex);
            if (model instanceof ValidValueModel) {
                ValidValueModel vvModel = (ValidValueModel) model;
                longestValue = vvModel.getLongestDisplayName();
            }
        }
        return longestValue;
    }
    
    public boolean validateAllFields(JTable fileTable,
                                     Set<RenameFieldRowValidator> externalValidators,
                                     Component dialogParent) {
        boolean isValid = true;

        for (int rowIndex = 0; isValid && (rowIndex < fields.length); rowIndex++) {
            RenameField[] rowFields = fields[rowIndex];

            // validate syntax based on renamer configuration
            for (int fieldIndex = 0; fieldIndex < rowFields.length; fieldIndex++) {
                RenameField field = rowFields[fieldIndex];
                if (! field.verify()) {
                    isValid = false;
                    String message = "The " + field.getDisplayName() +
                            " value for the file " + files[rowIndex].getName() +
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

            // perform any configured external validation
            String externalErrorMsg = null;
            try {
                for (RenameFieldRowValidator validator : externalValidators) {
                    validator.validate(new RenameFieldRow(files[rowIndex],
                                                          rowFields));
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

    public void removeSuccessfullyCopiedFiles(ArrayList<Integer> failedCopyRowIndices) {
        final int numRowsAfterRemove = failedCopyRowIndices.size();
        File[] failedFiles = new File[numRowsAfterRemove];
        JButton[] failedCopyButtons = new JButton[numRowsAfterRemove];
        RenameField[][] failedFields = new RenameField[numRowsAfterRemove][];
        int rowIndex = 0;
        for (Integer failedRowIndex : failedCopyRowIndices) {
            failedFiles[rowIndex] = files[failedRowIndex];
            failedCopyButtons[rowIndex] = copyButtons[failedRowIndex];
            failedFields[rowIndex] = fields[failedRowIndex];
            rowIndex++;
        }
        files = failedFiles;
        copyButtons = failedCopyButtons;
        fields = failedFields;
        this.fireTableDataChanged();
    }

    private void makeFields(File[] files) {

        if (files == null) {
           files = new File[0];
        }
        
        List<RenameField> renameFieldConfigs = config.getFieldConfigurations();
        int numberOfColumns = config.getNumberOfEditableFields() + 2;
        fields = new RenameField[files.length][renameFieldConfigs.size()];
        columnNames = new String[numberOfColumns];
        columnToFieldIndexMap = new HashMap<Integer, Integer>();
        fieldToColumnIndexMap = new HashMap<Integer, Integer>();
        columnNames[0] = " "; // one space needed for header row height
        columnNames[1] = "File Name";
        int columnIndex = 2;
        int fieldIndex = 0;

        for (RenameField fieldConfig : renameFieldConfigs) {
            if (fieldConfig.isEditable()) {
                columnNames[columnIndex] = fieldConfig.getDisplayName();
                columnToFieldIndexMap.put(columnIndex, fieldIndex);
                fieldToColumnIndexMap.put(fieldIndex, columnIndex);
                columnIndex++;
            }
            fieldIndex++;
        }

        URL image = FileTableModel.class.getResource("/copyArrowSimple.png");
        Icon icon = new ImageIcon(image);
        String copyButtonTip = "copy values from previous row";
        copyButtons = new JButton[files.length];

        for (int i = 0; i < files.length; i++) {
            copyButtons[i] = new JButton(icon);
            copyButtons[i].setToolTipText(copyButtonTip);
            fieldIndex = 0;
            for (RenameField renameFieldConfig : renameFieldConfigs) {
                fields[i][fieldIndex] = renameFieldConfig.getNewInstance();
                if (renameFieldConfig instanceof FileModificationTimeModel) {
                    FileModificationTimeModel model =
                            (FileModificationTimeModel) fields[i][fieldIndex];
                    model.setSourceFile(files[i]);
                }
                fieldIndex++;
            }
        }
    }
}
