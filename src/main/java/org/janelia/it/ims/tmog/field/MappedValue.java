/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

/**
 * This class encapsulates a mapped value.
 *
 * @author Eric Trautman
 */
public class MappedValue {

    private String from;
    private String to;

    @SuppressWarnings({"UnusedDeclaration"})
    public MappedValue() {
        this(null, null);
    }

    public MappedValue(String from,
                       String to) {
        this.from = from;
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public String toString() {
        return "MappedValue{" +
               "from='" + from + '\'' +
               ", to='" + to + '\'' +
               '}';
    }
}
