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
 * Tests the SourceFileDefaultValue class.
 *
 * @author Eric Trautman
 */
public class SourceFileDefaultValueTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public SourceFileDefaultValueTest(String name) {
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
        return new TestSuite(SourceFileDefaultValueTest.class);
    }

    /**
     * Tests the getValue method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetValue() throws Exception {

        String fileName = "./" +
                          "a1-Gal4-UAS-MCD8-GFP-nc82-GFP-10-20-06.mdb/" +
                          "a1-Gal4-UAS-MCD8-GFP-nc82-GFP-10-20-06-1a0.lsm";

        checkDefaultValue(fileName,
                          "a1.*nc82-GFP-(\\d\\d?-\\d\\d?-\\d\\d?){1}.*\\.lsm",
                          MatchType.name,
                          "10-20-06");

        checkDefaultValue(fileName,
                          ".*\\.(mdb).*",
                          MatchType.name,
                          null);

        checkDefaultValue(fileName,
                          ".*\\.(mdb).*",
                          MatchType.path,
                          "mdb");

        fileName = "./" +
                   "CG9887-Gal4-2-1-CYO-UAS-MCD8-GFP-nc82-GFP-8-22-07.mdb/" +
                   "CG9887-Gal4-2-1-CYO-UAS-MCD8-GFP-nc82-GFP-8-22-07.mdb/" +
                   "CG9887-Gal4-8-22-07_L6_Sum.lsm";

        checkDefaultValue(fileName,
                          ".*mdb[/\\\\].*Gal4-(.*)-UAS.*mdb[/\\\\].*\\.lsm",
                          MatchType.path,
                          "2-1-CYO");

        fileName = ".\\" +
                   "CG9887-Gal4-2-1-CYO-UAS-MCD8-GFP-nc82-GFP-8-22-07.mdb\\" +
                   "CG9887-Gal4-2-1-CYO-UAS-MCD8-GFP-nc82-GFP-8-22-07.mdb\\" +
                   "CG9887-Gal4-8-22-07_L6_Sum.lsm";

        checkDefaultValue(fileName,
                          ".*mdb[/\\\\].*Gal4-(.*)-UAS.*mdb[/\\\\].*\\.lsm",
                          MatchType.path,
                          "2-1-CYO");

        checkDefaultValue(
                "BSBx_line_without_genotype__mp-P02-4_20081028164856563.lsm",
                "BSBx_(.*?)__.*\\.lsm",
                MatchType.name,
                "line_without_genotype");

        checkDefaultValue(
                "BSBx_line_with_genotype__gt_with_bars__mp-P02-4_20081028164856563.lsm",
                "BSBx_(.*?)__.*\\.lsm",
                MatchType.name,
                "line_with_genotype");

        checkDefaultValue(
                "BSBx_line_with_bars__gt_with_bars__mp-P02-4_20081028164856563.lsm",
                "BSBx_.*?__(.*?)__.*\\.lsm",
                MatchType.name,
                "gt_with_bars");

        checkDefaultValue(
                "BSBx_line_with_bars__simplegt__mp-P02-4_20081028164856563.lsm",
                "BSBx_.*__(.).*\\.lsm",
                MatchType.name,
                "m");

        checkDefaultValue(
                "BSBx_line_with_bars__gt_with_bars__ml1-P02-4_20081028164856563.lsm",
                "BSBx_.*__.(.*?)-.*\\.lsm",
                MatchType.name,
                "l1");

        checkDefaultValue(
                "BSBx_line-with-dashes__gt-with-dashes__ml1-P02-4_20081028164856563.lsm",
                "BSBx_.*__.*?-(.*?)-.*\\.lsm",
                MatchType.name,
                "P02");        

        checkDefaultValue(
                "BSBx_line-with-dashes__ml1-P02-4_20081028164856563.lsm",
                "BSBx_.*__.*?-(.*?)-.*\\.lsm",
                MatchType.name,
                "P02");

        checkDefaultValue(
                "BSBx_line-with-dashes__gt-with-dashes__ml1-P02-4_20081028164856563.lsm",
                "BSBx_.*__.*?-.*?-(\\d++)[-_].*\\.lsm",
                MatchType.name,
                "4");

        checkDefaultValue(
                "BSBx_line-with-dashes__ml1-P02-23_20081028164856563.lsm",
                "BSBx_.*__.*?-.*?-(\\d++)[-_].*\\.lsm",
                MatchType.name,
                "23");

        checkDefaultValue(
                "BSBx_line-with-dashes__ml1-P02-12-hp_20081028164856563.lsm",
                "BSBx_.*__.*?-.*?-(\\d++)[-_].*\\.lsm",
                MatchType.name,
                "12");

        checkDefaultValue(
                "BSBx_line-with-dashes__ml1-P02-12-hp_20081028164856563.lsm",
                "BSBx_.*__.*?-.*?-\\d++-h(.)_.*\\.lsm",
                MatchType.name,
                "p");
    }

    private void checkDefaultValue(String fileName,
                                   String pattern,
                                   MatchType matchType,
                                   String expectedValue) {

        File file = new File(fileName);
        SourceFileDefaultValue defaultValue =
                new SourceFileDefaultValue(pattern, matchType);
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