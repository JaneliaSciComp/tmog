/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config.output;

import org.janelia.it.ims.tmog.field.DataField;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class encapsulates an output directory path fragment that is
 * derived from user specified data in a rename field.
 *
 * @author Eric Trautman
 */
public class RenameFieldValue implements OutputDirectoryComponent {

    private String fieldDisplayName;
    private String prefix;
    private String suffix;
    private String pattern;
    private Integer patternGroupNumber;
    private Pattern compiledPattern;

    /**
     * Empty constructor.
     */
    public RenameFieldValue() {
        this.patternGroupNumber = 1;
    }

    /**
     * @return the associated field display name.
     */
    public String getFieldDisplayName() {
        return fieldDisplayName;
    }

    /**
     * Sets the associated rename field display name for this fragment.
     *
     * @param  fieldDisplayName  name of the field whose value should be
     *                           used for this fragment.
     */
    public void setFieldDisplayName(String fieldDisplayName) {
        this.fieldDisplayName = fieldDisplayName;
    }

    /**
     * @return the prefix to prepend to this path fragment if its
     *         corresponding rename field is defined.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the prefix to prepend to this path fragment if its
     * corresponding rename field is defined.
     *
     * @param  prefix  prefix to prepend.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * @return the suffix to append to this path fragment if its
     *         corresponding rename field is defined.
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * Sets the suffix to append to this path fragment if its
     * corresponding rename field is defined.
     *
     * @param  suffix  suffix to append.
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
        if (pattern != null) {
            this.compiledPattern = Pattern.compile(pattern);
        }
    }

    public Integer getPatternGroupNumber() {
        return patternGroupNumber;
    }

    public void setPatternGroupNumber(Integer patternGroupNumber) {
        this.patternGroupNumber = patternGroupNumber;
    }

    /**
     * Uses the specified source data to derive an output directory
     * path fragment.
     *
     * @param  sourceFile    the source file being renamed.
     * @param  dataFields  the validated rename fields supplied by the user.
     *
     * @return the path fragment derived from the specified source data.
     */
    public String getValue(File sourceFile,
                           List<DataField> dataFields) {
        String value = null;
        if (fieldDisplayName != null) {
            for (DataField field : dataFields) {
                if (fieldDisplayName.equals(field.getDisplayName())) {
                    value = field.getCoreValue();
                    if (pattern != null) {
                        Matcher m = compiledPattern.matcher(value);
                        if (m.matches()) {
                            if (m.groupCount() >= patternGroupNumber) {
                                value = m.group(patternGroupNumber);
                            }
                        }
                    }
                    break;
                }
            }
        }

        if ((value == null) || (value.length() == 0)) {
            value = "";
        } else if ((prefix != null) || (suffix != null)) {
            StringBuilder sb = new StringBuilder(64);
            if (prefix != null) {
                sb.append(prefix);
            }
            sb.append(value);
            if (suffix != null) {
                sb.append(suffix);
            }
            value = sb.toString();
        }

        return value;
    }

    /**
     * @return a description of this output directory path fragment for display.
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder(64);
        if (fieldDisplayName != null) {
            sb.append("[");
            if (prefix != null) {
                sb.append(prefix);
            }
            sb.append("<");
            if (pattern != null) {
                sb.append("part of ");
            }
            sb.append(fieldDisplayName);
            sb.append(">");
            if (suffix != null) {
                sb.append(suffix);
            }
            sb.append("]");
        }
        return sb.toString();
    }

    /**
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return getDescription();
    }
}