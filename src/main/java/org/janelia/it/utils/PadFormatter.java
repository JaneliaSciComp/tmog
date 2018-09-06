/*
 * Copyright 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.utils;

/**
 * <p>
 * This class supports simple string padding, parsing its rules from
 * a pad format string.  The pad format string should have the following
 * format:
 * </p>
 *
 * <pre>
 *   [-]{minimum length}{pad character}
 * </pre>
 *
 * <p>
 * where:
 * </p>
 *
 * <table>
 *   <tr>
 *     <td>'-'</td>
 *     <td>indicates that values should be left justified
 *         (this is optional since by default values are right justified) </td>
 *   </tr>
 *   <tr>
 *     <td>{minimum length}</td>
 *     <td>identifies the minimum length for all padded values</td>
 *   </tr>
 *   <tr>
 *     <td>{pad character}</td>
 *     <td>identifies the character to use as a pad for values that have
 *         fewer characters than the minimum length</td>
 *   </tr>
 * </table>
 *
 * <p>
 * Here are some examples:
 * </p>
 *
 * <table>
 *   <tr>
 *     <th>Format</th>
 *     <th>Value</th>
 *     <th>Formatted Value</th>
 *   </tr>
 *   <tr>
 *     <td>'5q'</td>
 *     <td>'a'</td>
 *     <td>'qqqqa'</td>
 *   </tr>
 *   <tr>
 *     <td>'-5q'</td>
 *     <td>'bb'</td>
 *     <td>'bbqqq'</td>
 *   </tr>
 *   <tr>
 *     <td>'30'</td>
 *     <td>'1'</td>
 *     <td>'001'</td>
 *   </tr>
 * </table>
 *
 * @author Eric Trautman
 */
public class PadFormatter {

    private String format;
    private boolean isLeftJustified;
    private int minimumLength;
    private char padCharacter;

    public PadFormatter(String format) {
        this.format = format;

        if ((format == null) || (format.length() < 2)) {
            throw new IllegalArgumentException(
                    "invalid pad format '" + format + SYNTAX_FORM);
        }

        this.isLeftJustified = (format.charAt(0) == '-');

        int lenStartIndex = 0;
        if (this.isLeftJustified) {
            lenStartIndex = 1;
        }

        final int padCharacterIndex = format.length() - 1;
        this.padCharacter = format.charAt(padCharacterIndex);

        if (padCharacterIndex <= lenStartIndex) {
            throw new IllegalArgumentException(
                    "minimum length is missing from pad format '" + format +
                    SYNTAX_FORM);
        }

        String lenString = format.substring(lenStartIndex,
                                            padCharacterIndex);
        try {
            this.minimumLength = Integer.parseInt(lenString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "pad format '" + format + "' has invalid minimum length '" +
                    lenString + SYNTAX_FORM,
                    e);
        }
    }

    public String getFormat() {
        return format;
    }

    /**
     * @param  value  value to pad.
     *
     * @return padded form of the specified value.
     */
    public String formatValue(String value) {

        String formattedValue = value;

        if (value != null) {
            final int numPadCharacters = minimumLength - value.length();
            if (numPadCharacters > 0) {
                StringBuilder sb = new StringBuilder(minimumLength);
                if (isLeftJustified) {
                    sb.append(value);
                    for (int i = 0; i < numPadCharacters; i++) {
                        sb.append(padCharacter);
                    }
                } else {
                    for (int i = 0; i < numPadCharacters; i++) {
                        sb.append(padCharacter);
                    }
                    sb.append(value);
                }
                formattedValue = sb.toString();
            }
        }

        return formattedValue;
    }

    @Override
    public String toString() {
        return format;
    }

    private static final String SYNTAX_FORM =
            "', format should be specified as " +
            "'[-]<minimum length><pad character>'";
}