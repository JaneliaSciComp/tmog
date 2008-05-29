/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.plugin.PluginDataRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This class sets the image property using a composite of text and
 * field names.
 *
 * @author Eric Trautman
 */
public class CompositeSetter implements ImagePropertySetter {

    public static final String TOKEN_ID = "${";

    private String propertyType;
    private List<Token> tokens;

    public CompositeSetter(String propertyType,
                           String compositeFieldString)
            throws IllegalArgumentException {

        this.propertyType = propertyType;
        this.tokens = parseTokens(compositeFieldString);
    }

    public void setProperty(PluginDataRow row,
                            Image image) {
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) {
            sb.append(token.getValue(row));
        }
        if (sb.length() > 0) {
            image.addProperty(propertyType, sb.toString());
        }
    }

    protected List<Token> parseTokens(String compositeFieldString)
            throws IllegalArgumentException {

        ArrayList<Token> tokenList = new ArrayList<Token>();

        if ((compositeFieldString == null) ||
            (compositeFieldString.length() == 0)) {
            throw new IllegalArgumentException(
                    INVALID_SYNTAX +
                    "Empty value configured.");
        }

        if (compositeFieldString.endsWith(TOKEN_ID)) {
            throw new IllegalArgumentException(
                    INVALID_SYNTAX +
                    "Token start '${' is missing closing '}' in '" +
                    compositeFieldString + "'.");            
        }

        int start = compositeFieldString.indexOf(TOKEN_ID);
        Scanner scanner = null;
        if (start == -1) {
            tokenList.add(new Token(true, compositeFieldString));
        } else if (start == 0) {
            scanner = new Scanner(compositeFieldString);
        } else {
            tokenList.add(new Token(true,
                                    compositeFieldString.substring(0, start)));
            scanner = new Scanner(compositeFieldString.substring(start));
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
                            compositeFieldString + "'.");
                } else if (stop == 0) {
                    throw new IllegalArgumentException(
                            INVALID_SYNTAX +
                            "Empty token '${}' specified in '" +
                            compositeFieldString + "'.");
                } else {
                    tokenList.add(new Token(false,
                                            current.substring(0, stop)));
                    start = stop + 1;
                    if (start < current.length()) {
                        tokenList.add(new Token(true,
                                                current.substring(start)));
                    }
                }
            }
        }

        return tokenList;
    }

    public class Token {
        private boolean isLiteral = false;
        private String value;

        private Token(boolean literal,
                      String value) {
            isLiteral = literal;
            this.value = value;
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
            }
            return derivedValue;
        }
    }

    private static final String INVALID_SYNTAX =
            "Invalid composite syntax.  ";
}