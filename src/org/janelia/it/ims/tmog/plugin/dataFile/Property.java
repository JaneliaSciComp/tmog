/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.dataFile;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

/**
 * Simple item property.
 *
 * @author Eric Trautman
 */
public class Property {

    @XmlAttribute
    private String name;

    @XmlValue
    private String value;

    @SuppressWarnings({"UnusedDeclaration"})
    public Property() {
        this(null, null);
    }

    public Property(String name,
                    String value) {
        this.name = name;
        this.value = value;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setNameAndValue(String name,
                                String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
