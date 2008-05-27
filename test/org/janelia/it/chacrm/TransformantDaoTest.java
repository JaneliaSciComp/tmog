/*
 * Copyright 2007 Howard Hughes Medical Institute.
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    private Transformant transformant;
    private String transformantName;
    private int locationPathSequence = 0;

    /**
     * This flag can be used to stop database cleanup in each test's
     * tearDown method when you need to debug problems in the database.
     */
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

        creatTestFragment(Transformant.Status.crossed);
        transformant = createTransformant(Transformant.Status.crossed);
        transformantName = transformant.getTransformantID();
        transformant = getTransformant(transformantName, true);
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

        // setup tests retrieval with rank

        // test retrieval without rank
        getTransformant(transformantName, false);

        try {
            Transformant transformant =
                    dao.getTransformant(BAD_TRANSFORMANT_ID, false);
            fail("inavalid transformant ID should have caused exception " +
                 "but returned this instead: " + transformant);
        } catch (TransformantNotFoundException e) {
            LOG.info("expected exception thrown", e); // test passed
        }

        dao = new TransformantDao(EMPTY_DB_MANAGER);
        try {
            dao.getTransformant(transformantName,
                                false);
            fail("inavalid dbManager should have caused exception");
        } catch (SystemException e) {
            LOG.info("expected exception thrown", e); // test passed
        }
    }

    /**
     * Tests the setTransformantStatusAndLocation method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testSetTransformantStatusAndLocation() throws Exception {

        Integer featureID = transformant.getFeatureID();
        Transformant newTransformant = new Transformant(transformantName,
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
                dao.getTransformant(transformantName, false);
        assertNotNull("null transformant returned from database",
                      retrievedTransformant);
        assertEquals("invalid transformant ID returned from database",
                     transformantName,
                     retrievedTransformant.getTransformantID());
        assertEquals("invalid status returned from database",
                     Status.imaged,
                     retrievedTransformant.getStatus());

        try {
            dao.setTransformantStatusAndLocation(null);
            fail("set with null transformant should have caused exception");
        } catch (IllegalArgumentException e) {
            LOG.info("expected exception thrown", e); // test passed
        }

        try {
            Transformant badTransformant =
                    new Transformant(BAD_TRANSFORMANT_ID, null, null);
            badTransformant.setImageLocation(null);
            dao.setTransformantStatusAndLocation(badTransformant);
            fail("set with null image location should have caused exception");
        } catch (IllegalArgumentException e) {
            LOG.info("expected exception thrown", e); // test passed
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
            LOG.info("expected exception thrown", e); // test passed
        }

        dao = new TransformantDao(EMPTY_DB_MANAGER);
        try {
            dao.setTransformantStatusAndLocation(newTransformant);
            fail("inavalid dbManager should have caused exception");
        } catch (SystemException e) {
            LOG.info("expected exception thrown", e); // test passed
        }
    }

    /**
     * Tests the deleteImageLocation method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testDeleteImageLocation() throws Exception {

        ImageLocation imageLocation = transformant.getImageLocation();
        Integer rank = imageLocation.getRank();

        dao.deleteImageLocation(transformant);

        Transformant updatedTransformant = getTransformant(transformantName,
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

        ImageLocation location1 = transformant.getImageLocation();
        String location1Path = getUniqueLocationPath();
        location1 = new ImageLocation(location1Path,
                                      location1.getRank());
        transformant.setImageLocation(location1);

        dao.setTransformantStatusAndLocation(transformant);

        validateFragmentElements("after first location add",
                                 Transformant.Status.imaged,
                                 transformant.getFeatureID(),
                                 Transformant.Status.imaged,
                                 transformantName,
                                 Arrays.asList(location1Path));

        transformant = getTransformant(transformantName, true);
        ImageLocation location2 = transformant.getImageLocation();
        String location2Path = getUniqueLocationPath();
        location2 = new ImageLocation(location2Path,
                                      location2.getRank());
        transformant.setImageLocation(location2);

        dao.setTransformantStatusAndLocation(transformant);

        validateFragmentElements("after second location add",
                                 Transformant.Status.imaged,
                                 transformant.getFeatureID(),
                                 Transformant.Status.imaged,
                                 transformantName,
                                 Arrays.asList(location1Path, location2Path));

        dao.deleteImageLocationAndRollbackStatus(location1);

        validateFragmentElements("after delete",
                                 Transformant.Status.imaged,
                                 transformant.getFeatureID(),
                                 Transformant.Status.imaged,
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

        ImageLocation location1 = transformant.getImageLocation();
        String location1Path = getUniqueLocationPath();
        location1 = new ImageLocation(location1Path,
                                      location1.getRank());
        transformant.setImageLocation(location1);

        dao.setTransformantStatusAndLocation(transformant);

        validateFragmentElements("after first location add",
                                 Transformant.Status.imaged,
                                 transformant.getFeatureID(),
                                 Transformant.Status.imaged,
                                 transformantName,
                                 Arrays.asList(location1Path));

        dao.deleteImageLocationAndRollbackStatus(location1);

        validateFragmentElements("after delete",
                                 Transformant.Status.crossed,
                                 transformant.getFeatureID(),
                                 Transformant.Status.crossed,
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

        ImageLocation location1 = transformant.getImageLocation();
        String location1Path = getUniqueLocationPath();
        location1 = new ImageLocation(location1Path,
                                      location1.getRank());
        transformant.setImageLocation(location1);

        dao.setTransformantStatusAndLocation(transformant);

        validateFragmentElements("after first location add",
                                 Transformant.Status.imaged,
                                 transformant.getFeatureID(),
                                 Transformant.Status.imaged,
                                 transformantName,
                                 Arrays.asList(location1Path));

        // add second transformant to prevent fragment status rollback
        createTransformant(Transformant.Status.imaged);

        dao.deleteImageLocationAndRollbackStatus(location1);

        validateFragmentElements("after delete",
                                 Transformant.Status.imaged,
                                 transformant.getFeatureID(),
                                 Transformant.Status.crossed,
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
     * <p>Post-conditions: transaction should fail since
     *                     transformant is expected to have imaged status,
     *                     fragment, transformant, and image location
     *                     should remain unchanged</p>
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testDeleteImageLocationAndRollbackStatus4() throws Exception {

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

        validateFragmentElements("after first location add",
                                 Transformant.Status.imaged,
                                 transformant.getFeatureID(),
                                 Transformant.Status.gsi_failed,
                                 transformantName,
                                 Arrays.asList(location1Path));

        try {
            dao.deleteImageLocationAndRollbackStatus(location1);
            fail("transformant without imaged status should cause exception");
        } catch (SystemException e) {
            LOG.info("expected exception was thrown", e);  // test passed
        }

        // after failure, everything should remain the same
        validateFragmentElements("after failure",
                                 Transformant.Status.imaged,
                                 transformant.getFeatureID(),
                                 Transformant.Status.gsi_failed,
                                 transformantName,
                                 Arrays.asList(location1Path));
    }

    /**
     * Tests the deleteImageLocationAndRollbackStatus method.
     *
     * <p>Pre-conditions:  fragment status is imaged,
     *                     transformant status is imaged,
     *                     transformant has 1 image location</p>
     *
     * <p>Attempt to delete a non-existent location.</p>
     *
     * <p>Post-conditions: transaction should fail,
     *                     fragment, transformant, and image location
     *                     should remain unchanged</p>
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testDeleteImageLocationAndRollbackStatus5() throws Exception {

        ImageLocation location1 = transformant.getImageLocation();
        String location1Path = getUniqueLocationPath();
        location1 = new ImageLocation(location1Path,
                                      location1.getRank());
        transformant.setImageLocation(location1);

        dao.setTransformantStatusAndLocation(transformant);

        validateFragmentElements("after first location add",
                                 Transformant.Status.imaged,
                                 transformant.getFeatureID(),
                                 Transformant.Status.imaged,
                                 transformantName,
                                 Arrays.asList(location1Path));

        ImageLocation badLocation =
                new ImageLocation(location1.getRelativePath() + "bad",
                                  location1.getRank());
        try {
            dao.deleteImageLocationAndRollbackStatus(badLocation);
            fail("bad image location should cause exception");
        } catch (SystemException e) {
            LOG.info("expected exception was thrown", e);  // test passed
        }

        // after failure, everything should remain the same
        validateFragmentElements("after failure",
                                 Transformant.Status.imaged,
                                 transformant.getFeatureID(),
                                 Transformant.Status.imaged,
                                 transformantName,
                                 Arrays.asList(location1Path));
    }

    /**
     * Tests the deleteImageLocationAndRollbackStatus method.
     *
     * <p>Pre-conditions:  fragment status is imaged,
     *                     transformant status is imaged,
     *                     transformant has 2 image locations with same path</p>
     *
     * <p>Post-conditions: transaction should fail,
     *                     fragment, transformant, and image locations
     *                     should remain unchanged</p>
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testDeleteImageLocationAndRollbackStatus6() throws Exception {

        ImageLocation location1 = transformant.getImageLocation();
        String location1Path = getUniqueLocationPath();
        location1 = new ImageLocation(location1Path,
                                      location1.getRank());
        transformant.setImageLocation(location1);

        dao.setTransformantStatusAndLocation(transformant);

        validateFragmentElements("after first location add",
                                 Transformant.Status.imaged,
                                 transformant.getFeatureID(),
                                 Transformant.Status.imaged,
                                 transformantName,
                                 Arrays.asList(location1Path));

        transformant = getTransformant(transformantName, true);
        ImageLocation location2 = transformant.getImageLocation();
        location2 = new ImageLocation(location1Path,
                                      location2.getRank());
        transformant.setImageLocation(location2);

        dao.setTransformantStatusAndLocation(transformant);

        validateFragmentElements("after second location add",
                                 Transformant.Status.imaged,
                                 transformant.getFeatureID(),
                                 Transformant.Status.imaged,
                                 transformantName,
                                 Arrays.asList(location1Path, location1Path));

        try {
            dao.deleteImageLocationAndRollbackStatus(location1);
            fail("duplicate image location should cause exception");
        } catch (SystemException e) {
            LOG.info("expected exception was thrown", e);  // test passed
        }

        // after failure, everything should remain the same
        validateFragmentElements("after failure",
                                 Transformant.Status.imaged,
                                 transformant.getFeatureID(),
                                 Transformant.Status.imaged,
                                 transformantName,
                                 Arrays.asList(location1Path, location1Path));
    }

    private Transformant getTransformant(String transformantName,
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

    private void creatTestFragment(Transformant.Status status)
            throws Exception {

        String fragmentName = FRAGMENT_NAME.format(new Date());
        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement statement = null;
        try {
            connection = dbManager.getConnection();
            statement = connection.prepareStatement(SQL_INSERT_FEATURE);
            statement.setString(1, fragmentName);
            statement.setString(2, fragmentName);
            statement.setInt(3, FRAGMENT_TYPE_ID);

            statement.executeUpdate();

            statement =
                    connection.prepareStatement(SQL_SELECT_FEATURE_ID);
            statement.setString(1, fragmentName);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                testFragmentFeatureId = resultSet.getInt(1);
            } else {
                throw new IllegalStateException("fragment not found: " +
                                                fragmentName);
            }

            statement =
                    connection.prepareStatement(SQL_INSERT_FEATURE_PROPERTY);
            statement.setInt(1, testFragmentFeatureId);
            statement.setInt(2, STATUS_TYPE_ID);
            statement.setString(3, status.toString());

            statement.executeUpdate();
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
        PreparedStatement statement = null;
        try {
            connection = dbManager.getConnection();
            statement = connection.prepareStatement(SQL_INSERT_FEATURE);
            statement.setString(1, transformantName);
            statement.setString(2, transformantName);
            statement.setInt(3, TRANSFORMANT_TYPE_ID);

            statement.executeUpdate();

            Integer transformantFeatureId;
            statement =
                    connection.prepareStatement(SQL_SELECT_FEATURE_ID);
            statement.setString(1, transformantName);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                transformantFeatureId = resultSet.getInt(1);
            } else {
                throw new IllegalStateException("transformant not found: " +
                                                transformantName);
            }

            statement =
                    connection.prepareStatement(SQL_INSERT_FEATURE_PROPERTY);
            statement.setInt(1, transformantFeatureId);
            statement.setInt(2, STATUS_TYPE_ID);
            statement.setString(3, status.toString());

            statement.executeUpdate();

            statement.setInt(1, transformantFeatureId);
            statement.setInt(2, FRAGMENT_TYPE_ID);
            statement.setString(3, testFragmentFeatureId.toString());

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
            statement =
                    connection.prepareStatement(SQL_UPDATE_FEATURE_PROPERTY);
            statement.setString(1, status.toString());
            statement.setInt(2, featureId);
            statement.setInt(3, STATUS_TYPE_ID);

            statement.executeUpdate();
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }
    }

    private void deleteTestFragment() throws Exception {
        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement statement = null;
        try {
            connection = dbManager.getConnection();
            statement = connection.prepareStatement(
                    SQL_SELECT_TRANSFORMANTS_FOR_FRAGMENT);
            statement.setString(1, testFragmentFeatureId.toString());
            resultSet = statement.executeQuery();
            StringBuilder sqlDeleteFeature = new StringBuilder(128);
            sqlDeleteFeature.append(SQL_DELETE_FEATURE);
            sqlDeleteFeature.append(testFragmentFeatureId);
            while (resultSet.next()) {
                sqlDeleteFeature.append(",");
                sqlDeleteFeature.append(resultSet.getString(1));
            }
            sqlDeleteFeature.append(")");

            statement =
                    connection.prepareStatement(sqlDeleteFeature.toString());
            statement.executeUpdate();

            LOG.info("deleteTestFragment: completed " + sqlDeleteFeature);
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }
    }

    private synchronized String getUniqueLocationPath() {
        locationPathSequence++;
        return RELATIVE_PATH.format(new Date()) + locationPathSequence;
    }

    private void validateFragmentElements(String message,
                                          Transformant.Status fragmentStatus,
                                          Integer transformantFeatureId,
                                          Transformant.Status transformantStatus,
                                          String transformantName,
                                          List<String> locationPaths)
            throws Exception {

        validateFeatureStatus(message,
                              testFragmentFeatureId,
                              fragmentStatus,
                              true);

        validateFeatureStatus(message,
                              transformantFeatureId,
                              transformantStatus,
                              false);

        validateLocations(message,
                          transformantName,
                          locationPaths);
    }

    private void validateFeatureStatus(String message,
                                       Integer featureId,
                                       Transformant.Status expectedStatus,
                                       boolean isFragment)
            throws Exception {

        List<String> statusString = new ArrayList<String>();
        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement statement = null;

        try {
            connection = dbManager.getConnection();
            statement =
                    connection.prepareStatement(SQL_SELECT_FEATURE_PROPERTY);
            statement.setInt(1, featureId);
            statement.setInt(2, STATUS_TYPE_ID);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                statusString.add(resultSet.getString(1));
            }
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
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
     * The type id for "status" properties in the featureprop table.
     *
     * SELECT cvterm_id FROM cvterm WHERE name='ownwer' AND is_obsolete=0
     */
    private static final Integer STATUS_TYPE_ID = 60662;

    /**
     * The type id for "transformant" features in the feature table.
     *
     * SELECT cvterm_id FROM cvterm WHERE name='transformant' AND is_obsolete=0 
     */
    private static final Integer TRANSFORMANT_TYPE_ID = 60707;

    /**
     * The type id for "fragment" features in the feature table.
     *
     * SELECT cvterm_id FROM cvterm WHERE name='tiling_path_fragment_id' AND is_obsolete=0 
     */
    private static final Integer FRAGMENT_TYPE_ID = 60709;

    /**
     * The type id for "image location" properties in the featureprop table.
     *
     * SELECT cvterm_id FROM cvterm WHERE name='image_location' AND is_obsolete=0 
     */
    private static final Integer LOCATION_TYPE_ID = 60710;

    /**
     * SQL for retrieving a feature id.
     *   Parameter 1 is the feature name.
     */
    private static final String SQL_SELECT_FEATURE_ID =
           "SELECT feature_id FROM feature WHERE name=?";

    /**
     * SQL for inserting a feature.
     *   Parameter 1 is the feature name.
     *   Parameter 2 is the feature name.
     *   Parameter 3 is the feature type id.
     */
    private static final String SQL_INSERT_FEATURE =
            "INSERT INTO feature " +
            "(organism_id, name, uniquename, type_id, is_analysis) " +
            "VALUES (1, ?, ?, ?, false)";

    /**
     * SQL for deleting a feature.
     *   NOTE: deleting a feature will also delete all of its properties.
     */
    private static final String SQL_DELETE_FEATURE =
            "DELETE FROM feature WHERE feature_id IN (";

    /**
     * SQL for retrieving all image locations for a transformant.
     *   Parameter 1 is the transformant name.
     */
    private static final String SQL_SELECT_TRANSFORMANT_IMAGE_LOCATIONS =
           "SELECT value FROM featureprop WHERE feature_id=" +
           "(SELECT feature_id FROM feature WHERE name=?) AND type_id=" +
           LOCATION_TYPE_ID;

    /**
     * SQL for retrieving a the transformant feature ids for a fragment.
     *   Parameter 1 is the fragment feature id.
     */
    private static final String SQL_SELECT_TRANSFORMANTS_FOR_FRAGMENT =
            "SELECT feature_id FROM featureprop " +
            "WHERE type_id=" + FRAGMENT_TYPE_ID + " AND value=?";

    /**
     * SQL for retrieving a feature property value.
     *   Parameter 1 is the feature id.
     *   Parameter 2 is the property type id.
     */
    private static final String SQL_SELECT_FEATURE_PROPERTY =
            "SELECT value FROM featureprop " +
            "WHERE feature_id=? AND type_id=?";

    /**
     * SQL for inserting a feature property value.
     *   Parameter 1 is the feature id.
     *   Parameter 2 is the property type id.
     *   Parameter 3 is the property value.
     */
    private static final String SQL_INSERT_FEATURE_PROPERTY =
            "INSERT INTO featureprop (feature_id, type_id, value) " +
            "VALUES (?, ?, ?)";

    /**
     * SQL for updating a feature property value.
     *   Parameter 1 is the status value.
     *   Parameter 2 is the feature id.
     *   Parameter 3 is the property type id.
     */
    private static final String SQL_UPDATE_FEATURE_PROPERTY =
            "UPDATE featureprop SET value=? " +
            "WHERE feature_id=? AND type_id=?";

    private static final SimpleDateFormat FRAGMENT_NAME =
            new SimpleDateFormat("'testDaoFragment'yyyyMMddHHmmssSSS");
    private static final SimpleDateFormat TRANSFORMANT_NAME =
            new SimpleDateFormat("'testDaoTransformant'yyyyMMddHHmmssSSS");
    private static final SimpleDateFormat RELATIVE_PATH =
            new SimpleDateFormat("'testDaoLocation'yyyyMMddHHmmssSSS");

}