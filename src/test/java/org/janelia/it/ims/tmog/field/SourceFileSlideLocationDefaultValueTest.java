/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.field.SourceFileDefaultValue.MatchType;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;

/**
 * Tests the {@link SourceFileSlideLocationDefaultValue} class.
 *
 * @author Eric Trautman
 */
public class SourceFileSlideLocationDefaultValueTest
        extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public SourceFileSlideLocationDefaultValueTest(String name) {
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
        return new TestSuite(SourceFileSlideLocationDefaultValueTest.class);
    }

    /**
     * Tests the getValue method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetValue() throws Exception {

        final String pattern = "(?:^|.*_)B(\\d+)_.*";
        // default number of columns is 6, so 22 -> D4
        checkDefaultValue("B22_T01_20121029_32_MB381B_20X_R1_L36.lsm",
                          pattern,
                          "D4");

        // default number of columns is 6, so 2 -> A2
        checkDefaultValue("MB077B_51418_20121029_33_B02_T01_20X_R1_L01_20121113171136381.lsm",
                          pattern,
                          "A2");

        final String suffix = "_T1_20121029_32_63X_R1_L032.lsm";
        for (int i = 1; i < 7; i++) {
            checkDefaultValue("B" + i + suffix,
                              pattern,
                              "A" + i);
            checkDefaultValue("B" + (i + 6) + suffix,
                              pattern,
                              "B" + i);
            checkDefaultValue("B" + (i + 12) + suffix,
                              pattern,
                              "C" + i);
        }

        // missing value should be ignored
        checkDefaultValue("foobar",
                          ".*(\\d+).*",
                          null);

        // bad pattern should be ignored
        checkDefaultValue("foobar",
                          "(foo).*",
                          null);
    }

    private void checkDefaultValue(String fileName,
                                   String pattern,
                                   String expectedValue) {

        File file = new File(fileName);
        SourceFileSlideLocationDefaultValue defaultValue =
                new SourceFileSlideLocationDefaultValue(pattern,
                                                        MatchType.name);
        String actualValue = defaultValue.getValue(new FileTarget(file));
        if (expectedValue != null) {
            assertEquals("invalid value for " + defaultValue +
                         " and file " + fileName,
                         expectedValue, actualValue);
        } else {
            assertNull("invalid value '" + actualValue + "' for " +
                       defaultValue + " and file " + fileName, actualValue);
        }
    }
}