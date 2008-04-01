/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.simpsonlab;

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
import java.util.Properties;

/**
 * Tests the ImageDao class.
 *
 * @author Eric Trautman
 */
public class ImageDaoTest extends TestCase {

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(ImageDaoTest.class);

    private DbManager dbManager;
    private ImageDao dao;
    private Line testLine;
    private Image testImage;

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
    public ImageDaoTest(String name) {
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
        return new TestSuite(ImageDaoTest.class);
    }

    protected void setUp() throws Exception {
        Properties props = ImageDao.loadDatabaseProperties();
        dbManager = new DbManager("simpsonlab", props);
        dao = new ImageDao();
        testLine = new Line(LINE_NAME.format(new Date()), null);
        testImage = new Image();
    }

    protected void tearDown() throws Exception {
        if (isCleanupNeeded) {
            deleteTestSequenceNumber();
            if (testImage != null) {
                deleteImage(testImage.getId());
            }
        }
    }

    /**
     * Tests the getNextSpecimenNumber method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetNextSpecimenNumber() throws Exception {
        int specimenNumber = dao.getNextSpecimenNumber(testLine);

        assertEquals("invalid initial number", 1, specimenNumber);

        specimenNumber = dao.getNextSpecimenNumber(testLine);
        assertEquals("invalid update number", 2, specimenNumber);
    }


    /**
     * Tests the addImage method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testAddImage() throws Exception {
        String relativePath = IMAGE_PATH.format(new Date());
        testImage.setRelativePath(relativePath);
        testImage.setCaptureDate(new Date());
        ImageProperty property1 = new ImageProperty("testName1", "testValue1");
        testImage.addProperty(property1);
        ImageProperty property2 = new ImageProperty("testName2", "testValue2");
        testImage.addProperty(property2);

        Image updateImage = dao.addImage(testImage);

        assertNotNull("id not set after add", updateImage.getId());
    }

    private void deleteTestSequenceNumber() throws Exception {
        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement statement = null;
        try {
            connection = dbManager.getConnection();
            statement = connection.prepareStatement(
                    SQL_DELETE_NAMESPACE_SEQUENCE_NUMBER);
            statement.setString(1, testLine.getSpecimenNamespace());
            int rowsUpdated = statement.executeUpdate();
            LOG.info("deleteTestSequenceNumber: completed, " + rowsUpdated +
                     " row(s) updated");
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }
    }

    private void deleteImage(Integer imageId) throws Exception {
        if (imageId != null) {
            Connection connection = null;
            ResultSet resultSet = null;
            PreparedStatement statement = null;
            try {
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

    /**
     * SQL for deleting a namespace sequence number.
     *   Parameter 1 is the line specimen namespace.
     */
    private static final String SQL_DELETE_NAMESPACE_SEQUENCE_NUMBER =
            "DELETE FROM namespace_sequence_number WHERE namespace=?";

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

    private static final SimpleDateFormat LINE_NAME =
            new SimpleDateFormat("'testLine'yyyyMMddHHmmssSSS");
    private static final SimpleDateFormat IMAGE_PATH =
            new SimpleDateFormat("'test/file'yyyyMMddHHmmssSSS");


}