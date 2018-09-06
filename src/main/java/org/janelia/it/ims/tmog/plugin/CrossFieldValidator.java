/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.janelia.it.ims.tmog.config.PluginConfiguration;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class validates one field's value based upon another field's value.
 *
 * @author Eric Trautman
 */
public class CrossFieldValidator
        extends SimpleRowValidator {

    /**
     * Name of the property that identifies the field to validate.
     */
    public static final String VALIDATE_FIELD_NAME = "validateField";

    /**
     * Name of the property that identifies the pattern for valid values.
     */
    public static final String MATCHES_PATTERN_NAME = "matchesPattern";

    /**
     * Name of the property that identifies the reference field.
     */
    public static final String WHEN_REFERENCE_FIELD_NAME = "whenReferenceField";

    /**
     * Name of the property that identifies the pattern a reference field
     * must match to trigger validation.
     */
    public static final String MATCHES_REFERENCE_PATTERN_NAME =
            "matchesReferencePattern";

    /**
     * Name of the property that identifies the error message template
     * to display when validation fails.
     */
    public static final String ERROR_MESSAGE_NAME = "errorMessage";


    private String validateFieldName;
    private Pattern validationPattern;
    private String referenceFieldName;
    private Pattern referencePattern;

    /** Parsed configuration tokens for invalid resource error messages. */
    private PropertyTokenList errorMessageTokens;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public CrossFieldValidator() {
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

        validateFieldName = getRequiredProperty(VALIDATE_FIELD_NAME, config);
        final String validationPatternString =
                getRequiredProperty(MATCHES_PATTERN_NAME, config);
        validationPattern = compilePattern(validationPatternString,
                                           MATCHES_PATTERN_NAME);
        referenceFieldName =
                getRequiredProperty(WHEN_REFERENCE_FIELD_NAME, config);
        final String referencePatternString =
                getRequiredProperty(MATCHES_REFERENCE_PATTERN_NAME, config);
        referencePattern = compilePattern(referencePatternString,
                                          MATCHES_REFERENCE_PATTERN_NAME);
        final String errorMessage =
                getRequiredProperty(ERROR_MESSAGE_NAME, config);
        try {
            errorMessageTokens = new PropertyTokenList(errorMessage,
                                                       config.getProperties());
        } catch (Exception e) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MESSAGE + e.getMessage(), e);
        }

    }

    /**
     * Validate the configured field for the specified row.
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

        final String referenceValue = row.getCoreValue(referenceFieldName);
        if (referenceValue != null) {

            final Matcher referenceMatcher =
                    referencePattern.matcher(referenceValue);

            if (referenceMatcher.matches()) {

                boolean isValid = false;

                final String fieldValue = row.getCoreValue(validateFieldName);
                if (fieldValue != null) {
                    final Matcher fieldMatcher =
                            validationPattern.matcher(fieldValue);
                    isValid = fieldMatcher.matches();
                }

                if (! isValid) {
                    List<String> msgList =
                            errorMessageTokens.deriveValues(
                                    row.getDisplayNameToFieldMap(),
                                    false);
                    StringBuilder message = new StringBuilder(128);
                    for (String msg : msgList) {
                        message.append(msg);
                    }
                    throw new ExternalDataException(message.toString());
                }
            }
        }

    }

    private String getRequiredProperty(String propertyName,
                                       PluginConfiguration config)
            throws ExternalSystemException {
        final String value = config.getProperty(propertyName);
        if ((value == null) || (value.length() == 0)) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MESSAGE + "The '" +
                    propertyName + "' property must be defined.");
        }
        return value;
    }

    private Pattern compilePattern(String patternString,
                                   String propertyName)
            throws ExternalSystemException {
        Pattern pattern;
        try {
            pattern = Pattern.compile(patternString);
        } catch (Exception e) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MESSAGE +
                    "Invalid " + propertyName + " '" +
                    patternString + "' specified.  " +
                    e.getMessage(),
                    e);
        }
        return pattern;
    }

    private static final String INIT_FAILURE_MESSAGE =
            "Failed to initialize CrossFieldValidator plugin.  ";

}