/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.target.Target;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class encapsulates a default field value that is based upon
 * a date encoded in the original name of source file being renamed.
 * It allows the encoded source file date to be reformatted for
 * use as a default field value.
 *
 * @author Eric Trautman
 */
public class SourceFileDateDefaultValue extends SourceFileDefaultValue {

    private SimpleDateFormat fromDateFormatter;
    private SimpleDateFormat toDateFormatter;

    public SourceFileDateDefaultValue() {
        super();
        init();
    }

    public SourceFileDateDefaultValue(String pattern,
                                      MatchType matchType) {
        super(pattern, matchType);
        init();
    }

    public String getFromDatePattern() {
        return fromDateFormatter.toPattern();
    }

    public void setFromDatePattern(String fromDatePattern) {
        this.fromDateFormatter = new SimpleDateFormat(fromDatePattern);
        this.fromDateFormatter.setLenient(false);
    }

    public String getToDatePattern() {
        return toDateFormatter.toPattern();
    }

    public void setToDatePattern(String toDatePattern) {
        this.toDateFormatter = new SimpleDateFormat(toDatePattern);
    }

    public String getValue(Target target) {
        String value = super.getValue(target);
        if (value != null) {
            try {
                Date date = fromDateFormatter.parse(value);
                value = toDateFormatter.format(date);
            } catch (ParseException e) {
                value = null;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("skipping reformat of default date, " +
                              "failed to parse '" + value +
                              "' with date format " + getFromDatePattern());
                }
            }
        }
        return value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SourceFileDateDefaultValue");
        sb.append("{pattern='").append(getPattern()).append('\'');
        sb.append(", matchType=").append(getMatchType());
        sb.append(", fromDatePattern=").append(fromDateFormatter.toPattern());
        sb.append(", toDatePattern=").append(toDateFormatter.toPattern());
        sb.append('}');
        return sb.toString();
    }

    private void init() {
        this.fromDateFormatter = new SimpleDateFormat();
        this.fromDateFormatter.setLenient(false);
        this.toDateFormatter = new SimpleDateFormat();
    }

    /** The logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(SourceFileDateDefaultValue.class);

}