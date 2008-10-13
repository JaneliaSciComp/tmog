/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.utils.db.DbConfigException;
import org.janelia.it.utils.db.DbManager;
import org.janelia.it.utils.security.StringEncrypter;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * This class supports management of image data within an image database.
 *
 * @author Eric Trautman
 */
public class ImageDao {

    /**
     * The manager used to establish connections with the database.
     */
    private DbManager dbManager;

    /**
     * Constructs a dao using the default manager and configuration.
     *
     * @param  dbConfigurationKey  the key for loading database
     *                             configuration information.
     *
     * @throws ExternalSystemException
     *   if the database configuration information cannot be loaded.
     */
    public ImageDao(String dbConfigurationKey) throws ExternalSystemException {
        Properties props = loadDatabaseProperties(dbConfigurationKey);
        dbManager = new DbManager(dbConfigurationKey, props);
    }

    public DbManager getDbManager() {
        return dbManager;
    }

    /**
     * Verifies that a connection to the database can be established.
     *
     * @throws ExternalSystemException
     *   if a connection to the database can not be established.
     */
    public void checkConnection() throws ExternalSystemException {
        Connection connection = null;
        try {
            connection = dbManager.getConnection();
        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } finally {
            DbManager.closeResources(null, null, connection, LOG);
        }
    }

    public Image addImage(Image image) throws ExternalSystemException {
        String relativePath = image.getRelativePath();

        int imageId ;
        Connection connection = null;
        ResultSet selectResultSet = null;
        PreparedStatement insertImage = null;
        PreparedStatement select = null;
        PreparedStatement insertProperty = null;

        try {
            connection = dbManager.getConnection();
            connection.setAutoCommit(false);

            insertImage = connection.prepareStatement(SQL_INSERT_IMAGE);
            insertImage.setString(1, relativePath);
            Date captureDate = image.getCaptureDate();
            if (captureDate != null) {
                insertImage.setDate(2,
                                    new java.sql.Date(captureDate.getTime()));
            } else {
                insertImage.setDate(2, null);
            }
            insertImage.setString(3, image.getFamily());

            int rowsUpdated = insertImage.executeUpdate();
            if (rowsUpdated != 1) {
                throw new ExternalSystemException(
                        "Failed to create image '" + relativePath +
                        "'.  Attempted to create " + rowsUpdated + " rows.");
            }

            select = connection.prepareStatement(SQL_SELECT_IMAGE_ID);
            select.setString(1, relativePath);
            selectResultSet = select.executeQuery();
            if (selectResultSet.next()) {
                imageId = selectResultSet.getInt(1);
            } else {
                throw new ExternalSystemException(
                        "Failed to retrieve id for image '" + relativePath +
                        "'.");
            }

            insertProperty =
                    connection.prepareStatement(SQL_INSERT_IMAGE_PROPERTY);
            Map<String, String> properties = image.getPropertyTypeToValueMap();
            int numberOfPropertiesToAdd = 0;
            String value;
            for (String type : properties.keySet()) {
                value = properties.get(type);
                if ((value != null) && (value.length() > 0)) {
                    insertProperty.setInt(1, imageId);
                    insertProperty.setString(2, type);
                    insertProperty.setString(3, value);
                    insertProperty.addBatch();
                    numberOfPropertiesToAdd++;
                }
            }

            if (numberOfPropertiesToAdd > 0) {
                int[] numUpdates = insertProperty.executeBatch();
                if (numUpdates.length != numberOfPropertiesToAdd) {
                    throw new ExternalSystemException(
                            "Failed to add image properties for '" +
                            relativePath + "'.  Executed " +
                            numUpdates.length + " instead of " +
                            numberOfPropertiesToAdd + " statement(s).");
                } else {
                    for (int i = 0; i < numUpdates.length; i++) {
                        if (numUpdates[i] != 1) {
                            throw new ExternalSystemException(
                                    "Failed to add image properties for '" +
                                    relativePath + "'.  Attempted to create " +
                                    numUpdates[i] + " row(s) for property " +
                                    i + ".");
                        }
                    }
                }
            }

            connection.commit();
            image.setId(imageId);

        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ExternalSystemException(
                    "Failed to store image properties for '" +
                    relativePath + "'.", e);
        } finally {
            DbManager.closeResources(null, insertImage, null, LOG);
            DbManager.closeResources(selectResultSet, select, null, LOG);
            DbManager.closeResources(null, insertProperty, connection, LOG);
        }

        return image;
    }

    /**
     * @param  namespace  identifies the sequence's namespace.
     *
     * @return the next sequence number for the specified namespace.
     *
     * @throws ExternalSystemException
     *   if errors occur while retrieving the data.
     */
    public int getNextSequenceNumber(String namespace)
            throws ExternalSystemException {

        int nextNumber = 1;
        int rowsUpdated;

        Connection connection = null;
        ResultSet selectResultSet = null;
        PreparedStatement select = null;
        PreparedStatement increment = null;

        try {
            DbManager dbManager = getDbManager();
            connection = dbManager.getConnection();
            connection.setAutoCommit(false);
            select = connection.prepareStatement(SQL_SELECT_SEQUENCE_NUMBER);
            select.setString(1, namespace);
            selectResultSet = select.executeQuery();
            if (selectResultSet.next()) {
                Integer currentNumber = selectResultSet.getInt(1);
                nextNumber = currentNumber + 1;
                increment =
                        connection.prepareStatement(SQL_UPDATE_SEQUENCE_NUMBER);
                increment.setInt(1, nextNumber);
                increment.setString(2, namespace);
                rowsUpdated = increment.executeUpdate();
                if (rowsUpdated != 1) {
                    throw new ExternalSystemException(
                            "Failed to update next sequence number for " +
                            namespace + ".  Attempted to update " +
                            rowsUpdated + " rows.");
                }
            } else {
                increment =
                        connection.prepareStatement(SQL_INSERT_SEQUENCE_NUMBER);
                increment.setString(1, namespace);
                rowsUpdated = increment.executeUpdate();
                if (rowsUpdated != 1) {
                    throw new ExternalSystemException(
                            "Failed to create sequence number for " +
                            namespace + ".  Attempted to create " +
                            rowsUpdated + " rows.");
                }
            }

            connection.commit();

        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ExternalSystemException(
                    "Failed to determine next sequence number for " +
                    namespace, e);
        } finally {
            DbManager.closeResources(selectResultSet, select, null, LOG);
            DbManager.closeResources(null, increment, connection, LOG);
        }

        return nextNumber;
    }

    /**
     * Utility to load database properties from classpath.
     *
     * @param  dbConfigurationKey  the key for loading database
     *                             configuration information.
     *
     * @return populated database properties object.
     *
     * @throws ExternalSystemException
     *   if the load fails.
     */
    private static Properties loadDatabaseProperties(String dbConfigurationKey)
            throws ExternalSystemException {
        Properties props = new Properties();
        final String propFileName = "/" + dbConfigurationKey + ".properties";
        InputStream dbIn = ImageDao.class.getResourceAsStream(propFileName);
        try {
            props.load(dbIn);
        } catch (Exception e) {
            throw new ExternalSystemException(
                    "Failed to load " + dbConfigurationKey +
                    " configuration properties.",
                    e);
        }

        String passwordKey = "db." + dbConfigurationKey + ".password";
        String encryptedPassword = props.getProperty(passwordKey);
        if ((encryptedPassword != null) && (encryptedPassword.length() > 0)) {
            try {
                StringEncrypter encrypter =
                        new StringEncrypter(
                                StringEncrypter.DES_ENCRYPTION_SCHEME,
                                StringEncrypter.DEFAULT_ENCRYPTION_KEY);
                String clearPassword = encrypter.decrypt(encryptedPassword);
                props.put(passwordKey, clearPassword);
            } catch (Exception e) {
                throw new ExternalSystemException(
                        "Failed to decrypt " + dbConfigurationKey +
                        " password.", e);
            }
        }

        return props;
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(ImageDao.class);

    /**
     * SQL for inserting an image record.
     *   Parameter 1 is the image's relative path.
     *   Parameter 2 is the image's capture date.
     */
    private static final String SQL_INSERT_IMAGE =
            "INSERT INTO image (name, capture_date, family) VALUES (?,?,?)";

    /**
     * SQL for retrieving an image id.
     *   Parameter 1 is the image's relative path.
     */
    private static final String SQL_SELECT_IMAGE_ID =
            "SELECT id FROM image WHERE name=?";

    /**
     * SQL for inserting an image property record.
     *   Parameter 1 is the image's id.
     *   Parameter 2 is the property type (name).
     *   Parameter 3 is the property value.
     */
    private static final String SQL_INSERT_IMAGE_PROPERTY =
            "INSERT INTO image_property (image_id, type, value) VALUES (?,?,?)";

    /**
     * SQL for retrieving the current (max) sequence number for a namespace.
     *   Parameter 1 is the namespace.
     */
    private static final String SQL_SELECT_SEQUENCE_NUMBER =
            "SELECT sequence_number FROM namespace_sequence_number " +
            "WHERE namespace=? FOR UPDATE";

    /**
     * SQL for inserting the first sequence number for a namespace.
     *   Parameter 1 is the namespace.
     */
    private static final String SQL_INSERT_SEQUENCE_NUMBER =
            "INSERT INTO namespace_sequence_number " +
            "(namespace, sequence_number) VALUES (?, 1)";

    /**
     * SQL for updating the max sequence number for a namespace.
     *   Parameter 1 is the new sequence number.
     *   Parameter 2 is the namespace.
     */
    private static final String SQL_UPDATE_SEQUENCE_NUMBER =
            "UPDATE namespace_sequence_number SET sequence_number=? " +
            "WHERE namespace=?";

}