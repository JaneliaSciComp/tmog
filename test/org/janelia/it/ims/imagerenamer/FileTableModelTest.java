/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.imagerenamer.config.ProjectConfiguration;
import org.janelia.it.ims.imagerenamer.config.RenamePattern;
import org.janelia.it.ims.imagerenamer.field.FileModificationTimeModel;
import org.janelia.it.ims.imagerenamer.field.RenameField;
import org.janelia.it.ims.imagerenamer.field.VerifiedNumberModel;
import org.janelia.it.ims.imagerenamer.field.VerifiedTextModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the FileTableModel class.
 *
 * @author Eric Trautman
 */
public class FileTableModelTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public FileTableModelTest(String name) {
        super(name);
    }

    /**
     * Static method to return a suite of all tests.
     * <p/>
     * The JUnit framework uses Java reflection to build a suite of all public
     * methods that have names like "testXXXX()".
     *
     * @return suite of all tests defined in this class.
     */
    public static Test suite() {
        return new TestSuite(FileTableModelTest.class);
    }

    /**
     * Tests the removeSuccessfullyCopiedFiles method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testRemoveSuccessfullyCopiedFiles() throws Exception {

        File savedFile = new File("sortedFileNameB_save");
        File[] files = { new File("sortedFileNameA"),
                         savedFile,
                         new File("sortedFileNameC") };
        ProjectConfiguration config = new ProjectConfiguration();
        FileTableModel model = new FileTableModel(files, config);
        assertEquals("incorrect row count after creation",
                     files.length, model.getRowCount());
        ArrayList<Integer> failedList = new ArrayList<Integer>();
        model.removeSuccessfullyCopiedFiles(failedList);

        assertEquals("all files should be removed when failed list is empty",
                     failedList.size(), model.getRowCount());

        model = new FileTableModel(files, config);
        failedList.add(1);
        model.removeSuccessfullyCopiedFiles(failedList);
        assertEquals("row count should be the same as failed list size",
                     failedList.size(), model.getRowCount());

        File lastFile = (File) model.getValueAt(0, FileTableModel.FILE_COLUMN);
        assertEquals("incorrect file saved",
                     savedFile.getName(), lastFile.getName());                        
    }

    public void testCopyRow() throws Exception {

        // -----------------------
        // setup
        // -----------------------

        File fileA = new File("sortedFileNameA");
        File fileB = new File("sortedFileNameB");
        File[] files = {fileA, fileB};

        RenamePattern renamePattern = new RenamePattern();

        VerifiedTextModel textField = new VerifiedTextModel();
        textField.setMinimumLength(1);
        textField.setMaximumLength(2);
        textField.setPattern("[a-z][0-9]");
        textField.setRequired(true);
        renamePattern.add(textField);

        VerifiedNumberModel numberField = new VerifiedNumberModel();
        numberField.setMinimumValue(0);
        numberField.setMaximumValue(9);
        numberField.setRequired(false);
        renamePattern.add(numberField);

        FileModificationTimeModel fModField = new FileModificationTimeModel();
        fModField.setDatePattern("YYYYmmdd");
        renamePattern.add(fModField);

        ProjectConfiguration config = new ProjectConfiguration();
        config.setRenamePattern(renamePattern);

        FileTableModel model = new FileTableModel(files, config);

        // -----------------------
        // verify setup
        // -----------------------

        String textValue = "a1";
        textField.setText(textValue);
        model.setValueAt(textField, 0, FileTableModel.FILE_COLUMN + 1);

        String numberValue = "5";
        numberField.setText(numberValue);
        model.setValueAt(numberField, 0, FileTableModel.FILE_COLUMN + 2);

        List<FileTableRow> rows = model.getRows();
        assertNotNull("rows are missing from model", rows);
        assertEquals("model has incorrect number of rows",
                     files.length, rows.size());

        FileTableRow row0 = rows.get(0);
        checkFileTableRow(row0, "0", textValue, numberValue, fileA.getName());

        // -----------------------
        // finally, test copy
        // -----------------------

        model.copyRow(0, 1);

        rows = model.getRows();
        row0 = rows.get(0);
        checkFileTableRow(row0, "0", textValue, numberValue, fileA.getName());
        FileTableRow row1 = rows.get(1);
        checkFileTableRow(row1, "1", textValue, numberValue, fileB.getName());                
    }

    private void checkFileTableRow(FileTableRow row,
                                   String rowName,
                                   String expectedTextValue,
                                   String expectedNumberValue,
                                   String expectedFileName) {

        assertNotNull("row " + rowName + " is missing", row);

        RenameField[] row0fields = row.getFields();
        assertNotNull("row " + rowName + " fields are missing", row0fields);
        assertEquals("row " + rowName + " has incorrect number of fields",
                     3, row0fields.length);

        assertEquals("row " + rowName + " text field value is incorrect",
                     expectedTextValue, row0fields[0].getFileNameValue());
        assertEquals("row " + rowName + " number field value is incorrect",
                     expectedNumberValue, row0fields[1].getFileNameValue());

        RenameField row0field2 = row0fields[2];
        if (row0field2 instanceof FileModificationTimeModel) {
            FileModificationTimeModel fileField =
                    (FileModificationTimeModel) row0field2;
            File sourceFile = fileField.getSourceFile();
            assertNotNull("row " + rowName +
                          " file mod field source file is missing",
                          sourceFile);
            assertEquals("row " + rowName +
                         " file mod source file name is incorrect",
                         expectedFileName,
                         sourceFile.getName());

        } else {
            fail("row " + rowName + " file mod field has incorrect type: " +
                    row0field2.getClass().getName());
        }
    }
}
