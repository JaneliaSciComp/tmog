/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.config.ConfigurationException;
import org.janelia.it.ims.tmog.field.PluginDefaultValue;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.Target;
import org.janelia.it.utils.StringUtil;

import java.io.File;
import java.util.Map;

/**
 * This class encapsulates a default field value that is based upon
 * an image property value.
 *
 * @author Eric Trautman
 */
public class ImageDataDefaultValue
        implements PluginDefaultValue {

    private String propertyName;
    private ImageDataCache dataCache;

    public ImageDataDefaultValue() {
    }

    public void init(Map<String, String> properties)
            throws ConfigurationException {

        final String family = properties.get("family");
        if (! StringUtil.isDefined(family)) {
            throw new ConfigurationException(
                    INIT_FAILURE_MSG +
                    "Please specify a value for the 'family' " +
                    "plug-in property.");
        }

        this.propertyName = properties.get("image_property");
        if (! StringUtil.isDefined(this.propertyName)) {
            throw new ConfigurationException(
                    INIT_FAILURE_MSG +
                    "Please specify a value for the 'image_property' " +
                    "plug-in property.");
        }

        try {
            this.dataCache = ImageDataCache.getCache(getDbConfigurationKey(),
                                                     family);
        } catch (ExternalSystemException e) {
            throw new ConfigurationException(
                    INIT_FAILURE_MSG +
                    e.getMessage(),
                    e);
        }

    }

    public String getValue(Target target) {
        String value = null;
        if (target instanceof FileTarget) {
            final FileTarget fileTarget = (FileTarget) target;
            final File file = fileTarget.getFile();
            final String relativePath = PluginDataRow.getRelativePath(file);
            value = dataCache.getValue(relativePath, propertyName);
        }
        return value;
    }

    protected String getDbConfigurationKey() {
        return "nighthawk";
    }

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize Image Data Default Value plug-in.  ";
}