/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.config.preferences;

import org.janelia.it.utils.StringUtil;

/**
 * A default field name and value.
 *
 * @author Eric Trautman
 */
public class FieldDefault extends NamedObject {

    private String value;

    public FieldDefault() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    // TODO: replace this with jaxb annotations whenever we can drop jdk1.5
    public String toXml() {
        String xml;
        final String name = getName();
        if (StringUtil.isDefined(name) && StringUtil.isDefined(value)) {
            StringBuilder sb = new StringBuilder();
            sb.append("<fieldDefault name=\"");
            sb.append(StringUtil.getDefinedXmlValue(name));
            sb.append("\">");
            sb.append(StringUtil.getDefinedXmlValue(value));
            sb.append("</fieldDefault>\n");
            xml = sb.toString();
        } else {
            xml = "";
        }
        return xml;
    }
}