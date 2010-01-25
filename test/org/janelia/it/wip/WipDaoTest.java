/*
 * Copyright 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.wip;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the WipDao class.
 *
 * @author Eric Trautman
 */
public class WipDaoTest
        extends TestCase {

    private WipDao dao;

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public WipDaoTest(String name) {
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
        return new TestSuite(WipDaoTest.class);
    }

    protected void setUp() throws Exception {
        dao = new WipDao("wip");
    }

    /**
     * Tests the getBatchName method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetBatchName() throws Exception {
        String batchNumber = "108";
        String batchColor = "gray";
        String batchName = dao.getBatchName(batchNumber, batchColor);

        assertEquals("invalid name returned for batch number '" +
                     batchNumber + "' and color '" + batchColor + "'",
                     "20100122_gray", batchName);

        batchColor = "bogus-color";
        batchName = dao.getBatchName(batchNumber, batchColor);
        assertNull("name returned for batch number '" + batchNumber +
                   "' and invalid color '" + batchColor + "'",
                   batchName);
    }

}