/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.target.Target;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This class encapsulates a default field value that is based upon
 * a target property.  If desired, specific target property values
 * can also be mapped to actual values.
 *
 * @author Eric Trautman
 */
public class TargetPropertyDefaultValue
        implements DefaultValue {

    private String propertyName;
    private Map<String, String> map;
    private SimpleDateFormat fromDateFormatter;
    private SimpleDateFormat toDateFormatter;


    @SuppressWarnings({"UnusedDeclaration"})
    public TargetPropertyDefaultValue() {
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void addMappedValue(MappedValue mappedValue) {
        if (map == null) {
            this.map = new HashMap<String, String>();            
        }
        map.put(mappedValue.getFrom(), mappedValue.getTo());
    }

    public String getFromDatePattern() {
        return fromDateFormatter.toPattern();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setFromDatePattern(String fromDatePattern) {
        this.fromDateFormatter = new SimpleDateFormat(fromDatePattern);
        this.fromDateFormatter.setLenient(false);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public String getToDatePattern() {
        return toDateFormatter.toPattern();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setToDatePattern(String toDatePattern) {
        this.toDateFormatter = new SimpleDateFormat(toDatePattern);
    }

    public String getValue(Target target) {
        String value = target.getProperty(propertyName);

        if (map != null) {

            String mappedValue = map.get(value);
            if (mappedValue != null) {
                value = mappedValue;
            }

        } else if ((fromDateFormatter != null) &&
                   (toDateFormatter != null) &&
                   (value != null)) {

            try {
                Date date = fromDateFormatter.parse(value);
                value = toDateFormatter.format(date);
            } catch (ParseException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("skipping reformat of default date, " +
                              "failed to parse '" + value +
                              "' with date format " + getFromDatePattern());
                }
            }
        }

        return value;
    }

    /** The logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(TargetPropertyDefaultValue.class);

}