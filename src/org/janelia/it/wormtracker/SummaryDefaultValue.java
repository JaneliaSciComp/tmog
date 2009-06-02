/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.wormtracker;

import org.janelia.it.ims.tmog.config.ConfigurationException;
import org.janelia.it.ims.tmog.field.PluginDefaultValue;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.Target;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class encapsulates a default field value that is based upon
 * an experiment summary file name.  The configured pattern
 * is applied to a summary file's name to derive the
 * default value.  Configured patterns are expected to contain one and
 * only one "capturing group" that identifies the path fragment to use
 * for the default value.  Java regular expression capturing groups are
 * represented by parentheses in the pattern.
 *
 * @author Eric Trautman
 */
public class SummaryDefaultValue implements PluginDefaultValue {

    private String pattern;
    private Pattern compiledPattern;

    public SummaryDefaultValue() {
    }

    public void init(Map<String, String> properties)
            throws ConfigurationException {
        String patternValue = properties.get("pattern");
        if ((patternValue != null) && (patternValue.length() > 0)) {
            setPattern(patternValue);
        } else {
            throw new ConfigurationException(
                    "A \"pattern\" property must be defined " +
                    "for the " + SummaryDefaultValue.class.getName() +
                    " plug-in.");
        }        
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
        if (pattern != null) {
            compiledPattern = Pattern.compile(pattern);
        }
    }

    public String getValue(Target target) {
        String value = null;
        String summaryFileName = null;
        if (target instanceof FileTarget) {
            FileTarget fileTarget = (FileTarget) target;
            File file = fileTarget.getFile();
            summaryFileName = SummaryPrefixPlugin.getSummaryFileName(file);
        }

        if (summaryFileName != null) {
            Matcher m = compiledPattern.matcher(summaryFileName);
            if (m.matches()) {
                if (m.groupCount() > 0) {
                    value = m.group(1);
                }
            }
        }

        return value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SummaryDefaultValue");
        sb.append("{pattern='").append(pattern).append('\'');
        sb.append('}');
        return sb.toString();
    }

}