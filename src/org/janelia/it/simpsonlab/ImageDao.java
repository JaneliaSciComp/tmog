/*
 * Copyright © 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.simpsonlab;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.utils.db.DbConfigException;
import org.janelia.it.utils.db.DbManager;
import org.janelia.it.utils.security.StringEncrypter;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * This class supports management of image data within the Simpson Lab
 * data repository.
 *
 * @author Eric Trautman
 */
public class ImageDao {

    /**
     * The manager used to establish connections with the ChaCRM repository.
     */
    private DbManager dbManager;

    /**
     * Constructs a dao using the default manager and configuration.
     *
     * @throws SystemException
     *   if the database configuration information cannot be loaded.
     */
    public ImageDao() throws SystemException {
        Properties props = loadDatabaseProperties();
        dbManager = new DbManager("simpsonlab", props);
    }

    /**
     * Value constructor.
     *
     * @param  dbManager  manager used to establish connections
     *                    with the ChaCRM repository.
     */
    public ImageDao(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Verifies that a connection to the database can be established.
     *
     * @throws SystemException
     *   if a connection to the database can not be established.
     */
    public void checkConnection() throws SystemException {
        Connection connection = null;
        try {
            connection = dbManager.getConnection();
        } catch (DbConfigException e) {
            throw new SystemException(e.getMessage(), e);
        } finally {
            DbManager.closeResources(null, null, connection, LOG);
        }
    }

    /**
     * Retrieves a the next line specimen number for the specified namespace.
     *
     * @param  line  the fly line of interest.
     *
     * @return the next line specimen number for the specified namespace.
     *
     * @throws SystemException
     *   if errors occur while retrieving the data.
     */
    public int getNextSpecimenNumber(Line line)
            throws SystemException {

        int nextNumber = 1;
        String namespace = line.getSpecimenNamespace();
        int rowsUpdated;

        Connection connection = null;
        ResultSet selectResultSet = null;
        PreparedStatement select = null;
        PreparedStatement increment = null;

        try {
            connection = dbManager.getConnection();
            connection.setAutoCommit(false);
            select = connection.prepareStatement(SQL_SELECT_SPECIMEN_NUMBER);
            select.setString(1, namespace);
            selectResultSet = select.executeQuery();
            if (selectResultSet.next()) {
                Integer currentNumber = selectResultSet.getInt(1);
                nextNumber = currentNumber + 1;
                increment =
                        connection.prepareStatement(SQL_UPDATE_SPECIMEN_NUMBER);
                increment.setInt(1, nextNumber);
                increment.setString(2, namespace);
                rowsUpdated = increment.executeUpdate();
                if (rowsUpdated != 1) {
                    throw new SystemException(
                            "Failed to update next specimen number for " +
                            namespace + ".  Attempted to update " +
                            rowsUpdated + " rows.");
                }
            } else {
                increment =
                        connection.prepareStatement(SQL_INSERT_SPECIMEN_NUMBER);
                increment.setString(1, namespace);
                rowsUpdated = increment.executeUpdate();
                if (rowsUpdated != 1) {
                    throw new SystemException(
                            "Failed to create specimen number for " +
                            namespace + ".  Attempted to create " +
                            rowsUpdated + " rows.");
                }
            }

            connection.commit();

        } catch (DbConfigException e) {
            throw new SystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new SystemException(
                    "Failed to determine next specimen number for " +
                    namespace, e);
        } finally {
            DbManager.closeResources(selectResultSet, select, null, LOG);
            DbManager.closeResources(null, increment, connection, LOG);
        }

        return nextNumber;
    }

    public Image addImage(Image image) throws SystemException {
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
                throw new SystemException(
                        "Failed to create image '" + relativePath +
                        "'.  Attempted to create " + rowsUpdated + " rows.");
            }

            select = connection.prepareStatement(SQL_SELECT_IMAGE_ID);
            select.setString(1, relativePath);
            selectResultSet = select.executeQuery();
            if (selectResultSet.next()) {
                imageId = selectResultSet.getInt(1);
            } else {
                throw new SystemException(
                        "Failed to retrieve id for image '" + relativePath +
                        "'.");
            }

            insertProperty =
                    connection.prepareStatement(SQL_INSERT_IMAGE_PROPERTY);
            List<ImageProperty> properties = image.getProperties();
            int numberOfPropertiesToAdd = 0;
            String value;
            for (ImageProperty property : properties) {
                value = property.getValue();
                if ((value != null) && (value.length() > 0)) {
                    insertProperty.setInt(1, imageId);
                    insertProperty.setString(2, property.getColumnName());
                    insertProperty.setString(3, value);
                    insertProperty.addBatch();
                    numberOfPropertiesToAdd++;
                }
            }

            if (numberOfPropertiesToAdd > 0) {
                int[] numUpdates = insertProperty.executeBatch();
                if (numUpdates.length != numberOfPropertiesToAdd) {
                    throw new SystemException(
                            "Failed to add image properties for '" +
                            relativePath + "'.  Executed " +
                            numUpdates.length + " instead of " +
                            numberOfPropertiesToAdd + " statement(s).");
                } else {
                    for (int i = 0; i < numUpdates.length; i++) {
                        if (numUpdates[i] != 1) {
                            throw new SystemException(
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
            throw new SystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new SystemException(
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
     * Utility to load database properties from classpath.
     *
     * @return populated database properties object.
     *
     * @throws SystemException
     *   if the load fails.
     */
    public static Properties loadDatabaseProperties()
            throws SystemException {
        Properties props = new Properties();
        final String propFileName = "/simpsonlab.properties";
        InputStream dbIn =
                ImageDao.class.getResourceAsStream(propFileName);
        try {
            props.load(dbIn);
        } catch (IOException e) {
            throw new SystemException(
                    "Failed to load specimen number retrieval configuration properties.",
                    e);
        }

        String passwordKey = "db.simpsonlab.password";
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
                throw new SystemException(
                        "Failed to decrypt simpsonlab password.", e);
            }
        }

        return props;
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(ImageDao.class);

    /**
     * SQL for retrieving the current (max) specimen number for a line.
     *   Parameter 1 is the line specimen namespace.
     */
    private static final String SQL_SELECT_SPECIMEN_NUMBER =
            "SELECT sequence_number FROM namespace_sequence_number " +
            "WHERE namespace=? FOR UPDATE";

    /**
     * SQL for inserting the first specimen number for a line.
     *   Parameter 1 is the line specimen namespace.
     */
    private static final String SQL_INSERT_SPECIMEN_NUMBER =
            "INSERT INTO namespace_sequence_number " +
            "(namespace, sequence_number) VALUES (?, 1)";

    /**
     * SQL for updating the max specimen number for a line.
     *   Parameter 1 is the new specimen number.
     *   Parameter 2 is the line specimen namespace.
     */
    private static final String SQL_UPDATE_SPECIMEN_NUMBER =
            "UPDATE namespace_sequence_number SET sequence_number=? " +
            "WHERE namespace=?";

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

}