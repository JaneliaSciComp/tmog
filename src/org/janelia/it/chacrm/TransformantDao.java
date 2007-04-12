/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.chacrm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.janelia.it.chacrm.Transformant.Status;
import org.janelia.it.utils.db.DbConfigException;
import org.janelia.it.utils.db.DbManager;
import org.janelia.it.utils.security.StringEncrypter;

import java.io.IOException;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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

    /**
     * SQL for retrieving transformant object data.
     *   Parameter 1 is transformant ID.
     */
    private static final String SQL_SELECT_TRANSFORMANT =
            "SELECT owner, feature_id FROM feature_owner WHERE " +
            "feature_id=(SELECT feature_id FROM feature WHERE name=?)";

    /**
     * SQL for inserting a new image location row and retrieving the
     * corresponding rank.
     *   Parameter 1 is the returned image location rank.
     *   Parameter 2 is transformant feature id.
     */
    private static final String SQL_CALL_ADD_TRANSFORMANT_IMAGE_LOCATION =
            "{ ? = call store_transformant_image_location(?) }";

    /**
     * SQL for updating transformant status and image location.
     *   Parameter 1 is transformant feature id.
     *   Parameter 2 is transformant image location.
     *   Parameter 3 is transformant image rank.
     */
    private static final String SQL_CALL_IMAGE_TRANSFORMANT =
            "{ call store_transformant_image_location(?, ?, ?) }";

    /**
     * SQL for updating transformant status and image location.
     *   Parameter 1 is transformant feature id.
     *   Parameter 2 is transformant image rank.
     */
    private static final String SQL_DELETE_TRANSFORMANT_IMAGE_LOCATION =
            "DELETE from featureprop WHERE feature_id=? AND rank=? AND " +
            "type_id=(SELECT cvterm_id FROM cvterm WHERE name = " +
            "'image_location' AND is_obsolete = 0)";

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
     * @param  transformantID            identifies the transformant
     *                                   to be retrieved.
     *
     * @param  isNewImageLocationNeeded  if true, an new image location
     *                                   will be allocated and returned.
     *
     * @return a populated transformant object.
     *
     * @throws TransformantNotFoundException
     *   if no transformat exists with the specified identifier.
     * @throws SystemException
     *   if any other errors occur while retrieving the data.
     */
    public Transformant getTransformant(String transformantID,
                                        boolean isNewImageLocationNeeded)
            throws TransformantNotFoundException, SystemException {

        Transformant transformant = null;
        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement pStatement = null;
        CallableStatement cStatement = null;

        try {
            connection = dbManager.getConnection();
            pStatement = connection.prepareStatement(SQL_SELECT_TRANSFORMANT);
            pStatement.setString(1, transformantID);
            resultSet = pStatement.executeQuery();
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

            if (isNewImageLocationNeeded) {
                cStatement = connection.prepareCall(
                        SQL_CALL_ADD_TRANSFORMANT_IMAGE_LOCATION);
                cStatement.registerOutParameter(1, Types.INTEGER);
                cStatement.setInt(2, transformant.getFeatureID());
                cStatement.execute();
                int rank = cStatement.getInt(1);
                ImageLocation imageLocation = new ImageLocation("", rank);
                transformant.setImageLocation(imageLocation);
            }
        } catch (DbConfigException e) {
            throw new SystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new SystemException(
                    "Failed to retrieve transformant from database with ID: " +
                    transformantID, e);
        } finally {
            DbManager.closeResources(resultSet, pStatement, null, LOG);
            DbManager.closeResources(null, cStatement, connection, LOG);
        }

        return transformant;
    }

    /**
     * Updates the specified transformant's status and image location
     * in the repository.
     *
     * @param  transformant  transformant to be updated.
     *
     * @throws SystemException
     *   if any errors occur while persiting the data.
     * @throws IllegalArgumentException
     *   if a null transformant or image location is specified.
     */
    public void setTransformantStatusAndLocation(Transformant transformant)
            throws SystemException, IllegalArgumentException {

        if (transformant == null) {
            throw new IllegalArgumentException(
                    "Transformant is null.");
        }

        ImageLocation imageLocation = transformant.getImageLocation();
        if (imageLocation == null) {
            throw new IllegalArgumentException(
                    "Transformant image location is null.");
        }

        Connection connection = null;
        ResultSet resultSet = null;
        CallableStatement statement = null;
        try {
            connection = dbManager.getConnection();
            statement = connection.prepareCall(SQL_CALL_IMAGE_TRANSFORMANT);
            Integer featureID = transformant.getFeatureID();
            statement.setInt(1, featureID);
            statement.setString(2, imageLocation.getRelativePath());
            statement.setInt(3, imageLocation.getRank());

            statement.executeUpdate();

            if (LOG.isInfoEnabled()) {
                LOG.info("successfully set status for " + transformant);
            }
            
        } catch (DbConfigException e) {
            throw new SystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new SystemException(
                    "Failed to update status and image location " +
                    "in database for transformant: " +
                    transformant, e);
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }
    }

    /**
     * Deletes the image location for the specified transformat
     * from the repository.
     *
     * @param  transformant  transformant with location to be deleted.
     *
     * @throws TransformantNotFoundException
     *   if the transformant image location cannot be found.
     * @throws SystemException
     *   if any other errors occur while deleting the data.
     * @throws IllegalArgumentException
     *   if a null transformant or image location is specified.
     */
    public void deleteImageLocation(Transformant transformant)
            throws TransformantNotFoundException, SystemException,
                   IllegalArgumentException {

        if (transformant == null) {
            throw new IllegalArgumentException(
                    "Transformant is null.");
        }

        ImageLocation imageLocation = transformant.getImageLocation();
        if (imageLocation == null) {
            throw new IllegalArgumentException(
                    "Transformant image location is null.");
        }

        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement statement = null;

        try {
            connection = dbManager.getConnection();
            statement = connection.prepareStatement(
                    SQL_DELETE_TRANSFORMANT_IMAGE_LOCATION);
            statement.setInt(1, transformant.getFeatureID());
            statement.setInt(2, imageLocation.getRank());

            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated == 0) {
                throw new TransformantNotFoundException(
                        "Failed to delete image location for " + transformant);
            } else if (rowsUpdated == 1) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("deleted image location for " + transformant);
                }
            } else {
                LOG.warn(rowsUpdated + " rows were deleted for " +
                         transformant);
            }

        } catch (DbConfigException e) {
            throw new SystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new SystemException(
                    "Failed to delete image location for " + transformant, e);
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }
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
}
