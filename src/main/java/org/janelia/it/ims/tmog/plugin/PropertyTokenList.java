/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.janelia.it.ims.tmog.field.DataField;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * This class encapsulates a list of tokens parsed from
 * a plug-in configuration property.
 *
 * @author Eric Trautman
 */
public class PropertyTokenList {

    public static final String TOKEN_ID = "${";

    private List<PropertyToken> list;
    private GroupPropertyToken groupPropertyToken;

    /**
     * Constructs a list for the specified token string.
     *
     * @param  tokenString  token string to parse.
     * @param  properties   configuration properties (for group tokens).
     *
     * @throws IllegalArgumentException
     *   if the token string cannot be properly parsed.
     */
    public PropertyTokenList(String tokenString,
                             Map<String, String> properties)
            throws IllegalArgumentException {
        parse(tokenString, properties);
    }

    /**
     * @param  nameToFieldMap  map of field names to instances for
     *                         value derivation.
     *
     * @param  encodeValues    true if values should be URL encoded;
     *                         otherwise false.
     *
     * @return the list of derived values for this token list given the
     *         specified fields.  The list will only contain multiple elements
     *         if this token list contains a group field that is not
     *         concatenated.
     */
    public List<String> deriveValues(Map<String, DataField> nameToFieldMap,
                                     boolean encodeValues) {

        List<String> values = new ArrayList<String>();

        try {

            int numberOfValues = 1;
            if (groupPropertyToken != null) {
                numberOfValues =
                        groupPropertyToken.getNumberOfValues(nameToFieldMap);
            }

            StringBuilder sb;
            for (int i = 0; i < numberOfValues; i++) {
                sb = new StringBuilder();
                String tokenValue;
                for (PropertyToken token : list) {
                    tokenValue = token.getValue(nameToFieldMap, i);
                    if (tokenValue != null) {
                        if (encodeValues && (! token.isLiteral())) {
                            tokenValue = URLEncoder.encode(tokenValue, "UTF-8");
                        }
                        sb.append(tokenValue);
                    }
                }
                values.add(sb.toString());
            }

        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("failed to encode value", e);
        }

        return values;
    }

    private void parse(String tokenString,
                       Map<String, String> properties)
        throws IllegalArgumentException {

        list = new ArrayList<PropertyToken>();
        groupPropertyToken = null;

        if ((tokenString == null) || (tokenString.length() == 0)) {
            throw new IllegalArgumentException(
                    "Empty value configured.");
        }

        if (tokenString.endsWith(TOKEN_ID)) {
            throw new IllegalArgumentException(
                    "Token start '${' is missing closing '}' in '" +
                    tokenString + "'.");
        }

        int start = tokenString.indexOf(TOKEN_ID);
        Scanner scanner = null;
        if (start == -1) {
            list.add(new PropertyToken(true, tokenString));
        } else if (start == 0) {
            scanner = new Scanner(tokenString);
        } else {
            list.add(new PropertyToken(true,
                                       tokenString.substring(0, start)));
            scanner = new Scanner(tokenString.substring(start));
        }

        if (scanner != null) {
            scanner.useDelimiter("\\$\\{");

            int stop;
            String current;
            String tokenValue;
            while (scanner.hasNext()) {
                current = scanner.next();
                stop = current.indexOf('}');
                if (stop == -1) {
                    throw new IllegalArgumentException(
                            "Token start '${' is missing closing '}' in '" +
                            tokenString + "'.");
                } else if (stop == 0) {
                    throw new IllegalArgumentException(
                            "Empty token '${}' specified in '" +
                            tokenString + "'.");
                } else {

                    tokenValue = current.substring(0, stop);
                    if (GroupPropertyToken.isGroupPropertyToken(tokenValue)) {
                        if (groupPropertyToken != null) {
                            throw new IllegalArgumentException(
                                    "Multiple group tokens specified: '" +
                                    groupPropertyToken.getValue() +
                                    "' and '" + tokenValue +
                                    "' (only one is allowed).");
                        }
                        groupPropertyToken =
                                new GroupPropertyToken(tokenValue,
                                                       properties);
                        list.add(groupPropertyToken);
                    } else {
                        list.add(new PropertyToken(false, tokenValue));
                    }
                    start = stop + 1;
                    if (start < current.length()) {
                        this.list.add(
                                new PropertyToken(true,
                                                  current.substring(start)));
                    }
                }
            }
        }

    }

}