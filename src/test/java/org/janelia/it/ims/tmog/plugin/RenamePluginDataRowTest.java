/*
 * Copyright (c) 2018 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.field.PluginDataModel;
import org.janelia.it.ims.tmog.field.RunTimeModel;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;

/**
 * Tests the RenamePluginDataRow class.
 *
 * @author Eric Trautman
 */
public class RenamePluginDataRowTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public RenamePluginDataRowTest(String name) {
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
        return new TestSuite(RenamePluginDataRowTest.class);
    }

    /**
     * Tests the getRenamedFile method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetRenamedFile() throws Exception {
        File fromFile = new File("/scope/fromDir/fromFile");
        RunTimeModel fieldOne = new RunTimeModel();
        fieldOne.setDatePattern("HHmmssSSS");
        PluginDataModel fieldTwo = new PluginDataModel();
        String fieldTwoDisplayName = "rank";
        fieldTwo.setDisplayName(fieldTwoDisplayName);
        fieldTwo.setFormat("%02d");
        fieldTwo.setValue(2);
        DataRow dataRow = new DataRow(new FileTarget(fromFile));
        dataRow.addField(fieldOne);
        dataRow.addField(fieldTwo);
        File outputDir = new File("/home/outputDir");
        RenamePluginDataRow row = new RenamePluginDataRow(fromFile, 
                                                          dataRow,
                                                          outputDir);

        File renamedFileBeforeSleep = row.getRenamedFile();
        String fileNameBeforeSleep = renamedFileBeforeSleep.getAbsolutePath();
        Thread.sleep(250);
        File renamedFileAfterSleep = row.getRenamedFile();
        assertEquals("renamedFile name has changed after sleep",
                     fileNameBeforeSleep,
                     renamedFileAfterSleep.getAbsolutePath());

        row.setPluginDataValue(fieldTwoDisplayName, 3);
        File renamedFileAfterSet = row.getRenamedFile();
        String fileNameAfterSet = renamedFileAfterSet.getAbsolutePath();
        assertFalse("renamedFile NOT updated after setPluginDataValue call, " +
                    " name is: " + fileNameAfterSet,
                    fileNameAfterSet.equals(fileNameBeforeSleep));
    }
}
