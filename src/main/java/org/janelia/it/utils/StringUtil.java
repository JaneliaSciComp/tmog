/*
* Copyright 2008 Howard Hughes Medical Institute.
* All rights reserved.
* Use is subject to Janelia Farm Research Center Software Copyright 1.0
* license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
*/

package org.janelia.it.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides common string utility methods and constants.
 *
 * @author Eric Trautman
 */
public class StringUtil {

    /** An empty string. */
    public static final String EMPTY_STRING = "";

    /** The file separator for the current operating system. */
    public static final String FILE_SEPARATOR =
            System.getProperty("file.separator");

    /** The line separator for the current operating system. */
    public static final String LINE_SEPARATOR =
            System.getProperty("line.separator");

    /**
     * Makes sure null values are represented as empty strings and
     * escapes any characters in the specified string so that it can be
     * used as an XML attribute value or element value.  The escaping logic
     * for this method was copied from the private method:
     * {@link java.beans.XMLEncoder#quoteCharacters(String)}.
     *
     * @param  value  value to define and escape.
     *
     * @return a defined and escaped version of the specified value.
     */
    public static String getDefinedXmlValue(String value) {
        String definedValue;
        if (value == null) {
            definedValue = EMPTY_STRING;
        } else {
            StringBuffer result = null;
            char c;
            String replacement;
            for (int i = 0, max = value.length(), delta = 0; i < max; i++) {
                c = value.charAt(i);
                replacement = null;

                if (c == '&') {
                    replacement = XML_AMP;
                } else if (c == '<') {
                    replacement = XML_LT;
                } else if (c == '\r') {
                    replacement = XML_CR;
                } else if (c == '>') {
                    replacement = XML_GT;
                } else if (c == '"') {
                    replacement = XML_QUOT;
                } else if (c == '\'') {
                    replacement = XML_APOS;
                }

                if (replacement != null) {
                    if (result == null) {
                        result = new StringBuffer(value);
                    }
                    result.replace(i + delta, i + delta + 1, replacement);
                    delta += (replacement.length() - 1);
                }
            }
            if (result == null) {
                definedValue = value;
            } else {
                definedValue = result.toString();
            }
        }
        return definedValue;
    }

    /**
     * Simplistically transforms the specified name into a valid XML element
     * name by replacing all non-alphanumeric characters with an underscore,
     * prepending an underscore if the the name starts with a number, and
     * converting all characters to lower case.
     *
     * See <a href="http://www.w3.org/TR/xml/#NT-Name">
     * http://www.w3.org/TR/xml/#NT-Name</a> for XML element naming rules.
     *
     * @param  name  name to transform.
     *
     * @return a version of the specified name that is valid for use as an XML
     *         element name.
     */
    public static String getXmlElementName(String name) {
        String elementName;
        if ((name == null) || (name.length() == 0)) {
            elementName = "element";
        } else {
            Matcher m = NON_ALPHA_NUMERIC_PATTERN.matcher(name);
            elementName = m.replaceAll("_");
            if (Character.isDigit(elementName.charAt(0))) {
                elementName = "_" + elementName;
            }
            elementName = elementName.toLowerCase();
        }
        return elementName;
    }

    /**
     * @param  str  string to check.
     *
     * @return true if the specified string has 1 or more non-blank characters;
     *         otherwise false;
     */
    public static boolean isDefined(String str) {
        boolean isDefined = false;
        if (str != null) {
            String trimmedStr = str.trim();
            isDefined = (trimmedStr.length() > 0);
        }
        return isDefined;
    }

    // XML escape strings
    private static final String XML_AMP = "&amp;";
    private static final String XML_LT = "&lt;";
    private static final String XML_CR = "&#13;";
    private static final String XML_GT = "&gt;";
    private static final String XML_QUOT = "&quot;";
    private static final String XML_APOS = "&apos;";

    private static final Pattern NON_ALPHA_NUMERIC_PATTERN =
            Pattern.compile("[^\\p{Alnum}]");
}
