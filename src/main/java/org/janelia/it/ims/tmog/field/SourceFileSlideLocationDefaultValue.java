/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.target.Target;

/**
 * This class encapsulates a default field value that is based upon the
 * original name of source file being renamed.
 * The configured pattern is applied to a source file's name (or path)
 * to derive a slide sequence number.  The slide sequence number is then
 * converted to a row/column code (e.g. 'C5') based upon the configured 
 * number of columns for each slide.
 *
 * Configured patterns are expected to contain one and
 * only one "capturing group" that identifies the path fragment to use
 * for the default value.  Java regular expression capturing groups are
 * represented by parentheses in the pattern.
 *
 * @author Eric Trautman
 */
public class SourceFileSlideLocationDefaultValue
        extends SourceFileDefaultValue {

    /** Number of columns in each slide. */
    private int numberOfSlideColumns;

    @SuppressWarnings({"UnusedDeclaration"})
    public SourceFileSlideLocationDefaultValue() {
        this(null, MatchType.name);
    }

    public SourceFileSlideLocationDefaultValue(String pattern,
                                               MatchType matchType) {
        super(pattern, matchType);
        this.numberOfSlideColumns = 6;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setNumberOfSlideColumns(int numberOfSlideColumns) {
        this.numberOfSlideColumns = numberOfSlideColumns;
    }

    @Override
    public String getValue(Target target) {
        String value = null;
        final String slideSequenceNumberString = super.getValue(target);
        if (slideSequenceNumberString != null) {
            try {
                final int sequenceNumber =
                        Integer.parseInt(slideSequenceNumberString) - 1;
                final int row = sequenceNumber / numberOfSlideColumns;
                if (row < 26) {
                    final char rowChar = (char) ((int)'A' + row);
                    final int column =
                            (sequenceNumber % numberOfSlideColumns) + 1;
                    value = rowChar + String.valueOf(column);
                }
            } catch (NumberFormatException e) {
                // ignore errors
            }
        }
        return value;
    }

    @Override
    public String toString() {
        return "SourceFileSlideLocationDefaultValue{" +
               "matchType=" + getMatchType() +
               ", pattern='" + getPattern() + '\'' +
               ", patternGroupSpec=" + getPatternGroupSpec() +
               ", numberOfSlideColumns=" + numberOfSlideColumns +
               '}';
    }
}