/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.dataFile;

import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PropertyTokenList;
import org.janelia.it.ims.tmog.plugin.RowUpdater;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sets the value of one field based upon the values of other fields.
 *
 * @author Eric Trautman
 */
public class DerivedFieldUpdaterPlugin
        implements RowUpdater {

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize Derived Field Updater plug-in.  ";

    private String forFieldName;
    private PropertyTokenList setValueTo;
    private String whenFieldName;
    private Pattern matchesPattern;

    @Override
    public void init(PluginConfiguration config)
            throws ExternalSystemException {

        this.forFieldName = getRequiredProperty("forFieldName", config);
        this.setValueTo = new PropertyTokenList(getRequiredProperty("setValueTo", config), config.getProperties());
        this.whenFieldName = getRequiredProperty("whenFieldName", config);
        this.matchesPattern = Pattern.compile(getRequiredProperty("matchesPattern", config));
    }

    @Override
    public PluginDataRow updateRow(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        final String sourceValue = row.getCoreValue(whenFieldName);
        final Matcher m = matchesPattern.matcher(sourceValue);
        if (m.matches()) {
            final List<String> updatedValues = setValueTo.deriveValues(row.getDisplayNameToFieldMap(), false);
            row.applyPluginDataValue(forFieldName,
                                     updatedValues.get(0));
        }
        return row;
    }

    private String getRequiredProperty(String propertyName,
                                       PluginConfiguration config)
            throws ExternalSystemException {
        final String value = config.getProperty(propertyName);
        if ((value == null) || (value.length() == 0)) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "The '" +
                    propertyName + "' property must be defined.");
        }
        return value;
    }


}