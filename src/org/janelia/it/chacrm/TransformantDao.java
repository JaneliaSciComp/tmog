/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.chacrm;

import static org.janelia.it.chacrm.Transformant.Status;

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
import java.util.Properties;

/**
 * This class supports management of transformant data within the ChaCRM
 * data repository.
 *
 * @author R. Svirskas
 * @author Eric Trautman
 */
public class TransformantDao {

    /** The logger for this class. */
    private static final Log LOG =
            LogFactory.getLog(TransformantDao.class);

    /** SQL for retrieving the owner type identifier. */
    private static final String SQL_SELECT_OWNER_TYPE_ID =
            "SELECT cvterm_id FROM cvterm WHERE " +
            "name = 'owner' AND is_obsolete = 0";

    /** SQL for retrieving the image location type identifier. */
    private static final String SQL_SELECT_IMAGE_LOCATION_TYPE_ID =
            "SELECT cvterm_id FROM cvterm WHERE " +
            "name = 'image_location' AND is_obsolete = 0";

    /**
     * SQL for retrieving transformant object data.
     *   Parameter 1 is transformant ID.
     */
    private static final String SQL_SELECT_TRANSFORMANT =
            "SELECT owner, feature_id FROM feature_owner WHERE " +
            "feature_id=(SELECT feature_id FROM feature WHERE name=?)";

    /**
     * SQL for updating transformant status.
     *   Parameter 1 is transformant status.
     *   Parameter 2 is transformant feature id.
     */
    private static final String SQL_UPDATE_TRANSFORMANT_STATUS =
            "UPDATE featureprop SET value=? WHERE " +
            "feature_id=? AND type_id=(" + SQL_SELECT_OWNER_TYPE_ID + ")";

    /**
     * SQL for retrieving the feature id of a fragment associated with
     * a transformant.
     *   Parameter 1 is transformant feature id.
     */
    private static final String SQL_SELECT_FRAGMENT_FEATURE_ID =
            "SELECT value FROM featureprop WHERE " +
            "feature_id=? AND " +
            "type_id=(SELECT cvterm_id FROM cvterm WHERE " +
                       "name='tiling_path_fragment_id' AND is_obsolete=0)";

    /**
     * SQL for updating fragment status.
     *   Parameter 1 is transformant status.
     *   Parameter 2 is transformant feature id.
     */
    private static final String SQL_UPDATE_FRAGMENT_STATUS =
            "UPDATE featureprop SET value=? WHERE " +
            "feature_id=(" + SQL_SELECT_FRAGMENT_FEATURE_ID + ") AND " +
            "type_id=(" + SQL_SELECT_OWNER_TYPE_ID + ")";

    /**
     * SQL for updating fragment status.
     *   Parameter 1 is transformant feature id.
     *   Parameter 2 is image location.
     *   Parameter 3 is transformant feature id.
     */
    private static final String SQL_UPDATE_TRANSFORMANT_IMAGE_LOCATION =
            "INSERT INTO featureprop(feature_id, type_id, value, rank) " +
            "VALUES (?, (" + SQL_SELECT_IMAGE_LOCATION_TYPE_ID + "), ?, " +
            "(SELECT (case when max(rank) is null then 0 else max(rank) + 1 end) " +
            "FROM featureprop WHERE feature_id=? AND type_id=(" +
            SQL_SELECT_IMAGE_LOCATION_TYPE_ID + ")))";

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
    public TransformantDao() throws SystemException {
        Properties props = loadChaCRMDatabaseProperties();
        dbManager = new DbManager("chacrm", props);
    }

    /**
     * Value constructor.
     *
     * @param  dbManager  manager used to establish connections
     *                    with the ChaCRM repository.
     */
    public TransformantDao(DbManager dbManager) {
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
     * Retrieves a transformat from the repository based on the
     * specified identifier.
     *
     * @param  transformantID  identifies the transformant to be retrieved.
     *
     * @return a populated transformant object.
     *
     * @throws TransformantNotFoundException
     *   if no transformat exists with the specified identifier.
     * @throws SystemException
     *   if any other errors occur while retrieving the data.
     */
    public Transformant getTransformant(String transformantID)
            throws TransformantNotFoundException, SystemException {

        Transformant transformant = null;
        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement statement = null;
        try {
            connection = dbManager.getConnection();
            statement = connection.prepareStatement(SQL_SELECT_TRANSFORMANT);
            statement.setString(1, transformantID);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String statusStr = resultSet.getString(1);
                Status status = null;
                try {
                    status = Status.valueOf(statusStr);
                } catch (IllegalArgumentException e) {
                    LOG.warn("Retrieved unknown transformant status from " +
                             "database (" + statusStr +
                             ") for transformant with ID " + transformantID +
                             ".  Setting object status to null.", e);
                }
                Integer featureID = resultSet.getInt(2);
                transformant = new Transformant(transformantID,
                                                status,
                                                featureID);
            } else {
                throw new TransformantNotFoundException(
                        TransformantNotFoundException.getStandardMessage(
                                transformantID));
            }
        } catch (DbConfigException e) {
            throw new SystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new SystemException(
                    "Failed to retrieve transformant from database with ID: " +
                    transformantID, e);
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }

        return transformant;
    }

    /**
     * Updates the specified transformant's status in the repository.
     *
     * @param  transformant  transformat whose status should be persisted.
     *
     * @return the transformant object with any updates from the repository.
     *
     * @throws TransformantNotFoundException
     *   if the specified transformat does not exist in the repository.
     * @throws SystemException
     *   if any other errors occur while persiting the data.
     * @throws IllegalArgumentException
     *   if a null transformant is specified.
     */
    public Transformant setTransformantStatus(Transformant transformant)
            throws TransformantNotFoundException, SystemException,
                   IllegalArgumentException {

        if (transformant == null) {
            throw new IllegalArgumentException(
                    "Attempting to update a null transformant object.");
        }

        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement statement = null;
        try {
            connection = dbManager.getConnection();
            statement =
                    connection.prepareStatement(SQL_UPDATE_TRANSFORMANT_STATUS);
            Status status = transformant.getStatus();
            statement.setString(1, status.toString());
            Integer featureID = transformant.getFeatureID();
            statement.setInt(2, featureID);
            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated == 0) {
                throw new TransformantNotFoundException(transformant);
            } else if (rowsUpdated > 1) {
                LOG.warn("Transformant status update for " + transformant +
                         " caused " + rowsUpdated +
                         " rows to be updated instead of just one.");
            }

            statement =
                    connection.prepareStatement(SQL_UPDATE_FRAGMENT_STATUS);
            statement.setString(1, status.toString());
            statement.setInt(2, featureID);
            rowsUpdated = statement.executeUpdate();

            if (rowsUpdated == 0) {
                throw new SystemException(
                        "The fragment associated with " + transformant +
                        " can not be found in the ChaCRM database.  " +
                        "Status for the transformant has been updated.  " +
                        "Please verify the integrity of the transformant " +
                        "to fragment relationship.");
            } else if (rowsUpdated > 1) {
                LOG.warn("Fragment status update for " + transformant +
                         " caused " + rowsUpdated +
                         " rows to be updated instead of just one.");
            }

            String imageLocation = transformant.getImageLocation();

            if ((imageLocation != null) && (imageLocation.length() > 0)) {
                statement = connection.prepareStatement(
                        SQL_UPDATE_TRANSFORMANT_IMAGE_LOCATION);
                statement.setInt(1, featureID);
                statement.setString(2, imageLocation);
                statement.setInt(3, featureID);
                rowsUpdated = statement.executeUpdate();

                if (rowsUpdated == 0) {
                    throw new SystemException(
                            "The image location for " + transformant +
                            " can not be set in the ChaCRM database.  " +
                            "Status for the transformant has been updated.");
                }
            }

            if (LOG.isInfoEnabled()) {
                LOG.info("successfully set status for " + transformant);
            }
            
        } catch (DbConfigException e) {
            throw new SystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new SystemException(
                    "Failed to update status in database for transformant: " +
                    transformant, e);
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }

        return transformant;
    }

    /**
     * Utility to load ChaCRM database properties from classpath.
     *
     * @return populated database properties object.
     *
     * @throws SystemException
     *   if the load fails.
     */
    public static Properties loadChaCRMDatabaseProperties()
            throws SystemException {
        Properties props = new Properties();
        final String propFileName = "/chacrm.properties";
        InputStream dbIn =
                TransformantDao.class.getResourceAsStream(propFileName);
        try {
            props.load(dbIn);
        } catch (IOException e) {
            throw new SystemException(
                    "Failed to load ChaCRM database configuration properties.",
                    e);
        }

        String passwordKey = "db.chacrm.password";
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
                        "Failed to decrypt ChaCRM database password.", e);
            }
        }

        return props;
    }

    public static void main(String[] args) {
        System.out.println("SQL_SELECT_TRANSFORMANT:\n  " +
                SQL_SELECT_TRANSFORMANT);
        System.out.println("\nSQL_UPDATE_TRANSFORMANT_STATUS:\n  " +
                SQL_UPDATE_TRANSFORMANT_STATUS);
        System.out.println("\nSQL_UPDATE_FRAGMENT_STATUS:\n  " +
                SQL_UPDATE_FRAGMENT_STATUS);
        System.out.println("\nSQL_UPDATE_TRANSFORMANT_IMAGE_LOCATION:\n  " +
                SQL_UPDATE_TRANSFORMANT_IMAGE_LOCATION);
    }
}
