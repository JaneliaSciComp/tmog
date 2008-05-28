/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
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
        final String configurationKey = "simpsonlab";
        Properties props = ImageDao.loadDatabaseProperties(configurationKey);
        dbManager = new DbManager(configurationKey, props);
        dao = new ImageDao(configurationKey);
        testImage = new Image();
    }

    protected void tearDown() throws Exception {
        if (isCleanupNeeded) {
            if (testImage != null) {
                deleteImage(testImage.getId());
            }
        }
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
        testImage.setFamily("testFamily");
        testImage.addProperty("testPropertyA", "valueA");
        testImage.addProperty("testPropertyB", "valueB");

        Image updateImage = dao.addImage(testImage);

        assertNotNull("id not set after add", updateImage.getId());
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
}