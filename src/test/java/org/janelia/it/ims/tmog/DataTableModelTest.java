/*
 * Copyright 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.config.DataFields;
import org.janelia.it.ims.tmog.config.ProjectConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.FileExtensionModel;
import org.janelia.it.ims.tmog.field.FileModificationTimeModel;
import org.janelia.it.ims.tmog.field.VerifiedIntegerModel;
import org.janelia.it.ims.tmog.field.VerifiedTextModel;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.Target;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the DataTableModel class.
 *
 * @author Eric Trautman
 */
public class DataTableModelTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public DataTableModelTest(String name) {
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
        return new TestSuite(DataTableModelTest.class);
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
        List<Target> targets = getFileTargets(files);
        ProjectConfiguration config = new ProjectConfiguration();
        DataTableModel model = new DataTableModel("File Name", targets, config);
        assertEquals("incorrect row count after creation",
                     files.length, model.getRowCount());
        ArrayList<Integer> failedList = new ArrayList<Integer>();
        model.removeSuccessfullyCopiedFiles(failedList);

        assertEquals("all files should be removed when failed list is empty",
                     failedList.size(), model.getRowCount());

        model = new DataTableModel("File Name", targets, config);
        failedList.add(1);
        model.removeSuccessfullyCopiedFiles(failedList);
        assertEquals("row count should be the same as failed list size",
                     failedList.size(), model.getRowCount());

        FileTarget lastFileTarget = (FileTarget)
                model.getValueAt(0, model.getTargetColumnIndex());
        File lastFile = lastFileTarget.getFile();
        assertEquals("incorrect file saved",
                     savedFile.getName(), lastFile.getName());                        
    }

    public void testCopyRow() throws Exception {

        // -----------------------
        // setup
        // -----------------------

        String fileAExtension = ".lsm";
        File fileA = new File("sortedFileNameA" + fileAExtension);
        String fileBExtension = ".tif";
        File fileB = new File("sortedFileNameB" + fileBExtension);
        File[] files = {fileA, fileB};
        List<Target> targets = getFileTargets(files);

        DataFields dataFields = new DataFields();

        VerifiedTextModel textField = new VerifiedTextModel();
        textField.setMinimumLength(1);
        textField.setMaximumLength(2);
        textField.setPattern("[a-z][0-9]");
        textField.setRequired(true);
        dataFields.add(textField);

        VerifiedIntegerModel numberField = new VerifiedIntegerModel();
        numberField.setMinimum(0);
        numberField.setMaximum(9);
        numberField.setRequired(false);
        dataFields.add(numberField);

        FileModificationTimeModel fModField = new FileModificationTimeModel();
        fModField.setDatePattern("YYYYmmdd");
        dataFields.add(fModField);

        FileExtensionModel extensionField = new FileExtensionModel();
        dataFields.add(extensionField);

        ProjectConfiguration config = new ProjectConfiguration();
        config.setDataFields(dataFields);

        DataTableModel model = new DataTableModel("File Name", targets, config);

        // -----------------------
        // verify setup
        // -----------------------

        String textValue = "a1";
        textField.setText(textValue);
        model.setValueAt(textField,
                         0,
                         model.getTargetColumnIndex() + 1);

        String numberValue = "5";
        numberField.setText(numberValue);
        model.setValueAt(numberField,
                         0,
                         model.getTargetColumnIndex() + 2);

        List<DataRow> rows = model.getRows();
        assertNotNull("rows are missing from model", rows);
        assertEquals("model has incorrect number of rows",
                     files.length, rows.size());

        DataRow row0 = rows.get(0);
        checkFileTableRow(row0, "0", textValue, numberValue, fileAExtension);

        // -----------------------
        // finally, test copy
        // -----------------------

        model.copyRow(0, 1);

        rows = model.getRows();
        row0 = rows.get(0);
        checkFileTableRow(row0, "0", textValue, numberValue, fileAExtension);
        DataRow row1 = rows.get(1);
        checkFileTableRow(row1, "1", textValue, numberValue, fileBExtension);
    }

    private void checkFileTableRow(DataRow row,
                                   String rowName,
                                   String expectedTextValue,
                                   String expectedNumberValue,
                                   String expectedExtensionValue) {

        assertNotNull("row " + rowName + " is missing", row);

        List<DataField> row0fields = row.getFields();
        assertNotNull("row " + rowName + " fields are missing", row0fields);
        assertEquals("row " + rowName + " has incorrect number of fields",
                     4, row0fields.size());

        DataField row0field0 = row0fields.get(0);
        assertEquals("row " + rowName + " text field value is incorrect",
                     expectedTextValue, row0field0.getFileNameValue());

        DataField row0field1 = row0fields.get(1);
        assertEquals("row " + rowName + " number field value is incorrect",
                     expectedNumberValue, row0field1.getFileNameValue());

        DataField row0field2 = row0fields.get(2);
        if (row0field2 instanceof FileModificationTimeModel) {
            FileModificationTimeModel fileField =
                    (FileModificationTimeModel) row0field2;
            assertNotNull("row " + rowName +
                          " file modification time is null",
                          fileField.getSourceDate());

        } else {
            fail("row " + rowName +
                 " file modification field has incorrect type: " +
                 row0field2.getClass().getName());
        }

        DataField row0field3 = row0fields.get(3);
        if (row0field3 instanceof FileExtensionModel) {
            FileExtensionModel extField =
                    (FileExtensionModel) row0field3;
            assertEquals("row " + rowName +
                         " file extension is incorrect",
                         expectedExtensionValue,
                         extField.getExtension());

        } else {
            fail("row " + rowName +
                 " file extension field has incorrect type: " +
                 row0field3.getClass().getName());
        }
    }

    private List<Target> getFileTargets(File[] files) {
        ArrayList<Target> targets = new ArrayList<Target>();
        for (File file : files) {
            targets.add(new FileTarget(file));
        }
        return targets;
    }
}
