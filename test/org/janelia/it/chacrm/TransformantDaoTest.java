/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.chacrm;

import static org.janelia.it.chacrm.Transformant.Status;
import org.janelia.it.utils.db.DbManager;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Properties;

/**
 * Tests the TransformantDao class.
 *
 * @author Eric Trautman
 */
public class TransformantDaoTest extends TestCase {

    private static final String EXISTING_TRANSFORMANT_ID = "10A01_AE_01";
    private static final String BAD_TRANSFORMANT_ID = "bogus";
    private static final Integer BAD_FEATURE_ID = -1;
    private static final DbManager EMPTY_DB_MANAGER =
            new DbManager("empty", new Properties());

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public TransformantDaoTest(String name) {
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
        return new TestSuite(TransformantDaoTest.class);
    }

    /**
     * Tests the getTransformant method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetTransformant() throws Exception {
        TransformantDao dao = new TransformantDao();

        getExistingTransformant(dao);

        try {
            Transformant transformant =
                    dao.getTransformant(BAD_TRANSFORMANT_ID);
            fail("inavalid transformant ID should have caused exception " +
                 "but returned this instead: " + transformant);
        } catch (TransformantNotFoundException e) {
            assertTrue(true); // test passed
        }

        dao = new TransformantDao(EMPTY_DB_MANAGER);
        try {
            dao.getTransformant(EXISTING_TRANSFORMANT_ID);
            fail("inavalid dbManager should have caused exception");
        } catch (SystemException e) {
            assertTrue(true); // test passed
        }
    }

    /**
     * Tests the setTransformantStatus method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testSetTransformantStatus() throws Exception {
        TransformantDao dao = new TransformantDao();
        Transformant existingTransformant = getExistingTransformant(dao);
        Status originalStatus = existingTransformant.getStatus();
        Status newStatus = Status.imaged;
        if (originalStatus == newStatus) {
            newStatus = Status.transformant;
        }

        String transformantID = existingTransformant.getTransformantID();
        Integer featureID = existingTransformant.getFeatureID();
        Transformant newTransformant = new Transformant(transformantID,
                                                        newStatus,
                                                        featureID);
        newTransformant.setImageLocation("testDirectory/testImage.lsm");

        Transformant resultTransformant =
                dao.setTransformantStatus(newTransformant);

        assertNotNull("null transformant returned from call",
                      resultTransformant);
        assertEquals("invalid transformant ID returned from call",
                     transformantID,
                     resultTransformant.getTransformantID());
        assertEquals("invalid status returned from call",
                     newTransformant.getStatus(),
                     resultTransformant.getStatus());

        Transformant retrievedTransformant =
                dao.getTransformant(transformantID);
        assertNotNull("null transformant returned from database",
                      retrievedTransformant);
        assertEquals("invalid transformant ID returned from database",
                     transformantID,
                     retrievedTransformant.getTransformantID());
        assertEquals("invalid status returned from database",
                     newTransformant.getStatus(),
                     retrievedTransformant.getStatus());

        try {
            dao.setTransformantStatus(null);
            fail("set with null transformant should have caused exception");
        } catch (IllegalArgumentException e) {
            assertTrue(true); // test passed
        }

        try {
            Transformant badTransformant =
                    new Transformant(BAD_TRANSFORMANT_ID,
                                     Status.imaged,
                                     BAD_FEATURE_ID);
            dao.setTransformantStatus(badTransformant);
            fail("set with bad feature ID should have caused exception");
        } catch (TransformantNotFoundException e) {
            assertTrue(true); // test passed
        }

        dao = new TransformantDao(EMPTY_DB_MANAGER);
        try {
            dao.setTransformantStatus(newTransformant);
            fail("inavalid dbManager should have caused exception");
        } catch (SystemException e) {
            assertTrue(true); // test passed
        }
    }

    private Transformant getExistingTransformant(TransformantDao dao)
            throws TransformantNotFoundException, SystemException {
        Transformant transformant =
                dao.getTransformant(EXISTING_TRANSFORMANT_ID);
        assertNotNull("transformant should exist", transformant);
        assertEquals("invalid transformant ID returned",
                     EXISTING_TRANSFORMANT_ID,
                     transformant.getTransformantID());
        assertNotNull("status not populated", transformant.getStatus());
        return transformant;
    }

}
