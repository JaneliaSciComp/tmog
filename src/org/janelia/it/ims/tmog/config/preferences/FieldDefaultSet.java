/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.config.preferences;

import org.janelia.it.utils.StringUtil;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A set of default field names and values.
 *
 * @author Eric Trautman
 */
public class FieldDefaultSet extends NamedObject {

    private Map<String, FieldDefault> nameToDefaultMap;

    public FieldDefaultSet() {
        this.nameToDefaultMap = new LinkedHashMap<String, FieldDefault>();
    }

    public Collection<FieldDefault> getFieldDefaults() {
        return nameToDefaultMap.values();
    }

    public FieldDefault getFieldDefault(String name) {
        return nameToDefaultMap.get(name);
    }

    public void addFieldDefault(FieldDefault fieldDefault) {
        final String name = fieldDefault.getName();
        if (StringUtil.isDefined(name) &&
                StringUtil.isDefined(fieldDefault.getValue())) {
            this.nameToDefaultMap.put(name, fieldDefault);
        }
    }

    public int size() {
        return nameToDefaultMap.size();
    }
    
    // TODO: replace this with jaxb annotations whenever we can drop jdk1.5
    public String toXml() {
        String xml;
        final String name = getName();
        if (StringUtil.isDefined(name) && (nameToDefaultMap.size() > 0)) {
            StringBuilder sb = new StringBuilder();
            sb.append("    <fieldDefaultSet name=\"");
            sb.append(StringUtil.getDefinedXmlValue(name));
            sb.append("\">\n");
            String valueXml;
            for (FieldDefault fieldDefault : nameToDefaultMap.values()) {
                valueXml = fieldDefault.toXml();
                if (valueXml.length() > 0) {
                    sb.append("      ");
                    sb.append(valueXml);
                }
            }
            sb.append("    </fieldDefaultSet>\n");
            xml = sb.toString();
        } else {
            xml = "";
        }
        return xml;
    }
}