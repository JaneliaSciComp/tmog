/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config.preferences;

import org.janelia.it.utils.StringUtil;

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
    private Map<String, ViewDefault> nameToViewMap;

    /**
     * Constructs an empty set of project preferences.
     */
    public ProjectPreferences() {
        this.nameToDefaultSetMap = new LinkedHashMap<String, FieldDefaultSet>();
        this.nameToViewMap = new LinkedHashMap<String, ViewDefault>();
    }

    /**
     * @return the names of all field default sets in this project's
     *         preferences.
     */
    public Set<String> getFieldDefaultSetNames() {
        return nameToDefaultSetMap.keySet();
    }

    /**
     * @param  name  name of field default set to retrieve.
     *
     * @return field default set with the specified name or null if none exists.
     */
    public FieldDefaultSet getFieldDefaultSet(String name) {
        return nameToDefaultSetMap.get(name);
    }

    /**
     * Adds the specified field default set to this project's preferences.
     *
     * @param  fieldDefaultSet  field default set to add.
     */
    public void addFieldDefaultSet(FieldDefaultSet fieldDefaultSet) {
        this.nameToDefaultSetMap.put(fieldDefaultSet.getName(),
                                     fieldDefaultSet);
    }

    /**
     * Removes the specified field default set from this project's preferences.
     *
     * @param  name  name of field default set to remove.
     *
     * @return the removed set or null if none was found.
     */
    public FieldDefaultSet removeFieldDefaultSet(String name) {
        return nameToDefaultSetMap.remove(name);
    }

    /**
     * @param  name  name of field default set to check.
     *
     * @return true if a field default set with the specified name exists
     *         in this project's preferences; otherwise false.
     */
    public boolean containsDefaultSet(String name) {
        return nameToDefaultSetMap.containsKey(name);
    }

    /**
     * @param  name  name of the view default to retrieve.
     *
     * @return view default with the specified name or null if none exists.
     */
    public ViewDefault getViewDefault(String name) {
        return getViewDefault(name, false);
    }

    /**
     * @param  name              name of the view default to retrieve.
     *
     * @param  createWhenAbsent  if true and a view default with the specified
     *                           name does not exist, creates the view default.
     *
     * @return view default with the specified name.
     */
    public ViewDefault getViewDefault(String name,
                                      boolean createWhenAbsent) {
        ViewDefault viewDefault = nameToViewMap.get(name);
        if ((viewDefault == null) && createWhenAbsent) {
            viewDefault = new ViewDefault();
            viewDefault.setName(name);
            addViewDefault(viewDefault);
        }
        return viewDefault;
    }

    /**
     * Adds the specified view default to this project's preferences.
     *
     * @param  viewDefault  view default to add.
     */
    public void addViewDefault(ViewDefault viewDefault) {
        this.nameToViewMap.put(viewDefault.getName(),
                               viewDefault);
    }

    /**
     * @return an xml string representation of this object.
     */
    // TODO: replace this with jaxb annotations whenever we can drop jdk1.5
    public String toXml() {
        String xml;
        final String name = getName();
        if (StringUtil.isDefined(name)) {
            StringBuilder sb = new StringBuilder(512);
            sb.append("  <projectPreferences name=\"");
            sb.append(StringUtil.getDefinedXmlValue(name));
            sb.append("\">\n");
            final String nestedIndent = "    ";
            for (FieldDefaultSet defaultSet : nameToDefaultSetMap.values()) {
                sb.append(defaultSet.toXml(nestedIndent));
            }
            for (ViewDefault viewDefault : nameToViewMap.values()) {
                viewDefault.appendXml(nestedIndent, sb);
            }
            sb.append("  </projectPreferences>\n");
            xml = sb.toString();
        } else {
            xml = "";
        }
        return xml;
    }
}