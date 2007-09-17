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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.janelia.it.chacrm.Transformant.Status;
import org.janelia.it.utils.db.DbManager;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Tests the TransformantDao class.
 *
 * @author Eric Trautman
 */
public class TransformantDaoTest extends TestCase {

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(TransformantDaoTest.class);

    private static final String BAD_TRANSFORMANT_ID = "bogus";
    private static final Integer BAD_FEATURE_ID = -1;
    private static final DbManager EMPTY_DB_MANAGER =
            new DbManager("empty", new Properties());

    private DbManager dbManager;
    private TransformantDao dao;
    private Integer testFragmentFeatureId;
    private Transformant existingTransformant;
    private int locationPathSequence = 0;
    private boolean isCleanupNeeded = true;

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

    protected void setUp() throws Exception {
        Properties props = TransformantDao.loadChaCRMDatabaseProperties();
        dbManager = new DbManager("chacrm", props);

        dao = new TransformantDao();

        creatTestFragment(Transformant.Status.transformant);
        existingTransformant =
                createTransformant(Transformant.Status.transformant);
    }

    protected void tearDown() throws Exception {
        if (isCleanupNeeded) {
            deleteTestFragment();
        }
    }

    /**
     * Tests the getTransformant method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetTransformant() throws Exception {

        // test retrieval without rank
        String transformantName = existingTransformant.getTransformantID();
        getExistingTransformant(transformantName, false);

        // test retrieval with rank
        getExistingTransformant(transformantName, true);

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
            dao.getTransformant(existingTransformant.getTransformantID(),
                                false);
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

        String transformantName = existingTransformant.getTransformantID();
        Transformant transformant = getExistingTransformant(transformantName,
                                                            true);

        String transformantID = transformant.getTransformantID();
        Integer featureID = transformant.getFeatureID();
        Transformant newTransformant = new Transformant(transformantID,
                                                        Status.imaged,
                                                        featureID);
        ImageLocation existingImageLocation =
                transformant.getImageLocation();
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

        String transformantName = existingTransformant.getTransformantID();
        Transformant transformant = getExistingTransformant(transformantName,
                                                            true);

        ImageLocation imageLocation = transformant.getImageLocation();
        Integer rank = imageLocation.getRank();

        dao.deleteImageLocation(transformant);

        Transformant updatedTransformant =
                getExistingTransformant(transformantName,
                                        true);
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

    /**
     * Tests the deleteImageLocationAndRollbackStatus method.
     *
     * <p>Pre-conditions:  fragment status is imaged,
     *                     transformant status is imaged,
     *                     transformant has 2 image locations</p>
     *
     * <p>Post-conditions: image location is removed,
     *                     transformant and fragment status remain imaged</p>
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testDeleteImageLocationAndRollbackStatus1() throws Exception {

        String transformantName = existingTransformant.getTransformantID();
        Transformant transformant =
                getExistingTransformant(transformantName, true);
        ImageLocation location1 = transformant.getImageLocation();
        String location1Path = getUniqueLocationPath();
        location1 = new ImageLocation(location1Path,
                                      location1.getRank());
        transformant.setImageLocation(location1);

        dao.setTransformantStatusAndLocation(transformant);

        validateFeatureStatus("after first location add",
                              testFragmentFeatureId,
                              Transformant.Status.imaged,
                              true);

        validateFeatureStatus("after first location add",
                              transformant.getFeatureID(),
                              Transformant.Status.imaged,
                              false);

        validateLocations("after first location add",
                          transformantName,
                          Arrays.asList(location1Path));

        transformant = getExistingTransformant(transformantName, true);
        ImageLocation location2 = transformant.getImageLocation();
        String location2Path = getUniqueLocationPath();
        location2 = new ImageLocation(location2Path,
                                      location2.getRank());
        transformant.setImageLocation(location2);

        dao.setTransformantStatusAndLocation(transformant);

        validateFeatureStatus("after second location add",
                              testFragmentFeatureId,
                              Transformant.Status.imaged,
                              true);

        validateFeatureStatus("after second location add",
                              transformant.getFeatureID(),
                              Transformant.Status.imaged,
                              false);

        validateLocations("after second location add",
                          transformantName,
                          Arrays.asList(location1Path, location2Path));

        dao.deleteImageLocationAndRollbackStatus(location1);

        validateFeatureStatus("after delete",
                              testFragmentFeatureId,
                              Transformant.Status.imaged,
                              true);

        validateFeatureStatus("after delete",
                              transformant.getFeatureID(),
                              Transformant.Status.imaged,
                              false);

        validateLocations("after delete",
                          transformantName,
                          Arrays.asList(location2Path));
    }

    /**
     * Tests the deleteImageLocationAndRollbackStatus method.
     *
     * <p>Pre-conditions:  fragment status is imaged,
     *                     transformant status is imaged,
     *                     transformant has 1 image location</p>
     *
     * <p>Post-conditions: image location is removed,
     *                     transformant and fragment status are
     *                     reverted to transformant</p>
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testDeleteImageLocationAndRollbackStatus2() throws Exception {

        String transformantName = existingTransformant.getTransformantID();
        Transformant transformant =
                getExistingTransformant(transformantName, true);
        ImageLocation location1 = transformant.getImageLocation();
        String location1Path = getUniqueLocationPath();
        location1 = new ImageLocation(location1Path,
                                      location1.getRank());
        transformant.setImageLocation(location1);

        dao.setTransformantStatusAndLocation(transformant);

        validateFeatureStatus("after first location add",
                              testFragmentFeatureId,
                              Transformant.Status.imaged,
                              true);

        validateFeatureStatus("after first location add",
                              transformant.getFeatureID(),
                              Transformant.Status.imaged,
                              false);

        validateLocations("after first location add",
                          transformantName,
                          Arrays.asList(location1Path));

        dao.deleteImageLocationAndRollbackStatus(location1);

        validateFeatureStatus("after delete",
                              testFragmentFeatureId,
                              Transformant.Status.transformant,
                              true);

        validateFeatureStatus("after delete",
                              transformant.getFeatureID(),
                              Transformant.Status.transformant,
                              false);

        validateLocations("after delete",
                          transformantName,
                          new ArrayList<String>());
    }

    /**
     * Tests the deleteImageLocationAndRollbackStatus method.
     *
     * <p>Pre-conditions:  fragment status is imaged,
     *                     transformant 1 status is imaged,
     *                     transformant 2 status is imaged,
     *                     transformant 1 has 1 image location</p>
     *
     * <p>Post-conditions: image location is removed,
     *                     fragment status remains as imaged,
     *                     transformant 1 status is
     *                     reverted to transformant</p>
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testDeleteImageLocationAndRollbackStatus3() throws Exception {

        //isCleanupNeeded = false;
        
        String transformantName = existingTransformant.getTransformantID();
        Transformant transformant =
                getExistingTransformant(transformantName, true);
        ImageLocation location1 = transformant.getImageLocation();
        String location1Path = getUniqueLocationPath();
        location1 = new ImageLocation(location1Path,
                                      location1.getRank());
        transformant.setImageLocation(location1);

        dao.setTransformantStatusAndLocation(transformant);

        validateFeatureStatus("after first location add",
                              testFragmentFeatureId,
                              Transformant.Status.imaged,
                              true);

        validateFeatureStatus("after first location add",
                              transformant.getFeatureID(),
                              Transformant.Status.imaged,
                              false);

        validateLocations("after first location add",
                          transformantName,
                          Arrays.asList(location1Path));

        // add second transformant to prevent fragment status rollback
        createTransformant(Transformant.Status.imaged);

        dao.deleteImageLocationAndRollbackStatus(location1);

        validateFeatureStatus("after delete",
                              testFragmentFeatureId,
                              Transformant.Status.imaged,
                              true);

        validateFeatureStatus("after delete",
                              transformant.getFeatureID(),
                              Transformant.Status.transformant,
                              false);

        validateLocations("after delete",
                          transformantName,
                          new ArrayList<String>());
    }


    /**
     * Tests the deleteImageLocationAndRollbackStatus method.
     *
     * <p>Pre-conditions:  fragment status is imaged,
     *                     transformant status is gsi_failed,
     *                     transformant has 1 image location</p>
     *
     * <p>Post-conditions: image location is removed,
     *                     transformant status remains as gsi_failed,
     *                     fragment status remains as imaged</p>
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testDeleteImageLocationAndRollbackStatus4() throws Exception {

        isCleanupNeeded = false;

        String transformantName = existingTransformant.getTransformantID();
        Transformant transformant =
                getExistingTransformant(transformantName, true);
        ImageLocation location1 = transformant.getImageLocation();
        String location1Path = getUniqueLocationPath();
        location1 = new ImageLocation(location1Path,
                                      location1.getRank());
        transformant.setImageLocation(location1);

        dao.setTransformantStatusAndLocation(transformant);

        // set transformant status to gsi_failed after adding location since
        // adding location automatically updates transformant status to imaged
        setFeatureStatus(transformant.getFeatureID(),
                         Transformant.Status.gsi_failed);

        validateFeatureStatus("after first location add",
                              testFragmentFeatureId,
                              Transformant.Status.imaged,
                              true);

        validateFeatureStatus("after first location add",
                              transformant.getFeatureID(),
                              Transformant.Status.gsi_failed,
                              false);

        validateLocations("after first location add",
                          transformantName,
                          Arrays.asList(location1Path));

        dao.deleteImageLocationAndRollbackStatus(location1);

        validateFeatureStatus("after delete",
                              testFragmentFeatureId,
                              Transformant.Status.imaged,
                              true);

        validateFeatureStatus("after delete",
                              transformant.getFeatureID(),
                              Transformant.Status.gsi_failed,
                              false);

        validateLocations("after delete",
                          transformantName,
                          new ArrayList<String>());
    }

    /**
     * Tests the deleteImageLocationAndRollbackStatus method.
     *
     * <p>Pre-conditions:  fragment status is gsi_ready,
     *                     transformant status is imaged,
     *                     transformant has 1 image location</p>
     *
     * <p>Post-conditions: image location is removed,
     *                     fragment status remains as gsi_ready,
     *                     transformant status is
     *                     reverted to transformant</p>
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
//    public void testDeleteImageLocationAndRollbackStatus5() throws Exception {
//
//        String transformantName = existingTransformant.getTransformantID();
//        Transformant transformant =
//                getExistingTransformant(transformantName, true);
//        ImageLocation location1 = transformant.getImageLocation();
//        String location1Path = getUniqueLocationPath();
//        location1 = new ImageLocation(location1Path,
//                                      location1.getRank());
//        transformant.setImageLocation(location1);
//
//        dao.setTransformantStatusAndLocation(transformant);
//
//        // set fragment status to gsi_ready after adding location since
//        // adding location automatically updates fragment status to imaged
//        setFeatureStatus(testFragmentFeatureId, Transformant.Status.gsi_ready);
//
//        validateFeatureStatus("after first location add",
//                              testFragmentFeatureId,
//                              Transformant.Status.gsi_ready,
//                              true);
//
//        validateFeatureStatus("after first location add",
//                              transformant.getFeatureID(),
//                              Transformant.Status.imaged,
//                              false);
//
//        validateLocations("after first location add",
//                          transformantName,
//                          Arrays.asList(location1Path));
//
//        dao.deleteImageLocationAndRollbackStatus(location1);
//
//        validateFeatureStatus("after delete",
//                              testFragmentFeatureId,
//                              Transformant.Status.gsi_ready,
//                              true);
//
//        validateFeatureStatus("after delete",
//                              transformant.getFeatureID(),
//                              Transformant.Status.transformant,
//                              false);
//
//        validateLocations("after delete",
//                          transformantName,
//                          new ArrayList<String>());
//    }

    private Transformant getExistingTransformant(String transformantName,
                                                 boolean isNewImageLocationNeeded)
            throws TransformantNotFoundException, SystemException {
        Transformant transformant =
                dao.getTransformant(transformantName,
                                    isNewImageLocationNeeded);
        assertNotNull("transformant should exist", transformant);
        assertEquals("invalid transformant name returned",
                     transformantName,
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

    private void creatTestFragment(Transformant.Status fragmentStatus)
            throws Exception {

        Connection connection = null;
        ResultSet resultSet = null;
        CallableStatement statement = null;
        try {
            connection = dbManager.getConnection();
            statement = connection.prepareCall(SQL_CALL_CREATE_FRAGMENT);
            statement.registerOutParameter(1, Types.INTEGER);
            statement.setString(2, fragmentStatus.toString());

            statement.executeUpdate();
            testFragmentFeatureId = statement.getInt(1);
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }
    }

    private Transformant createTransformant(Transformant.Status status)
            throws Exception {

        String transformantName = TRANSFORMANT_NAME.format(new Date());
        Transformant transformant =
                new Transformant(transformantName,
                                 status,
                                 testFragmentFeatureId);

        Connection connection = null;
        ResultSet resultSet = null;
        CallableStatement statement = null;
        try {
            connection = dbManager.getConnection();
            statement = connection.prepareCall(SQL_CALL_CREATE_TRANSFORMANT);
            statement.setInt(1, testFragmentFeatureId);
            statement.setString(2, transformant.getTransformantID());
            statement.setString(3, transformant.getStatus().toString());

            statement.executeUpdate();
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }

        return transformant;
    }

    private void setFeatureStatus(Integer featureId,
                                  Transformant.Status status)
            throws Exception {

        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement statement = null;
        try {
            connection = dbManager.getConnection();
            statement = connection.prepareStatement(SQL_UPDATE_FEATURE_STATUS);
            statement.setString(1, status.toString());
            statement.setInt(2, featureId);

            statement.executeUpdate();
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }
    }

    private void deleteTestFragment() throws Exception {
        Connection connection = null;
        ResultSet resultSet = null;
        CallableStatement statement = null;
        try {
            connection = dbManager.getConnection();
            statement = connection.prepareCall(SQL_CALL_DELETE_FRAGMENT);
            statement.setInt(1, testFragmentFeatureId);

            statement.executeUpdate();
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }
    }

    private synchronized String getUniqueLocationPath() {
        locationPathSequence++;
        return RELATIVE_PATH.format(new Date()) + locationPathSequence;
    }

    private void validateFeatureStatus(String message,
                                       Integer featureId,
                                       Transformant.Status expectedStatus,
                                       boolean isFragment)
            throws Exception {

        List<String> statusString = new ArrayList<String>();
        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement pStatement = null;

        try {
            connection = dbManager.getConnection();
            pStatement = connection.prepareStatement(SQL_SELECT_FEATURE_STATUS);
            pStatement.setInt(1, featureId);
            resultSet = pStatement.executeQuery();
            while (resultSet.next()) {
                statusString.add(resultSet.getString(1));
            }
        } finally {
            DbManager.closeResources(resultSet, pStatement, connection, LOG);
        }

        String featureType = "transformant";
        if (isFragment) {
            featureType = "fragment";
        }

        assertEquals(message +
                     ", invalid number of status values returned for " +
                     featureType + " feature id " + featureId + ": " +
                     statusString,
                     1, statusString.size());

        assertEquals(message +
                     ", invalid status returned for " + featureType +
                     " feature id " + featureId,
                     expectedStatus.toString(),
                     statusString.get(0));
    }

    private void validateLocations(String message,
                                   String transformantName,
                                   List<String> expectedPaths)
            throws Exception {

        List<String> actualPaths = new ArrayList<String>();
        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement pStatement = null;

        try {
            connection = dbManager.getConnection();
            pStatement = connection.prepareStatement(
                    SQL_SELECT_TRANSFORMANT_IMAGE_LOCATIONS);
            pStatement.setString(1, transformantName);
            resultSet = pStatement.executeQuery();
            while (resultSet.next()) {
                actualPaths.add(resultSet.getString(1));
            }
        } finally {
            DbManager.closeResources(resultSet, pStatement, connection, LOG);
        }

        assertEquals(message +
                     ", incorrect number of image location paths for " +
                     transformantName + ", paths=" + actualPaths,
                     expectedPaths.size(), actualPaths.size());

        for (String expectedPath : expectedPaths) {
            assertTrue(message +
                       ", expected image location path " + expectedPath +
                       " missing from retrieved paths: " + actualPaths,
                       actualPaths.contains(expectedPath));
        }
    }

    /**
     * SQL for creating a test fragment.
     *   Parameter 1 is the returned fragment feature id.
     *   Parameter 2 is fragment status.
     */
    private static final String SQL_CALL_CREATE_FRAGMENT =
            "{ ? = call store_test_fragment(?) }";

    /**
     * SQL for creating a test transformant.
     *   Parameter 1 is fragment feature id.
     *   Parameter 2 is transformant name.
     *   Parameter 3 is transformant status.
     */
    private static final String SQL_CALL_CREATE_TRANSFORMANT =
            "{ call store_test_transformant(?,?,?) }";

    /**
     * SQL for deleting a test fragment and all its associated data.
     *   Parameter 1 is fragment feature id.
     */
    private static final String SQL_CALL_DELETE_FRAGMENT =
            "{ call remove_test_fragment(?) }";

    /**
     * SQL for retrieving fragment status.
     *   Parameter 1 is feature id.
     */
    private static final String SQL_SELECT_FEATURE_STATUS =
            "SELECT value FROM featureprop " +
            "WHERE feature_id=? AND type_id=60662";
            // type_id sub select:
            //   (SELECT cvterm_id FROM cvterm
            //    WHERE name='ownwer' AND is_obsolete=0)";

    /**
     * SQL for setting feature status.
     *   Parameter 1 is the status value.
     *   Parameter 2 is feature id.
     */
    private static final String SQL_UPDATE_FEATURE_STATUS =
            "UPDATE featureprop SET value=? " +
            "WHERE feature_id=? AND type_id=60662";
            // type_id sub select:
            //   (SELECT cvterm_id FROM cvterm
            //    WHERE name='ownwer' AND is_obsolete=0)";

    /**
     * SQL for retrieving all image locations for a transformant.
     *   Parameter 1 is transformant name.
     */
    private static final String SQL_SELECT_TRANSFORMANT_IMAGE_LOCATIONS =
           "SELECT value FROM featureprop WHERE feature_id=" +
           "(SELECT feature_id FROM feature WHERE name=?) AND type_id=60710";

    private static final SimpleDateFormat TRANSFORMANT_NAME =
            new SimpleDateFormat("'testDaoTransformant'yyyyMMddHHmmssSSS");
    private static final SimpleDateFormat RELATIVE_PATH =
            new SimpleDateFormat("'testDaoLocation'yyyyMMddHHmmssSSS");

}
