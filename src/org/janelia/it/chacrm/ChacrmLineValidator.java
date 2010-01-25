/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.chacrm;

import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PluginUtil;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowValidator;

import java.io.File;

/**
 * This class validates that lines entered with GMR_ prefixes actually
 * exist in the ChaCRM database.
 *
 * @author Eric Trautman
 */
public class ChacrmLineValidator
        implements RowValidator {

    /** The data access object for retrieving and updating transformant data. */
    private TransformantDao dao;

    /** The name of the property that contains the ChaCRM line. */
    private String linePropertyName = "Line";

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public ChacrmLineValidator() {
    }

    /**
     * Verifies that the plugin is ready for use.
     *
     * @param  config  the plugin configuration.
     *
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config) throws ExternalSystemException {
        try {
            setDao();
            dao.checkAvailability();
            String configuredLinePropertyName =
                    config.getProperty("linePropertyName");
            if (configuredLinePropertyName != null) {
                this.linePropertyName = configuredLinePropertyName;
            }
        } catch (ExternalSystemException e) {
            throw new ExternalSystemException(
                    "Failed to initialize ChacrmLineValidator plugin.  " +
                    e.getMessage(),
                    e);
        }
    }

    /**
     * If the row identifies a GMR line, validate that the line
     * exists in ChaCRM.
     *
     * @param  row  the user supplied meta-data to be validated.
     *
     * @throws ExternalDataException
     *   if the data is not valid.
     *
     * @throws ExternalSystemException
     *   if any error occurs while validating the data.
     */
    public void validate(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        RenamePluginDataRow dataRow = PluginUtil.castRenameRow(row, this);
        String transformantID = null;
        try {
            transformantID = getTransformantID(dataRow);
            if (transformantID != null) {
                dao.getTransformant(transformantID, false);
            }
        } catch (TransformantNotFoundException e) {
            File fromFile = dataRow.getFromFile();
            String fileName = fromFile.getName();
            throw new ExternalDataException(
                    "Transformant ID '" + transformantID +
                    "' does not exist in the ChaCRM database.  " +
                    "Please verify the " + linePropertyName +
                    " specified for the file " +
                    fileName, e);
        } catch (ExternalSystemException e) {
            throw new ExternalSystemException(
                    "Failed to retrieve ChaCRM status for transformant ID '" +
                    transformantID + "' because of a system error.", e);
        }

    }

    /**
     * Utility to parse a transformant identifier from the line property.
     *
     * @param  row  the set of entered data fields.
     *
     * @return the corresponding transformant identifier or null if the
     *         line does not fit the ChaCRM line format.
     */
    private String getTransformantID(PluginDataRow row) {
        String transformantID = null;
        String line = row.getCoreValue(linePropertyName);
        if ((line != null) && line.startsWith("GMR_") && (line.length() > 4)) {
            transformantID = line.substring(4);
        }
        return transformantID;
    }

    /**
     * Create the dao for this plug-in if it does not already exist.
     *
     * @throws ExternalSystemException
     *   if any error occurs during creation.
     */
    private synchronized void setDao() throws ExternalSystemException {
        if (dao == null) {
            dao = new TransformantDao();
        }
    }

}