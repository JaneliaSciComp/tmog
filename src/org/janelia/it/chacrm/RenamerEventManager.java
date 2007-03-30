/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.chacrm;

import static org.janelia.it.chacrm.Transformant.Status;
import org.janelia.it.ims.imagerenamer.plugin.ExternalSystemException;
import org.janelia.it.ims.imagerenamer.plugin.ExternalDataException;
import org.janelia.it.ims.imagerenamer.plugin.CopyCompleteInfo;
import org.janelia.it.ims.imagerenamer.plugin.CopyCompleteListener;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRowValidator;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRow;

import java.io.File;

/**
 * This class handles events "published" by the image renamer tool.
 *
 * @author Eric Trautman
 */
public class RenamerEventManager implements RenameFieldRowValidator,
                                            CopyCompleteListener {

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
            transformant = dao.getTransformant(transformantID);
        } catch (TransformantNotFoundException e) {
            File fromFile = row.getFromFile();
            String fileName = fromFile.getName();
            throw new ExternalDataException(
                    "Transformant ID '" + transformantID +
                    "' does not exist in the ChaCRM database.  " +
                    "Please verify your Plate, Well, Vector ID, and " +
                    "Insertion Site settings for the file " +
                    fileName, e);
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
                    ".  Please verify your Plate, Well, Vector ID, and " +
                    "Insertion Site settings for the file " +
                    fileName, e);
        }
    }

    /**
     * Updates the associated transformant status to imaged.
     *
     * @param  info  details about the event.
     *
     * @throws ExternalDataException
     *   if the transformant does not exist or has an invalid status.
     *
     * @throws ExternalSystemException
     *   if any other error occurs during processing.
     */
    public void completedSuccessfulCopy(CopyCompleteInfo info)
            throws ExternalDataException, ExternalSystemException {

        String transformantID = null;
        Status currentStatus = null;
        try {
            transformantID = getTransformantID(info);
            Transformant transformant = dao.getTransformant(transformantID);
            currentStatus = transformant.getStatus();
            transformant.setStatus(Status.imaged);
            File renameFile = info.getToFile();
            File renameDir = renameFile.getParentFile();
            String imageLoc;
            if (renameDir == null) {
                imageLoc = renameFile.getName();                
            } else {
                imageLoc = renameDir.getName() + "/" + renameFile.getName();
            }
            transformant.setImageLocation(imageLoc);
            dao.setTransformantStatus(transformant);
        } catch (TransformantNotFoundException e) {
            throw new ExternalDataException(
                    "Failed to update ChaCRM status because transformant ID " +
                    transformantID + " does not exist in the database.  " +
                    "Copy information is: " + info, e);
        } catch (IllegalStateException e) {
            throw new ExternalDataException(
                    "The ChaCRM status for transformant ID " + transformantID +
                    " is currently " + currentStatus +
                    " and may not be changed to a status of " + Status.imaged +
                    ".  Copy information is: " + info, e);
        } catch (SystemException e) {
            throw new ExternalSystemException(
                    "Failed to update ChaCRM status for transformant ID " +
                     transformantID + ".  Copy information is: " + info, e);
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
        String insertionSite = row.getFileNameValue("Insertion Site");
        transformantID =
                Transformant.constructTransformantID(plate,
                                                     well,
                                                     vector,
                                                     insertionSite);
        return transformantID;
    }

}
