/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.chacrm;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import static org.janelia.it.chacrm.Transformant.Status;
import org.janelia.it.utils.db.DbManager;

import java.util.Properties;

/**
 * Tests the TransformantDao class.
 *
 * @author Eric Trautman
 */
public class TransformantDaoTest extends TestCase {

    private static final String EXISTING_TRANSFORMANT_ID = "57A02_AD_01";
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

        // test retrieval without rank
        getExistingTransformant(dao, false);

        // test retrieval with rank
        getExistingTransformant(dao, true);

        try {
            Transformant transformant =
                    dao.getTransformant(BAD_TRANSFORMANT_ID, false);
            fail("inavalid transformant ID should have caused exception " +
                 "but returned this instead: " + transformant);
        } catch (TransformantNotFoundException e) {
            assertTrue(true); // test passed
        }

        dao = new TransformantDao(EMPTY_DB_MANAGER);
        try {
            dao.getTransformant(EXISTING_TRANSFORMANT_ID, false);
            fail("inavalid dbManager should have caused exception");
        } catch (SystemException e) {
            assertTrue(true); // test passed
        }
    }

    /**
     * Tests the setTransformantStatusAndLocation method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testSetTransformantStatusAndLocation() throws Exception {
        TransformantDao dao = new TransformantDao();
        Transformant existingTransformant =
                getExistingTransformant(dao, true);

        String transformantID = existingTransformant.getTransformantID();
        Integer featureID = existingTransformant.getFeatureID();
        Transformant newTransformant = new Transformant(transformantID,
                                                        Status.imaged,
                                                        featureID);
        ImageLocation existingImageLocation =
                existingTransformant.getImageLocation();
        ImageLocation newImageLocation =
                new ImageLocation("testDirectory/testImage.lsm",
                                  existingImageLocation.getRank());
        newTransformant.setImageLocation(newImageLocation);

        dao.setTransformantStatusAndLocation(newTransformant);

        Transformant retrievedTransformant =
                dao.getTransformant(transformantID, false);
        assertNotNull("null transformant returned from database",
                      retrievedTransformant);
        assertEquals("invalid transformant ID returned from database",
                     transformantID,
                     retrievedTransformant.getTransformantID());
        assertEquals("invalid status returned from database",
                     Status.imaged,
                     retrievedTransformant.getStatus());

        try {
            dao.setTransformantStatusAndLocation(null);
            fail("set with null transformant should have caused exception");
        } catch (IllegalArgumentException e) {
            assertTrue(true); // test passed
        }

        try {
            Transformant badTransformant =
                    new Transformant(BAD_TRANSFORMANT_ID, null, null);
            badTransformant.setImageLocation(null);
            dao.setTransformantStatusAndLocation(badTransformant);
            fail("set with null image location should have caused exception");
        } catch (IllegalArgumentException e) {
            assertTrue(true); // test passed
        }

        try {
            Transformant badTransformant =
                    new Transformant(BAD_TRANSFORMANT_ID,
                                     Status.imaged,
                                     BAD_FEATURE_ID);
            badTransformant.setImageLocation(new ImageLocation("", 0));
            dao.setTransformantStatusAndLocation(badTransformant);
            fail("set with bad feature ID should have caused exception");
        } catch (SystemException e) {
            assertTrue(true); // test passed
        }

        dao = new TransformantDao(EMPTY_DB_MANAGER);
        try {
            dao.setTransformantStatusAndLocation(newTransformant);
            fail("inavalid dbManager should have caused exception");
        } catch (SystemException e) {
            assertTrue(true); // test passed
        }
    }

    /**
     * Tests the deleteImageLocation method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testDeleteImageLocation() throws Exception {
        TransformantDao dao = new TransformantDao();
        Transformant transformant = getExistingTransformant(dao, true);

        ImageLocation imageLocation = transformant.getImageLocation();
        Integer rank = imageLocation.getRank();

        dao.deleteImageLocation(transformant);

        Transformant updatedTransformant = getExistingTransformant(dao, true);
        imageLocation = updatedTransformant.getImageLocation();

        assertEquals("invalid rank returned after delete",
                     rank, imageLocation.getRank());

        try {
            dao.deleteImageLocation(null);
            fail("delete with null transformant should have caused exception");
        } catch (IllegalArgumentException e) {
            assertTrue(true); // test passed
        }

        try {
            Transformant badTransformant =
                    new Transformant(BAD_TRANSFORMANT_ID, null, null);
            badTransformant.setImageLocation(null);
            dao.deleteImageLocation(badTransformant);
            fail("delete with null image location should have caused exception");
        } catch (IllegalArgumentException e) {
            assertTrue(true); // test passed
        }

        try {
            Transformant badTransformant =
                    new Transformant(BAD_TRANSFORMANT_ID,
                                     Status.imaged,
                                     BAD_FEATURE_ID);
            badTransformant.setImageLocation(new ImageLocation("", 0));
            dao.deleteImageLocation(badTransformant);
            fail("delete with bad feature ID should have caused exception");
        } catch (TransformantNotFoundException e) {
            assertTrue(true); // test passed
        }
    }

    private Transformant getExistingTransformant(TransformantDao dao,
                                                 boolean isNewImageLocationNeeded)
            throws TransformantNotFoundException, SystemException {
        Transformant transformant =
                dao.getTransformant(EXISTING_TRANSFORMANT_ID,
                                    isNewImageLocationNeeded);
        assertNotNull("transformant should exist", transformant);
        assertEquals("invalid transformant ID returned",
                     EXISTING_TRANSFORMANT_ID,
                     transformant.getTransformantID());
        assertNotNull("status not populated", transformant.getStatus());
        if (isNewImageLocationNeeded) {
            ImageLocation imageLocation = transformant.getImageLocation();
            assertNotNull("image location not returned", imageLocation);
            Integer rank = imageLocation.getRank();
            assertNotNull("image location rank not populated", rank);
            assertTrue("invalid image location rank value of " + rank,
                       (rank >= 0));
        }
        return transformant;
    }

}
