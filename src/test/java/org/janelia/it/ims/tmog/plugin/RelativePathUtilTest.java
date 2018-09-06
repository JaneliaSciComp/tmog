/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;

/**
 * Tests the RelativePathUtil class.
 *
 * @author Eric Trautman
 */
public class RelativePathUtilTest
        extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public RelativePathUtilTest(String name) {
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
        return new TestSuite(RelativePathUtilTest.class);
    }

    /**
     * Tests the getRelativePath method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetRelativePath() throws Exception {
        String[][] testData = {
                // fullPath,           maxParents, relativePath
                { "/aa/bb/cc.lsm",     "0",        "cc.lsm"},
                { "/aa/bb/cc.lsm",     "1",        "bb/cc.lsm"},
                { "/aa/bb/cc.lsm",     "2",        "aa/bb/cc.lsm"},
                { "/aa/bb/cc.lsm",     "3",        "aa/bb/cc.lsm"},
                { "/aa/bb/cc.lsm",     "4",        "aa/bb/cc.lsm"},
                { "/aa//bb//cc.lsm",   "2",        "aa/bb/cc.lsm"},
                { "/aa//bb/./cc.lsm",  "2",        "aa/bb/cc.lsm"},
                { "/aa//bb/../cc.lsm", "1",        "aa/cc.lsm"},
        };

        File file;
        int maxParents;
        String expectedRelativePath;
        String relativePath;
        for (String[] testCaseData : testData) {
            file = new File(testCaseData[0]);
            maxParents = Integer.parseInt(testCaseData[1]);
            expectedRelativePath = testCaseData[2];
            relativePath = RelativePathUtil.getRelativePath(file, maxParents);
            assertEquals("incorrect relative path returned for '" +
                         file.getAbsolutePath() + "', maxParents=" + maxParents,
                         expectedRelativePath, relativePath);
        }

    }

}