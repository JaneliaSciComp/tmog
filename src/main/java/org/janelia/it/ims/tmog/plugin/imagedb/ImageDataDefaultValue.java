/*
 * Copyright (c) 2018 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.config.ConfigurationException;
import org.janelia.it.ims.tmog.field.PluginDefaultValue;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.RelativePathUtil;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.Target;
import org.janelia.it.utils.StringUtil;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class encapsulates a default field value that is based upon
 * an image property value.
 *
 * @author Eric Trautman
 */
public class ImageDataDefaultValue
        implements PluginDefaultValue {

    private String propertyName;
    private int relativePathDepth;
    private boolean useBaseNameSearch;
    private Pattern baseNamePattern;
    private ImageDataCache dataCache;

    public ImageDataDefaultValue() {
        this.relativePathDepth = 1;
        this.useBaseNameSearch = false;
        this.baseNamePattern = DEFAULT_BASE_NAME_PATTERN;
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

        String depth = properties.get("relativePathDepth");
        if (StringUtil.isDefined(depth)) {
            try {
                this.relativePathDepth = Integer.parseInt(depth);
            } catch (NumberFormatException e) {
                throw new ConfigurationException(
                        INIT_FAILURE_MSG +
                        "Please specify a valid value for the " +
                        "'relative_path_depth' plug-in property.", e);
            }
        }

        final String baseNameSearch = properties.get("baseNameSearch");
        if (StringUtil.isDefined(baseNameSearch)) {
            this.useBaseNameSearch = Boolean.valueOf(baseNameSearch);
        }

        final String baseNamePatternString = properties.get("baseNamePattern");
        if (StringUtil.isDefined(baseNamePatternString)) {
            try {
                this.baseNamePattern = Pattern.compile(baseNamePatternString);
            } catch (Exception e) {
                throw new ConfigurationException(
                        INIT_FAILURE_MSG +
                        "Please specify a valid value for the " +
                        "'baseNamePattern' plug-in property.", e);
            }
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

            String relativePath = null;
            if (useBaseNameSearch) {
                Matcher m = baseNamePattern.matcher(file.getName());
                if (m.matches() && (m.groupCount() == 1)) {
                    relativePath = '%' + m.group(1) + '%';
                }
            }

            if (relativePath == null) {
                relativePath =
                    RelativePathUtil.getRelativePath(file, relativePathDepth);
            }

            value = dataCache.getValue(relativePath, propertyName);
        }
        return value;
    }

    private String getDbConfigurationKey() {
        return "sage";
    }

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize Image Data Default Value plug-in.  ";

    private static final Pattern DEFAULT_BASE_NAME_PATTERN =
            Pattern.compile("(?:^.*_|^)([A-Z]+_\\d{17}_\\d+).*");
}