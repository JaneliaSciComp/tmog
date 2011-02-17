/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This class encapsulates a token parsed from a plug-in configuration
 * property.  It's {@link #getValue(PluginDataRow)} method allows
 * dynamic data row values to be retrieved based upon the token name.
 *
 * The {@link #parseTokens(String)} utility method is provided to parse
 * a configuration property value into a list of tokens.
 *
 * @author Eric Trautman
 */
public class PropertyToken {

    public static final String TOKEN_ID = "${";

    public static List<PropertyToken> parseTokens(String tokenString)
            throws IllegalArgumentException {

        ArrayList<PropertyToken> tokenList = new ArrayList<PropertyToken>();

        if ((tokenString == null) || (tokenString.length() == 0)) {
            throw new IllegalArgumentException(
                    INVALID_SYNTAX +
                    "Empty value configured.");
        }

        if (tokenString.endsWith(TOKEN_ID)) {
            throw new IllegalArgumentException(
                    INVALID_SYNTAX +
                    "Token start '${' is missing closing '}' in '" +
                    tokenString + "'.");
        }

        int start = tokenString.indexOf(TOKEN_ID);
        Scanner scanner = null;
        if (start == -1) {
            tokenList.add(new PropertyToken(true, tokenString));
        } else if (start == 0) {
            scanner = new Scanner(tokenString);
        } else {
            tokenList.add(new PropertyToken(true,
                                            tokenString.substring(0, start)));
            scanner = new Scanner(tokenString.substring(start));
        }

        if (scanner != null) {
            scanner.useDelimiter("\\$\\{");

            int stop;
            String current;
            while (scanner.hasNext()) {
                current = scanner.next();
                stop = current.indexOf('}');
                if (stop == -1) {
                    throw new IllegalArgumentException(
                            INVALID_SYNTAX +
                            "Token start '${' is missing closing '}' in '" +
                            tokenString + "'.");
                } else if (stop == 0) {
                    throw new IllegalArgumentException(
                            INVALID_SYNTAX +
                            "Empty token '${}' specified in '" +
                            tokenString + "'.");
                } else {
                    tokenList.add(
                            new PropertyToken(false,
                                              current.substring(0, stop)));
                    start = stop + 1;
                    if (start < current.length()) {
                        tokenList.add(
                                new PropertyToken(true,
                                                  current.substring(start)));
                    }
                }
            }
        }

        return tokenList;
    }

    public static String deriveString(PluginDataRow row,
                                      List<PropertyToken> list) {
        StringBuilder sb = new StringBuilder();
        for (PropertyToken token : list) {
            sb.append(token.getValue(row));
        }
        return sb.toString();
    }

    private boolean isLiteral = false;
    private String value;
    private String prefix;
    private String suffix;

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
                            INVALID_SYNTAX +
                            "Token prefix is missing closing quote for ${" +
                            value + "}.");
                }
            }

            if (valueStart == valueStop) {
                throw new IllegalArgumentException(
                        INVALID_SYNTAX +
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
                            INVALID_SYNTAX +
                            "Token suffix is missing opening quote for ${" +
                            value + "}.");
                }
            }

            if (valueStop <= valueStart) {
                throw new IllegalArgumentException(
                        INVALID_SYNTAX +
                        "Token is missing in ${" + value + "}.");
            }

            this.value = value.substring(valueStart, valueStop);
        }
    }

    public boolean isLiteral() {
        return isLiteral;
    }

    public String getValue() {
        return value;
    }

    public String getValue(PluginDataRow row) {
        String derivedValue;
        if (isLiteral) {
            derivedValue = value;
        } else {
            derivedValue = row.getCoreValue(value);
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

    private static final String INVALID_SYNTAX =
            "Invalid composite syntax.  ";
}

