/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
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
    private Map<String, FieldDefaultSet> nameToDefaultSetMap;

    /**
     * Constructs an empty set.
     */
    public FieldDefaultSet() {
        this.nameToDefaultMap = new LinkedHashMap<String, FieldDefault>();
        this.nameToDefaultSetMap = new LinkedHashMap<String, FieldDefaultSet>();
    }

    /**
     * @return the collection of sets nested within this set.
     */
    public Collection<FieldDefaultSet> getFieldDefaultSets() {
        return nameToDefaultSetMap.values();
    }

    /**
     * @param  name  name of the default to retrieve.
     *
     * @return the default associated with the specified name
     *         (or null if none exists).
     */
    public FieldDefault getFieldDefault(String name) {
        return nameToDefaultMap.get(name);
    }

    /**
     * @param  name  name of the default set to retrieve.
     *
     * @return the nested default set associated with the specified name
     *         (or null if none exists).
     */
    public FieldDefaultSet getFieldDefaultSet(String name) {
        return nameToDefaultSetMap.get(name);
    }

    /**
     * Adds the specified default to this set.
     *
     * @param  fieldDefault  default to add.
     */
    public void addFieldDefault(FieldDefault fieldDefault) {
        final String name = fieldDefault.getName();
        if (StringUtil.isDefined(name) &&
                StringUtil.isDefined(fieldDefault.getValue())) {
            this.nameToDefaultMap.put(name, fieldDefault);
        }
    }

    /**
     * Adds the specified default set to this set.
     *
     * @param  fieldDefaultSet  default set to add.
     */
    public void addFieldDefaultSet(FieldDefaultSet fieldDefaultSet) {
        final String name = fieldDefaultSet.getName();
        if (StringUtil.isDefined(name) && fieldDefaultSet.size() > 0) {
            this.nameToDefaultSetMap.put(name, fieldDefaultSet);
        }
    }

    /**
     * @return the total number of defaults and nested default sets in this set.
     */
    public int size() {
        return nameToDefaultMap.size() + nameToDefaultSetMap.size();
    }

    /**
     * @param  indent  indentation string to prepend to all elements.
     *
     * @return an xml representation of this set
     *         formatted with the specified indentation.
     */
    // TODO: replace this with jaxb annotations whenever we can drop jdk1.5
    protected String toXml(String indent) {
        String xml;
        if (size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(indent);
            final String name = getName();
            if (StringUtil.isDefined(name)) {
                sb.append("<fieldDefaultSet name=\"");
                sb.append(StringUtil.getDefinedXmlValue(name));
                sb.append("\">\n");
            } else {
                sb.append("<fieldDefaultSet>\n");
            }

            String valueXml;
            for (FieldDefault fieldDefault : nameToDefaultMap.values()) {
                valueXml = fieldDefault.toXml();
                if (valueXml.length() > 0) {
                    sb.append(indent);
                    sb.append("  ");
                    sb.append(valueXml);
                }
            }
            
            for (FieldDefaultSet groupSet : nameToDefaultSetMap.values()) {
                if (groupSet.size() > 0) {
                    sb.append(groupSet.toXml(indent + "  "));
                }
            }

            sb.append(indent);
            sb.append("</fieldDefaultSet>\n");
            xml = sb.toString();
        } else {
            xml = "";
        }
        return xml;
    }
}