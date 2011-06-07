/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.chacrm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PluginUtil;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowListener;
import org.janelia.it.ims.tmog.plugin.RowValidator;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.janelia.it.chacrm.Transformant.Status;

/**
 * This class handles events "published" by the image transmogrifier tool.
 *
 * @author Eric Trautman
 */
public class ChacrmEventManager implements RowValidator, RowListener {

    /** The logger for this class. */
    private static final Log LOG =
            LogFactory.getLog(ChacrmEventManager.class);

    /**
     * The plugin data name for image location rank information.
     */
    private static final String IMAGE_LOCATION_RANK = "imageLocationRank";

    /**
     * Standard message to verify components of transformant identifier.
     */
    private static final String MSG_VERIFY_TRANSFORMANT_COMPONENTS =
            "Please verify your Plate, Well, Vector ID, and " +
            "Landing Site settings for the file ";

    /**
     * The data access object for retrieving and updating transformant data.
     */
    private TransformantDao dao;

    /**
     * The configured pattern value used to identify when files are being
     * renamed from one area of the lab image file share to another.
     */
    private String labImageSharePatternValue;

    /** The compiled pattern for the labImageSharePatternValue. */
    private Pattern labImageSharePattern;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public ChacrmEventManager() {
    }

    /**
     * Verifies that the plugin is ready for use by checking external
     * dependencies.
     *
     * @param config the plugin configuration.
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config) throws ExternalSystemException {
        try {
            setDao();
            dao.checkAvailability();
            labImageSharePatternValue =
                    config.getProperty("labImageSharePattern");
            if (labImageSharePatternValue != null) {
                labImageSharePattern =
                        Pattern.compile(labImageSharePatternValue);
            }
        } catch (PatternSyntaxException pse) {
            throw new ExternalSystemException(
                    "Failed to initialize ChaCRM plugin because of invalid " +
                    "labImageSharePattern '" + labImageSharePatternValue +
                    "'.  " + pse.getMessage(), pse);
        } catch (ExternalSystemException e) {
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
     * @param row the user supplied rename information to be validated.
     * @throws ExternalDataException   if the data is not valid.
     * @throws ExternalSystemException if any error occurs while validating the data.
     */
    public void validate(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        RenamePluginDataRow dataRow = PluginUtil.castRenameRow(row, this);
        String transformantID = null;
        Transformant transformant;
        try {
            transformantID = getTransformantID(dataRow);
            transformant = dao.getTransformant(transformantID, false);
        } catch (TransformantNotFoundException e) {
            File fromFile = dataRow.getFromFile();
            String fileName = fromFile.getName();
            throw new ExternalDataException(
                    "Transformant ID '" + transformantID +
                    "' does not exist in the ChaCRM database.  " +
                    MSG_VERIFY_TRANSFORMANT_COMPONENTS + fileName, e);
        } catch (ExternalSystemException e) {
            throw new ExternalSystemException(
                    "Failed to retrieve ChaCRM status for transformant ID '" +
                    transformantID + "' because of a system error.", e);
        }

        try {
            transformant.setStatus(Status.imaged);
        } catch (IllegalStateException e) {
            File fromFile = dataRow.getFromFile();
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
    public RenamePluginDataRow processEvent(EventType eventType,
                                            PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        RenamePluginDataRow dataRow = PluginUtil.castRenameRow(row, this);
        switch (eventType) {
            case END_ROW_FAIL:
                failedCopy(dataRow);
                break;
            case END_ROW_SUCCESS:
                completedSuccessfulCopy(dataRow);
                break;
            case START_ROW:
                dataRow = startingCopy(dataRow);
                break;
        }
        return dataRow;
    }

    /**
     * Utility to extract an image location from the specified file name
     * based on the specified base directory pattern.
     *
     * @param  fileName              file name to check.
     *
     * @param  baseDirectoryPattern  pattern of the base directory to be
     *                               excluded from the relative image location.
     *
     * @return a relative image location if the base pattern is matched;
     *         otherwise null.
     */
    public static ImageLocation getImageLocation(String fileName,
                                                 Pattern baseDirectoryPattern) {
        ImageLocation imageLocation = null;

        if (baseDirectoryPattern != null) {
            Matcher m = baseDirectoryPattern.matcher(fileName);
            if (m.find()) {
                int relativePathStart = m.end();
                String relativePath = fileName.substring(relativePathStart);
                imageLocation = new ImageLocation(relativePath, null);
            }
        }

        return imageLocation;
    }

    /**
     * Processes start copy event.
     *
     * @param row the row information for the event.
     * @return row information with updated rank.
     * @throws ExternalDataException   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException if a non-recoverable system error occurs during processing.
     */
    private RenamePluginDataRow startingCopy(RenamePluginDataRow row)
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
        } catch (ExternalSystemException e) {
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
     * @param row the row information for the event.
     * @throws ExternalDataException   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException if a non-recoverable system error occurs during processing.
     */
    private void completedSuccessfulCopy(RenamePluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        String transformantID = null;
        Status currentStatus = null;
        try {
            transformantID = getTransformantID(row);
            Transformant transformant = dao.getTransformant(transformantID,
                                                            false);
            currentStatus = transformant.getStatus();
            transformant.setStatus(Status.imaged);
            String relativePath = row.getRelativePath();
            Integer rank = (Integer)
                    row.getPluginDataValue(IMAGE_LOCATION_RANK);
            ImageLocation imageLocation = new ImageLocation(relativePath, rank);
            transformant.setImageLocation(imageLocation);

            User user = User.getUser(row);
            dao.setTransformantStatusAndLocation(transformant, user);

            File fromFile = row.getFromFile();
            String fromFileName = fromFile.getAbsolutePath();
            ImageLocation fromFileImageLocation =
                    getImageLocation(fromFileName,
                                     labImageSharePattern);
            if (fromFileImageLocation != null) {
                try {
                    dao.deleteImageLocationAndRollbackStatus(
                            fromFileImageLocation,
                            user);
                } catch (Exception e) {
                    // log this error, but allow transaction to complete
                    LOG.error("failed to remove " + fromFileImageLocation +
                              " for file " + fromFileName +
                              " from ChaCRM database", e);
                }
            }

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
     * @param row the row information for the event.
     * @throws ExternalDataException   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException if a non-recoverable system error occurs during processing.
     */
    private void failedCopy(RenamePluginDataRow row)
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
     * @throws ExternalSystemException
     *   if any error occurs during creation.
     */
    private synchronized void setDao() throws ExternalSystemException {
        if (dao == null) {
            dao = new TransformantDao();
        }
    }

    /**
     * Utility to translate transmogrifier tool data fields into a
     * transformant identifier.
     *
     * @param row the set of transmogrifier tool data fields.
     * @return the corresponding transformant identifier.
     */
    public static String getTransformantID(PluginDataRow row) {
        String transformantID;
        String plate = row.getCoreValue("Plate");
        String well = row.getCoreValue("Well");
        String vector = row.getCoreValue("Vector ID");
        String insertionSite = row.getCoreValue("Landing Site");
        transformantID =
                Transformant.constructTransformantID(plate,
                                                     well,
                                                     vector,
                                                     insertionSite);
        return transformantID;
    }
}
