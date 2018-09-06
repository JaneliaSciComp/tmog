/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.filefilter;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;

/**
 * Tests the NumberComparator class.
 *
 * @author Eric Trautman
 */
public class NumberComparatorTest
        extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param name name of the test case.
     */
    public NumberComparatorTest(String name) {
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
        return new TestSuite(NumberComparatorTest.class);
    }

    /**
     * Tests the compare method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testCompare() throws Exception {

        Object testData[][] = {
                { "aaa", "bbb", -1 },
                { "aaa", "aaa", 0 },
                { "bbb", "aaa", 1 },
                { "aaa", "aaa1.lsm", -1 },
                { "aaa1.lsm", "aaa1.lsm", 0 },
                { "aaa1.lsm", "aaa", 1 },
                { "aaa1.lsm", "aaa10.lsm", -1 },
                { "aaa10.lsm", "aaa1.lsm", 1 },
                { "aaaa.lsm", "aaa1.lsm", 1 },
                { "aaa2.lsm", "aaa10.lsm", -1 }
        };

        NumberComparator comparator = new NumberComparator();
        validateTestData(testData, comparator);

    }

    /**
     * Tests the compare method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testCompareAlternateLNumber() throws Exception {

        Object testData[][] = {
                { "B1_T1_20111103_2_R1_L001.lsm", "B1_T1_20111103_2_R1_L002.lsm", -1 },
                { "B19_T2_20111103_2_R1_L075.lsm", "B1_T1_20111103_2_R1_L002.lsm", 1 },
                { "B19_T2_20111103_2_R1_L075.lsm", "B19_T2_20111103_2_R1_L076.lsm", -1 },
         };

        NumberComparator comparator =
                new NumberComparator(".*_(L)(\\d++)(\\.lsm)");
        validateTestData(testData, comparator);

    }

    /**
     * Tests the compare method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testCompareLNumberWithUncommonGroupIndexes() throws Exception {

        Object testData[][] = {
                { "20140312_26B12DBD_24C08AD_DPX_20X_L01.lsm", "20140312_26B12DBD_24C08AD_DPX_20X_L02.lsm", -1 },
                { "20140312_26B12DBD_24C08AD_DPX_20X_L01.lsm", "20140313_26B12DBD_24C08AD_DPX_63X_L01.lsm", -1 },
                { "20140312_26B12DBD_24C08AD_DPX_20X_L01.lsm", "20140312_00B12DBD_24C08AD_DPX_20X_L01.lsm", 1 }
        };

        NumberComparator comparator =
                new NumberComparator("\\d{8}_(.*AD)_DPX_(\\d\\d)X_L(\\d\\d)\\.lsm",
                                     1,3,2);
        validateTestData(testData, comparator);

    }

    private void validateTestData(Object[][] testData,
                                  NumberComparator comparator) {
        for (Object[] testRow : testData) {
            String name1 = (String) testRow[0];
            String name2 = (String) testRow[1];
            File file1 = new File(name1);
            File file2 = new File(name2);
            int expectedResult = (Integer) testRow[2];
            int actualResult = comparator.compare(new FileTarget(file1),
                                                  new FileTarget(file2));

            boolean isValid = ((expectedResult > 0) && (actualResult > 0)) ||
                    ((expectedResult < 0) && (actualResult < 0)) ||
                    ((expectedResult == 0) && (actualResult == 0));

            assertTrue(name1 + " compared to " + name2 +
                       " returned invalid result of " + actualResult +
                       " (should have same sign as " + expectedResult + ")",
                       isValid);
        }
    }

}
