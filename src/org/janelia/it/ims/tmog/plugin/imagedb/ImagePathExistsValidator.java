/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RelativePathUtil;
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
    private int relativePathDepth;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public ImagePathExistsValidator() {
        this.shouldExist = true;
        this.relativePathDepth = 1;
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

            String depthString = props.get("relativePathDepth");
            if (StringUtil.isDefined(depthString)) {
                try {
                    this.relativePathDepth = Integer.parseInt(depthString);
                } catch (NumberFormatException e) {
                    throw new ExternalSystemException(
                            INIT_FAILURE_MSG + "Please specify a valid " +
                            "'relativePathDepth' plug-in property.", e);
                }
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

        File file;
        if (row instanceof RenamePluginDataRow) {
            file = ((RenamePluginDataRow) row).getFromFile();
        } else {
            file = row.getTargetFile();
        }

        final String relativePath =
                RelativePathUtil.getRelativePath(file, relativePathDepth);
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