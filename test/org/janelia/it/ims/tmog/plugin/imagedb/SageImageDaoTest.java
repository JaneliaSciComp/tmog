/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.utils.db.DbManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Tests the ImageDao class.
 *
 * @author Eric Trautman
 */
public class SageImageDaoTest
        extends TestCase {

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(SageImageDaoTest.class);

    private SageImageDao dao;
    private Image testImage;
    private String sequenceNamespace;
    private String testLineName;

    /**
     * This flag can be used to stop database cleanup in the image test's
     * tearDown method when you need to debug problems in the database.
     */
    private boolean isImageCleanupNeeded = true;
    private boolean isSequenceCleanupNeeded = false;
    private boolean isLineCleanupNeeded = false;

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public SageImageDaoTest(String name) {
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
        return new TestSuite(SageImageDaoTest.class);
    }

    protected void setUp() throws Exception {
        dao = new SageImageDao("sage");
        testImage = new Image();
        SimpleDateFormat namespaceTemplate =
            new SimpleDateFormat("'testLine'yyyyMMddHHmmssSSS");
        sequenceNamespace = namespaceTemplate.format(new Date());
        testLineName = sequenceNamespace;
    }

    protected void tearDown() throws Exception {
        if (isImageCleanupNeeded) {
            if (testImage != null) {
                deleteImage(testImage.getId());
            }
        }
        if (isSequenceCleanupNeeded) {
            deleteTestSequenceNumber();
        }
        if (isLineCleanupNeeded) {
            deleteLine();
        }
    }

    /**
     * Tests the saveProperties and getImageId methods.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testSaveProperties() throws Exception {
        String relativePath = IMAGE_PATH.format(new Date());
        testImage.setRelativePaths(relativePath, null);
        testImage.setCaptureDate(new Date());
        testImage.setFamily(FAMILY_1);
        testImage.addProperty(TEST_PROPERTY_1, "valueA");
        testImage.addProperty(TEST_PROPERTY_2, "valueB");
        testImage.addProperty(Image.LINE_PROPERTY, LINE_NAME_1);
        testImage.addProperty(Image.LAB_PROPERTY, LINE_LAB_1);

        testImage = dao.saveProperties(testImage);
        Integer testImageId = testImage.getId();

        assertNotNull("id not set after add", testImage.getId());

        testImage = new Image();
        testImage.setId(testImageId);
        testImage.setRelativePaths(relativePath, null);
        testImage.setCaptureDate(null);
        testImage.setFamily(FAMILY_2);
        testImage.addProperty(TEST_PROPERTY_1, "updatedValueA");
        testImage.addProperty(TEST_PROPERTY_2, "updatedValueB");
        testImage.addProperty(TEST_PROPERTY_3, "valueC");
        testImage.addProperty(Image.LINE_PROPERTY, LINE_NAME_2);
        testImage.addProperty(Image.LAB_PROPERTY, LINE_LAB_2);

        testImage = dao.saveProperties(testImage);

        assertEquals("id changed after update", testImageId, testImage.getId());

        Integer imageId = dao.getImageId(relativePath);
        assertEquals("invalid id returned for " + relativePath,
                     testImageId, imageId);

        testImage = new Image();
        testImage.setId(testImageId);
        testImage.setRelativePaths(relativePath, null);
        testImage.addProperty(TEST_PROPERTY_3, "updatedValueC");
        testImage.addProperty(Image.LINE_PROPERTY, LINE_NAME_2);

        testImage = dao.saveProperties(testImage);

        assertEquals("id changed after update", testImageId, testImage.getId());

        relativePath = "missing-relative-path";
        imageId = dao.getImageId(relativePath);
        assertNull("id returned for invalid path: " + relativePath, imageId);

        Map<String, String> imageData =
                dao.getImageData(FAMILY_2,
                                 testImage.getRelativePath());
        assertNotNull("image data not found", imageData);
        assertEquals("property missing from image data",
                     "updatedValueC", imageData.get(TEST_PROPERTY_3));        
    }

    /**
     * Tests the saveProperties method for an image that has no properties.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testSaveImageOnly() throws Exception {
        String relativePath = IMAGE_PATH.format(new Date());
        testImage.setRelativePaths(relativePath, null);
        testImage.setCaptureDate(new Date());
        testImage.setFamily(FAMILY_1);
        testImage.addProperty(Image.LINE_PROPERTY, LINE_NAME_1);

        testImage = dao.saveProperties(testImage);
        Integer testImageId = testImage.getId();

        assertNotNull("id not set after add", testImage.getId());

        testImage = new Image();
        testImage.setId(testImageId);
        testImage.setRelativePaths(relativePath, null);
        testImage.setCaptureDate(new Date());
        testImage.setFamily(FAMILY_2);

        testImage = dao.saveProperties(testImage);

        assertEquals("id changed after update", testImageId, testImage.getId());
    }

    /**
     * Tests the saveProperties method for an existing image whose
     * relative path has changed.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testSaveForChangedRelativePath() throws Exception {
        String relativePath = IMAGE_PATH.format(new Date());
        testImage.setRelativePaths(relativePath, null);
        testImage.setCaptureDate(new Date());
        testImage.setFamily(FAMILY_1);
        testImage.addProperty(Image.LINE_PROPERTY, LINE_NAME_1);

        testImage = dao.saveProperties(testImage);
        Integer testImageId = testImage.getId();

        assertNotNull("id not set after add", testImage.getId());

        String previousRelativePath = relativePath;
        relativePath = previousRelativePath + "_changed";
        testImage = new Image();
        testImage.setId(testImageId);
        testImage.setRelativePaths(relativePath, previousRelativePath);
        testImage.setCaptureDate(new Date());
        testImage.setFamily(FAMILY_1);
        // line info required since previous path image get deleted
        testImage.addProperty(Image.LINE_PROPERTY, LINE_NAME_1);

        testImage = dao.saveProperties(testImage);

        Integer previousId = dao.getImageId(previousRelativePath);
        assertNull("image for previous relative path '" +
                   previousRelativePath + "' should have been removed",
                   previousId);

        Integer imageId = dao.getImageId(relativePath);
        assertEquals("invalid id returned for " + relativePath,
                     testImage.getId(), imageId);
    }

    /**
     * Tests the getNextSpecimenNumber method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetNextSequenceNumber() throws Exception {
        isSequenceCleanupNeeded = true;

        int specimenNumber = dao.getNextSequenceNumber(sequenceNamespace);

        assertEquals("invalid initial number", 1, specimenNumber);

        specimenNumber = dao.getNextSequenceNumber(sequenceNamespace);
        assertEquals("invalid update number", 2, specimenNumber);
    }

    /**
     * Tests the addLine method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testAddLine() throws Exception {
        isLineCleanupNeeded = true;

        Line line = new Line(testLineName, "flylight");
        Line dbdLine = new Line(LINE_NAME_1, LINE_LAB_1);
        Line adLine = new Line(LINE_NAME_2, LINE_LAB_2);
        line.setParentA(dbdLine);
        line.setParentB(adLine);

        Line addedLine = dao.addLine(line);

        assertNotNull("missing line id", addedLine.getId());

        Line secondTryLine = dao.addLine(line);

        assertEquals("multiple ids exist for line",
                     addedLine.getId(), secondTryLine.getId());
    }

    private void deleteImage(Integer imageId) throws Exception {
        if (imageId != null) {
            Connection connection = null;
            ResultSet resultSet = null;
            PreparedStatement statement = null;
            try {
                DbManager dbManager = dao.getDbManager();
                connection = dbManager.getConnection();
                statement = connection.prepareStatement(
                        SQL_DELETE_IMAGE_PROPERTIES);
                statement.setInt(1, imageId);
                int rowsUpdated = statement.executeUpdate();
                LOG.info("deleteImage: removed " + rowsUpdated +
                         " image_property row(s) for image id " + imageId);

                statement = connection.prepareStatement(SQL_DELETE_IMAGE);
                statement.setInt(1, imageId);
                rowsUpdated = statement.executeUpdate();
                LOG.info("deleteImage: removed " + rowsUpdated +
                         " image row(s) for image id " + imageId);
            } finally {
                DbManager.closeResources(resultSet, statement, connection, LOG);
            }
        }
    }

    private void deleteTestSequenceNumber() throws Exception {
        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement statement = null;
        try {
            DbManager dbManager = dao.getDbManager();
            connection = dbManager.getConnection();
            statement = connection.prepareStatement(
                    SQL_DELETE_NAMESPACE_SEQUENCE_NUMBER);
            statement.setString(1, sequenceNamespace);
            int rowsUpdated = statement.executeUpdate();
            LOG.info("deleteTestSequenceNumber: completed, " + rowsUpdated +
                     " row(s) updated");
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }
    }

    private void deleteLine() throws Exception {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            DbManager dbManager = dao.getDbManager();
            connection = dbManager.getConnection();
            statement = connection.prepareStatement(SQL_DELETE_LINE);
            statement.setString(1, testLineName);
            int rowsUpdated = statement.executeUpdate();
            LOG.info("deleteLine: removed " + rowsUpdated +
                     " row(s) for " + testLineName);

        } finally {
            DbManager.closeResources(null, statement, connection, LOG);
        }
    }

    private static final String TEST_PROPERTY_1 = "age";
    private static final String TEST_PROPERTY_2 = "effector"; // use cv_relationship "inherited" type
    private static final String TEST_PROPERTY_3 = "gender";
    private static final String FAMILY_1 = "baker_lab";
    private static final String FAMILY_2 = "baker_biorad";
    private static final String LINE_LAB_1 = "baker";
    private static final String LINE_NAME_1 = "10-102";
    private static final String LINE_LAB_2 = "rubin";
    private static final String LINE_NAME_2 = "GMR_10A01_AE_01";

    /**
     * SQL for deleting all properties for an image.
     *   Parameter 1 is the image id.
     */
    private static final String SQL_DELETE_IMAGE_PROPERTIES =
            "DELETE FROM image_property WHERE image_id=?";

    /**
     * SQL for deleting all properties for an image.
     *   Parameter 1 is the image id.
     */
    private static final String SQL_DELETE_IMAGE =
            "DELETE FROM image WHERE id=?";

    private static final SimpleDateFormat IMAGE_PATH =
            new SimpleDateFormat("'test/file'yyyyMMddHHmmssSSS");

    /**
     * SQL for deleting a namespace sequence number.
     *   Parameter 1 is the sequence namespace.
     */
    private static final String SQL_DELETE_NAMESPACE_SEQUENCE_NUMBER =
            "DELETE FROM namespace_sequence_number WHERE namespace=?";

    /**
     * SQL for deleting a test line.
     *   Parameter 1 is the unique line name.
     */
    private static final String SQL_DELETE_LINE =
            "DELETE FROM line WHERE name=?";
}