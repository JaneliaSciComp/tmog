/*
 * Copyright 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config.preferences;

import org.janelia.it.utils.StringUtil;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * All preference settings for a specific project.
 *
 * @author Eric Trautman
 */
public class ProjectPreferences extends NamedObject {

    private Map<String, FieldDefaultSet> nameToDefaultSetMap;

    public ProjectPreferences() {
        this.nameToDefaultSetMap = new LinkedHashMap<String, FieldDefaultSet>();
    }

    public Collection<FieldDefaultSet> getFieldDefaultSets() {
        return nameToDefaultSetMap.values();
    }

    public Set<String> getFieldDefaultSetNames() {
        return nameToDefaultSetMap.keySet();
    }

    public FieldDefaultSet getFieldDefaultSet(String name) {
        return nameToDefaultSetMap.get(name);
    }

    public void addFieldDefaultSet(FieldDefaultSet fieldDefaultSet) {
        this.nameToDefaultSetMap.put(fieldDefaultSet.getName(),
                                     fieldDefaultSet);
    }

    public FieldDefaultSet removeFieldDefaultSet(String name) {
        return nameToDefaultSetMap.remove(name);
    }

    public int getNumberOfFieldDefaultSets() {
        return nameToDefaultSetMap.size();
    }

    public boolean containsDefaultSet(String name) {
        return nameToDefaultSetMap.containsKey(name);
    }
    
    // TODO: replace this with jaxb annotations whenever we can drop jdk1.5
    public String toXml() {
        String xml;
        final String name = getName();
        if (StringUtil.isDefined(name) && (nameToDefaultSetMap.size() > 0)) {
            StringBuilder sb = new StringBuilder();
            sb.append("  <projectPreferences name=\"");
            sb.append(StringUtil.getDefinedXmlValue(name));
            sb.append("\">\n");
            for (FieldDefaultSet defaultSet : nameToDefaultSetMap.values()) {
                if (defaultSet.size() > 0) {
                    sb.append(defaultSet.toXml());
                }
            }
            sb.append("  </projectPreferences>\n");
            xml = sb.toString();
        } else {
            xml = "";
        }
        return xml;
    }
}