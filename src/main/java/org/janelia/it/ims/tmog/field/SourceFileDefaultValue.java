/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.Target;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
    private String patternGroupSpec;
    private List<Integer> patternGroupNumberList;
    private int maxGroupNumber;
    private Pattern compiledPattern;
    private MatchType matchType;

    public SourceFileDefaultValue() {
        this(null, MatchType.name);
    }

    public SourceFileDefaultValue(String pattern,
                                  MatchType matchType) {
        setPattern(pattern);
        this.patternGroupNumberList = null;
        this.matchType = matchType;
        this.maxGroupNumber = 1;
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

    public String getPatternGroupSpec() {
        return patternGroupSpec;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setPatternGroupSpec(String patternGroupSpec) {

        this.patternGroupSpec = patternGroupSpec;

        try {
            final String[] numbers = patternGroupSpec.split(",");
            this.patternGroupNumberList =
                    new ArrayList<Integer>(numbers.length);
            for (String number : numbers) {
                this.patternGroupNumberList.add(Integer.parseInt(number));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "invalid pattern group spec '" + patternGroupSpec +
                    "'", e);
        }

        if (this.patternGroupNumberList.size() == 0) {
            throw new IllegalArgumentException(
                    "invalid pattern group spec '" + patternGroupSpec +
                    "', at least one number must be specified");
        }

        for (Integer number : this.patternGroupNumberList) {
            if (number < 0) {
                throw new IllegalArgumentException(
                        "invalid pattern group spec '" + patternGroupSpec +
                        "', all numbers must be greater than zero");
            } else if (number > this.maxGroupNumber) {
                this.maxGroupNumber = number;
            }
        }
    }

    public String getMatchType() {
        return matchType.name();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setMatchType(String matchTypeName) {
        try {
            this.matchType = MatchType.valueOf(matchTypeName);
        } catch (IllegalArgumentException e) {
            LOG.warn("ignoring invalid match type name " + matchTypeName);
        }
    }

    public String getValue(Target target) {
        String value = null;
        File sourceFile = null;
        if (target instanceof FileTarget) {
            sourceFile = ((FileTarget) target).getFile();
        }
        if (sourceFile != null) {
            String textToMatch;
            if (MatchType.path.equals(matchType)) {
                textToMatch = sourceFile.getAbsolutePath();
            } else {
                textToMatch = sourceFile.getName();
            }
            Matcher m = compiledPattern.matcher(textToMatch);
            if (m.matches()) {
                if (m.groupCount() >= maxGroupNumber) {
                    if (patternGroupNumberList == null) {
                        value = m.group(1);
                    } else {
                        StringBuilder sb = new StringBuilder(128);
                        for (Integer number : patternGroupNumberList) {
                            sb.append(m.group(number));
                        }
                        value = sb.toString();
                    }
                }
            }
        }
        return value;
    }

    @Override
    public String toString() {
        return "SourceFileDefaultValue{" +
               "matchType=" + matchType +
               ", pattern='" + pattern + '\'' +
               ", patternGroupSpec=" + patternGroupSpec +
               '}';
    }

    /** The logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(SourceFileDefaultValue.class);

}