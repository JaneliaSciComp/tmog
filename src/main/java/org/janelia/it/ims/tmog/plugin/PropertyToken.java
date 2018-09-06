/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.janelia.it.ims.tmog.field.DataField;

import java.util.Map;

/**
 * This class encapsulates a token parsed from a plug-in configuration
 * property.
 *
 * @author Eric Trautman
 */
public class PropertyToken {

    public static final String TOKEN_ID = "${";

    private boolean isLiteral = false;
    private String value;
    private String prefix;
    private String suffix;

    /**
     * Constructs a token for a standard (non-group) field.
     *  
     * @param  literal  true if the value is a literal string;
     *                  false if it identifies a field name.
     *
     * @param  value    the token value.
     */
    public PropertyToken(boolean literal,
                         String value) {
        isLiteral = literal;
        if (isLiteral) {
            this.value = value;
            this.prefix = null;
            this.suffix = null;
        } else {

            int valueStart = 0;
            int valueStop = value.length();

            if (value.startsWith("'")) {
                int prefixStop = value.indexOf('\'', 1);
                if (prefixStop > 0) {
                    this.prefix = value.substring(1, prefixStop);
                    valueStart = prefixStop + 1;
                } else {
                    throw new IllegalArgumentException(
                            "Token prefix is missing closing quote for ${" +
                            value + "}.");
                }
            }

            if (valueStart == valueStop) {
                throw new IllegalArgumentException(
                        "Token is missing in ${" + value + "}.");
            }

            if (value.endsWith("'")) {
                int suffixStart = value.indexOf('\'', valueStart) + 1;
                if ((suffixStart > 0) && (suffixStart < valueStop)) {
                    this.suffix = value.substring(suffixStart,
                                                  (valueStop - 1));
                    valueStop = suffixStart - 1;
                } else {
                    throw new IllegalArgumentException(
                            "Token suffix is missing opening quote for ${" +
                            value + "}.");
                }
            }

            if (valueStop <= valueStart) {
                throw new IllegalArgumentException(
                        "Token is missing in ${" + value + "}.");
            }

            this.value = value.substring(valueStart, valueStop);
        }
    }

    /**
     * @return true if this token's value is a literal string;
     *         false if the token's value identifies a field name.
     */
    public boolean isLiteral() {
        return isLiteral;
    }

    /**
     * @return this token's raw value.
     */
    public String getValue() {
        return value;
    }

    /**
     * @param  nameToFieldMap  map of field names to instances for
     *                         value derivation.
     *
     * @param  index           the row index for the desired value
     *                         (only relevant for field groups).
     *
     * @return the derived value for this token based upon the specified map.
     *         If the token is literal, the raw value is simply returned.
     */
    public String getValue(Map<String, DataField> nameToFieldMap,
                           int index) {
        String derivedValue = null;
        if (isLiteral) {
            derivedValue = value;
        } else {
            final DataField field = nameToFieldMap.get(value);
            if (field != null) {
                derivedValue = field.getCoreValue();
            }
            if ((derivedValue != null) && (derivedValue.length() > 0)) {
                if (prefix != null) {
                    derivedValue = prefix + derivedValue;
                }
                if (suffix != null) {
                    derivedValue = derivedValue + suffix;
                }
            }
        }
        return derivedValue;
    }

}

