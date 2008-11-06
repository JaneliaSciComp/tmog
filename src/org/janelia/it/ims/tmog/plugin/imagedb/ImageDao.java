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
import org.janelia.it.utils.StringUtil;
import org.janelia.it.utils.db.DbConfigException;
import org.janelia.it.utils.db.DbManager;
import org.janelia.it.utils.security.StringEncrypter;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * This class supports management of image data within an image database.
 *
 * @author Eric Trautman
 */
public class ImageDao implements ImagePropertyWriter {

    /**
     * The manager used to establish connections with the database.
     */
    private DbManager dbManager;

    private String dbConfigurationKey;

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
        this.dbManager = new DbManager(dbConfigurationKey, props);
        this.dbConfigurationKey = dbConfigurationKey;
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
    public void checkAvailability() throws ExternalSystemException {
        Connection connection = null;
        try {
            connection = dbManager.getConnection();
        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } finally {
            DbManager.closeResources(null, null, connection, LOG);
        }
    }

    /**
     * Writes (inserts or updates) the specified image properties
     * to the image database.
     *
     * @param  image  image to be persisted.
     *
     * @return the specified image with its database id updated.
     *
     * @throws ExternalSystemException
     *   if the save fails.
     */
    public Image saveProperties(Image image) throws ExternalSystemException {
        String relativePath = image.getRelativePath();

        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement select = null;

        try {
            connection = dbManager.getConnection();
            connection.setAutoCommit(false);

            select = connection.prepareStatement(SQL_SELECT_IMAGE_PROPERTY_TYPES);
            select.setString(1, relativePath);
            resultSet = select.executeQuery();

            Integer imageId = null;
            Integer lastImageId = null;
            List<String> existingPropertyNames = new ArrayList<String>();
            String propertyName;
            while (resultSet.next()) {
                imageId = resultSet.getInt(1);
                if ((lastImageId != null) && (! lastImageId.equals(imageId))) {
                    throw new ExternalSystemException(
                            "Database contains multiple images with the " +
                            "same relative path: '"  + relativePath + "'.");
                }
                propertyName = resultSet.getString(2);
                if (StringUtil.isDefined(propertyName)) {
                    existingPropertyNames.add(propertyName);
                }
                lastImageId = imageId;
            }

            if (imageId == null) {
                image = addImage(image, connection);
            } else {
                image.setId(imageId);
                updateImage(image, connection);
                if (existingPropertyNames.size() > 0) {
                    updateImageProperties(image,
                                          connection,
                                          existingPropertyNames);
                }
            }

            connection.commit();

        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ExternalSystemException(
                    "Failed to store image properties for '" +
                    relativePath + "'.", e);
        } finally {
            DbManager.closeResources(resultSet, select, connection, LOG);
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("successfully persisted image properties to the '" +
                     dbConfigurationKey + "' database: " + image);
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
     * @param  relativePath  the relative path for the desired image.
     *
     * @return the image identifier for the specified path
     *         (or null if the path is not found).
     *
     * @throws ExternalSystemException
     *   if the retrieval fails.
     */
    public Integer getImageId(String relativePath) throws ExternalSystemException {

        Integer imageId = null;
        Connection connection = null;

        try {
            connection = dbManager.getConnection();
            imageId = getImageId(relativePath, connection);
        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ExternalSystemException(
                    "Failed to retrieve image id for '" +
                    relativePath + "'.", e);
        } finally {
            DbManager.closeResources(null, null, connection, LOG);
        }

        LOG.info("getImageId: returning " + imageId + " for " + relativePath);
        
        return imageId;
    }

    private Integer getImageId(String relativePath,
                               Connection connection)
            throws SQLException, ExternalSystemException {

        Integer imageId = null;
        PreparedStatement select = null;
        ResultSet resultSet = null;

        try {
            select = connection.prepareStatement(SQL_SELECT_IMAGE_ID);
            select.setString(1, relativePath);
            resultSet = select.executeQuery();
            if (resultSet.next()) {
                imageId = resultSet.getInt(1);
            }
        } finally {
            DbManager.closeResources(resultSet, select, null, LOG);
        }

        return imageId;
    }

    private Image addImage(Image image,
                          Connection connection)
            throws SQLException, ExternalSystemException {

        String relativePath = image.getRelativePath();

        PreparedStatement insertImage = null;
        try {
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
            insertImage.setBoolean(4, image.isDisplay());

            int rowsUpdated = insertImage.executeUpdate();
            if (rowsUpdated != 1) {
                throw new ExternalSystemException(
                        "Failed to create image '" + relativePath +
                        "'.  Attempted to create " + rowsUpdated + " rows.");
            }

            Integer imageId = getImageId(relativePath, connection);
            if (imageId == null) {
                throw new ExternalSystemException(
                        "Failed to retrieve id for image '" + relativePath +
                        "'.");
            }
            image.setId(imageId);
            updateImageProperties(image, connection, new ArrayList<String>());

        } finally {
            DbManager.closeResources(null, insertImage, null, LOG);
        }

        return image;
    }

    private void updateImage(Image image,
                             Connection connection)
            throws SQLException, ExternalSystemException {

        String relativePath = image.getRelativePath();

        PreparedStatement updateImage = null;
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("UPDATE image SET ");

            Date captureDate = image.getCaptureDate();
            if (captureDate != null) {
                sql.append("capture_date=?, ");
            }

            String family = image.getFamily();
            if (family != null) {
                sql.append("family=?, ");
            }

            sql.append("display=? WHERE id=?");

            int columnIndex = 1;
            updateImage = connection.prepareStatement(sql.toString());
            if (captureDate != null) {
                updateImage.setDate(columnIndex,
                                    new java.sql.Date(captureDate.getTime()));
                columnIndex++;
            }
            if (family != null) {
                updateImage.setString(columnIndex, image.getFamily());
                columnIndex++;
            }
            updateImage.setBoolean(columnIndex, image.isDisplay());
            columnIndex++;
            updateImage.setInt(columnIndex, image.getId());

            int rowsUpdated = updateImage.executeUpdate();
            if (rowsUpdated != 1) {
                throw new ExternalSystemException(
                        "Failed to update image '" + relativePath +
                        "'.  Attempted to update " + rowsUpdated + " rows.");
            }

            LOG.info("updateImage: successfully executed " + sql.toString());

        } finally {
            DbManager.closeResources(null, updateImage, null, LOG);
        }
    }

    private void updateImageProperties(Image image,
                                       Connection connection,
                                       List<String> existingPropertyNames)
            throws SQLException, ExternalSystemException {

        int imageId = image.getId();
        String relativePath = image.getRelativePath();

        Set<String> insertPropertyNames = new LinkedHashSet<String>();
        Set<String> updatePropertyNames = new LinkedHashSet<String>();

        Map<String, String> properties = image.getPropertyTypeToValueMap();
        for (String type : properties.keySet()) {
            if (StringUtil.isDefined(type)) {
                if (existingPropertyNames.contains(type)) {
                    updatePropertyNames.add(type);
                } else {
                    insertPropertyNames.add(type);
                }
            }
        }

        int numberOfPropertiesToAdd = insertPropertyNames.size();
        if (numberOfPropertiesToAdd > 0) {
            PreparedStatement insertProperty = null;

            try {

                insertProperty =
                        connection.prepareStatement(SQL_INSERT_IMAGE_PROPERTY);
                for (String type : insertPropertyNames) {
                    insertProperty.setInt(1, imageId);
                    insertProperty.setString(2, type);
                    insertProperty.setString(3, properties.get(type));
                    insertProperty.addBatch();
                }

                int[] numUpdates = insertProperty.executeBatch();
                validateUpdateCounts("Failed to add image properties for '" +
                                     relativePath + "'.",
                                     numberOfPropertiesToAdd,
                                     numUpdates);

                LOG.info("updateImageProperties: successfully added " +
                         insertPropertyNames);

            } finally {
                DbManager.closeResources(null, insertProperty, null, LOG);
            }
        }

        int numberOfPropertiesToUpdate = updatePropertyNames.size();
        if (numberOfPropertiesToUpdate > 0) {
            PreparedStatement updateProperty = null;

            try {

                updateProperty =
                        connection.prepareStatement(SQL_UPDATE_IMAGE_PROPERTY);
                for (String type : updatePropertyNames) {
                    updateProperty.setString(1, properties.get(type));
                    updateProperty.setInt(2, imageId);
                    updateProperty.setString(3, type);
                    updateProperty.addBatch();
                }

                int[] numUpdates = updateProperty.executeBatch();
                validateUpdateCounts("Failed to update image properties for '" +
                                     relativePath + "'.",
                                     numberOfPropertiesToUpdate,
                                     numUpdates);

                LOG.info("updateImageProperties: successfully updated " +
                         updatePropertyNames);

            } finally {
                DbManager.closeResources(null, updateProperty, null, LOG);
            }
        }

    }

    private void validateUpdateCounts(String failureContext,
                                      int expectedNumberOfUpdates,
                                      int[] numUpdates)
            throws ExternalSystemException {
        if (numUpdates.length != expectedNumberOfUpdates) {
            throw new ExternalSystemException(
                    failureContext + "  Executed " +
                    numUpdates.length + " instead of " +
                    expectedNumberOfUpdates + " statement(s).");
        } else {
            for (int i = 0; i < numUpdates.length; i++) {
                if (numUpdates[i] != 1) {
                    throw new ExternalSystemException(
                            failureContext + "  Attempted to update " +
                            numUpdates[i] + " row(s) for property " +
                            i + ".");
                }
            }
        }
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
     *   Parameter 3 is the image's family.
     *   Parameter 4 is the image's display flag.
     */
    private static final String SQL_INSERT_IMAGE =
            "INSERT INTO image (name, capture_date, family, display) " +
            "VALUES (?,?,?,?)";

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
     * SQL for inserting an image property record.
     *   Parameter 1 is the property value.
     *   Parameter 2 is the image's id.
     *   Parameter 3 is the property type (name).
     */
    private static final String SQL_UPDATE_IMAGE_PROPERTY =
            "UPDATE image_property SET value=? WHERE image_id=? AND type=?";

    /**
     * SQL for retrieving an image's property types.
     *   Parameter 1 is the image's relative path.
     */
    private static final String SQL_SELECT_IMAGE_PROPERTY_TYPES =
            "SELECT i.id, ip.type FROM image i " +
            "LEFT OUTER JOIN image_property ip ON i.id=ip.image_id " +
            "WHERE i.name=?"; 

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