/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.VerifiedTextModel;

import java.util.List;
import java.util.Map;

/**
 * This class validates a text value composed of multiple fields.
 *
 * @author Eric Trautman
 */
public class MultipleFieldTextValidator
        extends SimpleRowValidator {

    /** Parsed configuration tokens for deriving a row specific value. */
    private PropertyTokenList valueTokens;

    private String errorMessage;

    /** Model used to verify derived values. */
    private VerifiedTextModel model;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public MultipleFieldTextValidator() {
    }

    /**
     * Verifies that the plugin is ready for use.
     *
     * @param  config  the plugin configuration.
     *
     * @throws org.janelia.it.ims.tmog.plugin.ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config) throws ExternalSystemException {

        final String value =
                getRequiredProperty("value", config);
        final Integer minimumLength =
                getIntegerProperty("minimumLength", config);
        final String pattern =
                config.getProperty("pattern");
        final Integer maximumLength =
                getIntegerProperty("maximumLength", config);
        final boolean isRequired =
                Boolean.parseBoolean(config.getProperty("required"));

        this.errorMessage = getRequiredProperty("errorMessage", config);

        try {
            this.valueTokens = new PropertyTokenList(value,
                                                     config.getProperties());
            this.model = new VerifiedTextModel();
            this.model.setMinimumLength(minimumLength);
            this.model.setMaximumLength(maximumLength);
            this.model.setPattern(pattern);
            this.model.setRequired(isRequired);
        } catch (Exception e) {
            throw new ExternalSystemException(INIT_FAILURE + e.getMessage(),
                                              e);
        }
    }

    /**
     * Validates derived value(s) for the current row.
     *
     * @param  sessionName  unique name for session being validated.
     * @param  row          the user supplied information to be validated.
     *
     * @throws ExternalDataException
     *   if the data is not valid.
     *
     * @throws ExternalSystemException
     *   if any error occurs while validating the data.
     */
    public void validate(String sessionName,
                         PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        VerifiedTextModel validationModel = model.getNewInstance(true);
        final Map<String, DataField> fieldMap = row.getDisplayNameToFieldMap();
        final List<String> values = valueTokens.deriveValues(fieldMap, false);

        for (String value : values) {
            validationModel.setText(value);
            if (! validationModel.verify()) {
                throw new ExternalDataException(
                        errorMessage + "  " +
                        validationModel.getErrorMessage());
            }
        }

    }

    private String getRequiredProperty(String propertyName,
                                       PluginConfiguration config)
            throws ExternalSystemException {
        final String value = config.getProperty(propertyName);
        if ((value == null) || (value.length() == 0)) {
            throw new ExternalSystemException(
                    INIT_FAILURE + "The '" + propertyName +
                    "' property must be defined.");
        }
        return value;
    }

    private Integer getIntegerProperty(String propertyName,
                                       PluginConfiguration config)
            throws ExternalSystemException {
        Integer value = null;
        final String valueStr = config.getProperty(propertyName);
        if ((valueStr != null) && (valueStr.trim().length() > 0)) {
            try {
                value = Integer.parseInt(valueStr);
            } catch (NumberFormatException e) {
                throw new ExternalSystemException(
                        INIT_FAILURE + "The '" + propertyName +
                        "' property must be an integer.", e);
            }
        }
        return value;
    }

    private static final String INIT_FAILURE =
            "Failed to initialize MultipleFieldTextValidator plugin.  ";
}