/*
 * Copyright © 2008 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class encapsulates a default field value that is based upon the
 * original name of source file being renamed.  The configured pattern
 * is applied to a source file's name (or path) to derive the
 * default value.  Configured patterns are expected to contain one and
 * only one "capturing group" that identifies the path fragment to use
 * for the default value.  Java regular expression capturing groups are
 * represented by parentheses in the pattern.
 *
 * @author Eric Trautman
 */
public class SourceFileDefaultValue implements DefaultValue {

    public enum MatchType { name, path }
    
    private String pattern;
    private Pattern compiledPattern;
    private MatchType matchType;

    public SourceFileDefaultValue() {
        this(null, MatchType.name);
    }

    public SourceFileDefaultValue(String pattern,
                                  MatchType matchType) {
        setPattern(pattern);
        this.matchType = matchType;
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

    public String getMatchType() {
        return matchType.name();
    }

    public void setMatchType(String matchTypeName) {
        try {
            this.matchType = MatchType.valueOf(matchTypeName);
        } catch (IllegalArgumentException e) {
            LOG.warn("ignoring invalid match type name " + matchTypeName);
        }
    }

    public String getValue(File sourceFile) {
        String value = null;
        if (sourceFile != null) {
            String textToMatch;
            if (MatchType.path.equals(matchType)) {
                textToMatch = sourceFile.getAbsolutePath();
            } else {
                textToMatch = sourceFile.getName();
            }
            Matcher m = compiledPattern.matcher(textToMatch);
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
        sb.append("SourceFileDefaultValue");
        sb.append("{pattern='").append(pattern).append('\'');
        sb.append(", matchType=").append(matchType);
        sb.append('}');
        return sb.toString();
    }

    /** The logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(SourceFileDefaultValue.class);

}