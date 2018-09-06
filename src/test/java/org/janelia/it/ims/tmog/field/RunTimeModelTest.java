/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the RunTimeModel class.
 *
 * @author Eric Trautman
 */
public class RunTimeModelTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public RunTimeModelTest(String name) {
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
        return new TestSuite(RunTimeModelTest.class);
    }

    /**
     * Tests the getFileNameValue method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetFileNameValue() throws Exception {
        RunTimeModel model = new RunTimeModel();
        model.setDatePattern("HHmmssSSS");
        String fileNameValue = model.getFileNameValue();
        Thread.sleep(250);
        assertEquals("fileNameValue has changed after sleep",
                     fileNameValue, model.getFileNameValue());
    }

}
