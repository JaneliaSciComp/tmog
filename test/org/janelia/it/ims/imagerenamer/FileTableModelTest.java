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

import java.io.File;
import java.util.ArrayList;

/**
 * Tests the FileTableModel class.
 *
 * @author Rob Svirskas
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

}
