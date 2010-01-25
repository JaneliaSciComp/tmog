/*
 * Copyright 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.chacrm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.janelia.it.chacrm.Transformant.Status;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.utils.db.AbstractDao;
import org.janelia.it.utils.db.DbConfigException;
import org.janelia.it.utils.db.DbManager;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * This class supports management of transformant data within the ChaCRM
 * data repository.
 *
 * @author R. Svirskas
 * @author Eric Trautman
 */
public class TransformantDao extends AbstractDao {

    /** The logger for this class. */
    private static final Log LOG =
            LogFactory.getLog(TransformantDao.class);

    /**
     * SQL for retrieving transformant object data.
     *   Parameter 1 is transformant name.
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
            "{ call store_transformant_image_location(?, ?, ?, ?) }";

    /**
     * SQL for removing an image location and updating any associated
     * transformant and fragment status if necessary.
     *   Parameter 1 is transformant image location.
     */
    private static final String SQL_CALL_REMOVE_IMAGE_LOCATION =
            "{ call remove_transformant_image_location(?, ?) }";

    /**
     * SQL for deleting a transformant image location without changing
     * the associated transformant or fragment status.
     *   Parameter 1 is transformant feature id.
     *   Parameter 2 is transformant image rank.
     */
    private static final String SQL_DELETE_TRANSFORMANT_IMAGE_LOCATION =
            "DELETE from featureprop WHERE feature_id=? AND rank=? AND " +
            "type_id=(SELECT cvterm_id FROM cvterm WHERE name = " +
            "'image_location' AND is_obsolete = 0)";

    /**
     * Constructs a dao using the default manager and configuration.
     *
     * @throws ExternalSystemException
     *   if the database configuration information cannot be loaded.
     */
    public TransformantDao() throws ExternalSystemException {
        super("chacrm");
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
     * @throws ExternalSystemException
     *   if any other errors occur while retrieving the data.
     */
    public Transformant getTransformant(String transformantID,
                                        boolean isNewImageLocationNeeded)
            throws TransformantNotFoundException, ExternalSystemException {

        Transformant transformant = null;
        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement pStatement = null;
        CallableStatement cStatement = null;

        try {
            final DbManager dbManager = getDbManager();
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
            throw new ExternalSystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ExternalSystemException(
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
     * @param  user          person responsible for changing the status.
     *
     * @throws ExternalSystemException
     *   if any errors occur while persiting the data.
     * @throws IllegalArgumentException
     *   if a null transformant or image location is specified.
     */
    public void setTransformantStatusAndLocation(Transformant transformant,
                                                 User user)
            throws ExternalSystemException, IllegalArgumentException {

        if (transformant == null) {
            throw new IllegalArgumentException("Transformant is null.");
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
            final DbManager dbManager = getDbManager();
            connection = dbManager.getConnection();
            statement = connection.prepareCall(SQL_CALL_IMAGE_TRANSFORMANT);
            Integer featureID = transformant.getFeatureID();
            statement.setInt(1, featureID);
            statement.setString(2, imageLocation.getRelativePath());
            statement.setInt(3, imageLocation.getRank());
            statement.setString(4, user.getName());

            statement.executeUpdate();

            if (LOG.isInfoEnabled()) {
                LOG.info("successfully set status for " + transformant +
                         ", " + user);
            }
            
        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ExternalSystemException(
                    "Failed to update status and image location " +
                    "in database for transformant: " +
                    transformant, e);
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }
    }

    /**
     * Deletes the image location for the specified transformat
     * from the repository without changing the associated transformant
     * or fragment status values.
     *
     * @param  transformant  transformant with location to be deleted.
     *
     * @throws TransformantNotFoundException
     *   if the transformant image location cannot be found.
     * @throws ExternalSystemException
     *   if any other errors occur while deleting the data.
     * @throws IllegalArgumentException
     *   if a null transformant or image location is specified.
     */
    public void deleteImageLocation(Transformant transformant)
            throws TransformantNotFoundException, ExternalSystemException,
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
            final DbManager dbManager = getDbManager();
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
            throw new ExternalSystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ExternalSystemException(
                    "Failed to delete image location for " + transformant, e);
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }
    }

    /**
     * Deletes the specified image location from the repository and
     * updates the associated transformant and fragment status values
     * if necessary.
     *
     * @param  imageLocation  image location to be removed.
     * @param  user           person responsible for removing the location.
     *
     * @throws ExternalSystemException
     *   if any errors occur while removing the data.
     * @throws IllegalArgumentException
     *   if a image location is specified.
     */
    public void deleteImageLocationAndRollbackStatus(ImageLocation imageLocation,
                                                     User user)
            throws ExternalSystemException, IllegalArgumentException {

        if (imageLocation == null) {
            throw new IllegalArgumentException("Image location is null.");
        }

        Connection connection = null;
        ResultSet resultSet = null;
        CallableStatement statement = null;
        try {
            final DbManager dbManager = getDbManager();
            connection = dbManager.getConnection();
            statement = connection.prepareCall(SQL_CALL_REMOVE_IMAGE_LOCATION);
            statement.setString(1, imageLocation.getRelativePath());
            statement.setString(2, user.getName());

            statement.executeUpdate();

            if (LOG.isInfoEnabled()) {
                LOG.info("successfully removed " + imageLocation + ", " + user);
            }

        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ExternalSystemException(
                    "Failed to remove image location: " +
                    imageLocation, e);
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }
    }
}
