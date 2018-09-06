/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.field.SourceFileDefaultValue.MatchType;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;

/**
 * Tests the SourceFileDateDefaultValue class.
 *
 * @author Eric Trautman
 */
public class SourceFileDateDefaultValueTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public SourceFileDateDefaultValueTest(String name) {
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
        return new TestSuite(SourceFileDateDefaultValueTest.class);
    }

    /**
     * Tests the getValue method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetValue() throws Exception {

        File file = new File("a1-Gal4-UAS-MCD8-GFP-nc82-GFP-10-20-06-1a0.lsm");
        SourceFileDateDefaultValue defaultValue =
                new SourceFileDateDefaultValue(
                        ".*-(\\d\\d?-\\d\\d?-\\d\\d){1}.*\\.lsm",
                        MatchType.name);
        defaultValue.setFromDatePattern("MM-dd-yy");
        defaultValue.setToDatePattern("MMddyyyy");
        String value = defaultValue.getValue(new FileTarget(file));
        assertEquals("invalid value for " + defaultValue + " and file " +
                     file.getAbsolutePath(),
                     "10202006", value);

        file = new File("a1-Gal4-UAS-MCD8-GFP-nc82-GFP-10-200-06-1a0.lsm");
        value = defaultValue.getValue(new FileTarget(file));
        assertNull("invalid value '" + value + "' for " + defaultValue +
                   " and file " + file.getAbsolutePath(),
                   value);

        file = new File("MZ739-4-Gal4-11-5-07_L12_Sum.lsm");
        value = defaultValue.getValue(new FileTarget(file));
        assertEquals("invalid value for " + defaultValue + " and file " +
                     file.getAbsolutePath(),
                     "11052007", value);
    }
}