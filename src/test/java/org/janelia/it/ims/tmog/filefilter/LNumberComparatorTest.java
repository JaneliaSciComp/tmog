/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.filefilter;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;
import java.util.Arrays;

/**
 * Tests the LNumberComparator class.
 *
 * @author Eric Trautman
 */
public class LNumberComparatorTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param name name of the test case.
     */
    public LNumberComparatorTest(String name) {
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
        return new TestSuite(LNumberComparatorTest.class);
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
                { "aaa", "aaa_L1_bbb.lsm", -1 },
                { "aaa_L1_bbb.lsm", "aaa_L1_bbb.lsm", 0 },
                { "aaa_L1_bbb.lsm", "aaa", 1 },
                { "aaa_L1_bbb.lsm", "aaa_L10_bbb.lsm", -1 },
                { "aaa_L10_bbb.lsm", "aaa_L1_bbb.lsm", 1 },
                { "aaa_La_bbb.lsm", "aaa_L1_bbb.lsm", 1 },
                { "aaa_L2_bbb.lsm", "aaa_L10_bbb.lsm", -1 }
        };

        LNumberComparator comparator = new LNumberComparator();

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

    /**
     * Tests the compare method through a full array sort.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testCompareViaSort() throws Exception {

        String correctlySortedNames[] = {
                "0000_aaa",
                "0000_bbb",
                "1111DC_U_022107_L99_Sum.lsm",
                "6325DC_U_022107_L0_Sum.lsm",
                "6325DC_U_022107_L1_Sum.lsm",
                "6325DC_U_022107_L2_Sum.lsm",
                "6325DC_U_022107_L8_Sum.lsm",
                "6325DC_U_022107_L9_Sum.lsm",
                "6325DC_U_022107_L10_Sum.lsm",
                "6325DC_U_022107_L11_Sum.lsm",
                "6325DC_U_022107_L20_Sul.lsm",
                "6325DC_U_022107_L20_Sum.lsm",
                "6325DC_U_022107_L20_Sun.lsm",
                "6325DC_U_022107_L21_Sum.lsm",
                "6325DC_U_022107_L22_Sum.lsm",
                "6325DC_U_022107_L99_Sum.lsm",
                "6325DC_U_022107_L111_Sum.lsm",
                "6325DC_U_022107_L222_Sum.lsm",
                "7777DC_U_022107_L22_Sum.lsm",
                "9999_aaa",
                "9999_bbb"                
        };

        FileTarget[] targets = new FileTarget[correctlySortedNames.length];

        int index = 0;
        for (String name : correctlySortedNames) {
            int reverseOrder = targets.length - 1 - index;
            File file = new File(name);
            targets[reverseOrder] = new FileTarget(file);
            index++;
        }

        Arrays.sort(targets, new LNumberComparator());

        index = 0;
        for (FileTarget target : targets) {
            assertEquals("name " + index + " of " + targets.length +
                         " was not sorted correctly",
                         correctlySortedNames[index], target.getName());
            index++;
        }
    }

}
