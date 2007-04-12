/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.chacrm;

import static org.janelia.it.chacrm.Transformant.Status;
import org.janelia.it.ims.imagerenamer.plugin.CopyListener;
import org.janelia.it.ims.imagerenamer.plugin.ExternalDataException;
import org.janelia.it.ims.imagerenamer.plugin.ExternalSystemException;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRow;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRowValidator;

import java.io.File;

/**
 * This class handles events "published" by the image renamer tool.
 *
 * @author Eric Trautman
 */
public class RenamerEventManager implements RenameFieldRowValidator,
                                            CopyListener {

    /** The plugin data name for image location rank information. */
    private static final String IMAGE_LOCATION_RANK = "imageLocationRank";

    /** Standard message to verify components of transformant identifier. */
    private static final String MSG_VERIFY_TRANSFORMANT_COMPONENTS =
            "Please verify your Plate, Well, Vector ID, and " +
            "Landing Site settings for the file ";


    /** The data access object for retrieving and updating transformant data. */
    private TransformantDao dao;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.imagerenamer.config.PluginFactory}.
     */
    public RenamerEventManager() {
    }

    /**
     * Verifies that the plugin is ready for use by checking external
     * dependencies.
     *
     * @throws ExternalSystemException
     *   if the plugin or any of its dependencies are not available.
     */
    public void init() throws ExternalSystemException {
        try {
            setDao();
            dao.checkConnection();
        } catch (SystemException e) {
            throw new ExternalSystemException(
                    "Failed to initialize ChaCRM plugin.  " + e.getMessage(),
                    e);
        }
    }

    /**
     * Validates the set of rename information collected for
     * a specific file (row).
     * <p/>
     * The rename information is translated into a transformant identifier
     * which is used to access the ChaCRM database.
     * Validation then includes the following checks:
     * <ul>
     * <li> the transformant exists in the ChaCRM database </li>
     * <li> the transformant currently has a valid status </li>
     * </ul>
     *
     * @param  row  the user supplied rename information to be validated.
     *
     * @throws ExternalDataException
     *   if the data is not valid.
     *
     * @throws ExternalSystemException
     *   if any error occurs while validating the data.
     */
    public void validate(RenameFieldRow row)
            throws ExternalDataException, ExternalSystemException {

        String transformantID = null;
        Transformant transformant;
        try {
            transformantID = getTransformantID(row);
            transformant = dao.getTransformant(transformantID, false);
        } catch (TransformantNotFoundException e) {
            File fromFile = row.getFromFile();
            String fileName = fromFile.getName();
            throw new ExternalDataException(
                    "Transformant ID '" + transformantID +
                    "' does not exist in the ChaCRM database.  " +
                    MSG_VERIFY_TRANSFORMANT_COMPONENTS + fileName, e);
        } catch (SystemException e) {
            throw new ExternalSystemException(
                    "Failed to retrieve ChaCRM status for transformant ID '" +
                     transformantID + "' because of a system error.", e);
        }

        try {
            transformant.setStatus(Status.imaged);
        } catch (IllegalStateException e) {
            File fromFile = row.getFromFile();
            String fileName = fromFile.getName();
            throw new ExternalDataException(
                    "The ChaCRM status for transformant ID '" + transformantID +
                    "' is currently " + transformant.getStatus() +
                    " and may not be changed to a status of " + Status.imaged +
                    ".  " + MSG_VERIFY_TRANSFORMANT_COMPONENTS + fileName, e);
        }
    }

    /**
     * Processes the specified copy event.
     *
     * @param  eventType  type of copy event.
     * @param  row        details about the event.
     *
     * @return the rename field row for processing (with any
     *         updates from this plugin).
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public RenameFieldRow processEvent(EventType eventType,
                                       RenameFieldRow row)
            throws ExternalDataException, ExternalSystemException {
        switch (eventType) {
            case END_FAIL:
                failedCopy(row);
                break;
            case END_SUCCESS:
                completedSuccessfulCopy(row);
                break;
            case START:
                row = startingCopy(row);
                break;
        }
        return row;
    }

    /**
     * Processes start copy event.
     *
     * @param  row  the row information for the event.
     *
     * @return row information with updated rank.
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    private RenameFieldRow startingCopy(RenameFieldRow row)
            throws ExternalDataException, ExternalSystemException {

        String transformantID = null;
        try {
            transformantID = getTransformantID(row);
            Transformant transformant = dao.getTransformant(transformantID,
                                                            true);
            ImageLocation imageLocation = transformant.getImageLocation();
            Integer rank = imageLocation.getRank();
            row.setPluginDataValue(IMAGE_LOCATION_RANK, rank);
        } catch (TransformantNotFoundException e) {
            throw new ExternalDataException(
                    "Failed to retrieve transformant image rank " +
                    "because transformant ID " + transformantID +
                    " does not exist in the ChaCRM database.  " +
                    "Detailed data is: " + row, e);
        } catch (SystemException e) {
            throw new ExternalSystemException(
                    "Failed to retrieve ChaCRM image rank for " +
                    "transformant ID " + transformantID +
                    ".  Detailed data is: " + row, e);
        }

        return row;
    }

    /**
     * Processes completed copy successfully event.
     *
     * @param  row  the row information for the event.
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    private void completedSuccessfulCopy(RenameFieldRow row)
            throws ExternalDataException, ExternalSystemException {

        String transformantID = null;
        Status currentStatus = null;
        try {
            transformantID = getTransformantID(row);
            Transformant transformant = dao.getTransformant(transformantID,
                                                            false);
            currentStatus = transformant.getStatus();
            transformant.setStatus(Status.imaged);
            File renameFile = row.getRenamedFile();
            File renameDir = renameFile.getParentFile();
            String relativePath;
            if (renameDir == null) {
                relativePath = renameFile.getName();
            } else {
                relativePath = renameDir.getName() + "/" + renameFile.getName();
            }
            Integer rank = (Integer)
                    row.getPluginDataValue(IMAGE_LOCATION_RANK);
            ImageLocation imageLocation = new ImageLocation(relativePath, rank);
            transformant.setImageLocation(imageLocation);
            dao.setTransformantStatusAndLocation(transformant);
        } catch (TransformantNotFoundException e) {
            throw new ExternalDataException(
                    "Failed to update ChaCRM status because transformant ID " +
                    transformantID + " does not exist in the database.  " +
                    "Detailed data is: " + row, e);
        } catch (IllegalStateException e) {
            throw new ExternalDataException(
                    "The ChaCRM status for transformant ID " + transformantID +
                    " is currently " + currentStatus +
                    " and may not be changed to a status of " + Status.imaged +
                    ".  Detailed data is: " + row, e);
        } catch (Exception e) {
            throw new ExternalSystemException(
                    "Failed to update ChaCRM status for transformant ID " +
                     transformantID + ".  Detailed data is: " + row, e);
        }
    }

    /**
     * Processes completed copy failure event.
     *
     * @param  row  the row information for the event.
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    private void failedCopy(RenameFieldRow row)
            throws ExternalDataException, ExternalSystemException {
        try {
            String transformantID = getTransformantID(row);
            Transformant transformant = dao.getTransformant(transformantID,
                                                            false);
            Integer rank = (Integer)
                    row.getPluginDataValue(IMAGE_LOCATION_RANK);
            ImageLocation imageLocation = new ImageLocation("", rank);
            transformant.setImageLocation(imageLocation);
            dao.deleteImageLocation(transformant);
        } catch (TransformantNotFoundException e) {
            throw new ExternalDataException(
                    "Failed to delete transformant image location from ChaCRM" +
                    "database becasue the location could not be found.  " +
                    "Detailed data is: " + row, e);
        } catch (Exception e) {
            throw new ExternalSystemException(
                    "Failed to delete transformant image location from ChaCRM" +
                    "database.  Detailed data is: " + row, e);
        }
    }

    /**
     * Create the dao for this manager if it does not already exist.
     *
     * @throws SystemException
     *   if any error occurs during creation.
     */
    private synchronized void setDao() throws SystemException {
        if (dao == null) {
            dao = new TransformantDao();
        }
    }

    /**
     * Utility to translate renamer tool data fields into a
     * transformant identifier.
     *
     * @param  row  the set of renamer tool data fields.
     *
     * @return the corresponding transformant identifier.
     */
    private static String getTransformantID(RenameFieldRow row) {
        String transformantID;
        String plate = row.getFileNameValue("Plate");
        String well = row.getFileNameValue("Well");
        String vector = row.getFileNameValue("Vector ID");
        String insertionSite = row.getFileNameValue("Landing Site");
        transformantID =
                Transformant.constructTransformantID(plate,
                                                     well,
                                                     vector,
                                                     insertionSite);
        return transformantID;
    }
}
