/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowValidator;
import org.janelia.it.utils.StringUtil;

import java.io.File;
import java.util.Map;

/**
 * This plug-in validates that an image relative path exists in the
 * image database.
 *
 * @author Eric Trautman
 */
public class ImagePathExistsValidator implements RowValidator {

    private ImageDao dao;
    private boolean shouldExist;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public ImagePathExistsValidator() {
        this.shouldExist = true;
    }

    /**
     * Verifies that the plugin is ready for use by checking external
     * dependencies.
     *
     * @param  config  the plugin configuration.
     *
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config) throws ExternalSystemException {
        Map<String, String> props = config.getProperties();
        String dbConfigurationKey = props.get("db.config.key");

        if (StringUtil.isDefined(dbConfigurationKey)) {

            try {
                dao = new ImageDao(dbConfigurationKey);
                dao.checkAvailability();
            } catch (ExternalSystemException e) {
                throw new ExternalSystemException(
                        INIT_FAILURE_MSG +
                        e.getMessage(),
                        e);
            }

            String shouldExistValue = props.get("shouldExist");
            if ("false".equalsIgnoreCase(shouldExistValue)) {
                shouldExist = false;
            }
        } else {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "Please specify a value for the 'db.config.key' " +
                    "plug-in property.");
        }
    }

    /**
     * Validates that the specified row's relative path can be found in
     * the image database.
     *
     * @param  row  the user supplied information to be validated.
     *
     * @throws ExternalDataException
     *   if the data is not valid.
     *
     * @throws ExternalSystemException
     *   if any error occurs while validating the data.
     */
    public void validate(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        String relativePath;
        if (row instanceof RenamePluginDataRow) {
            File fromFile = ((RenamePluginDataRow) row).getFromFile();
            relativePath = PluginDataRow.getRelativePath(fromFile);
        } else {
            relativePath = row.getRelativePath();
        }

        try {
            Integer imageId = dao.getImageId(relativePath);
            if (shouldExist) {
                if (imageId == null) {
                    throw new ExternalDataException(
                            "The path '" + relativePath +
                            "' does not exist in the Image database.  " +
                            "Please only select images whose properties have " +
                            "previously been saved in the database.");
                }
            } else {
                if (imageId != null) {
                    throw new ExternalDataException(
                            "The path '" + relativePath +
                            "' already exists in the Image database.  " +
                            "Please only select images whose properties have " +
                            "not yet been saved in the database.");
                }
            }
        } catch (ExternalSystemException e) {
            throw new ExternalSystemException(
                    "Failed to retrieve the image identifier for '" +
                    relativePath + "' because of a system error.", e);
        }
    }

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize Image Data Exists Validator plug-in.  ";
}