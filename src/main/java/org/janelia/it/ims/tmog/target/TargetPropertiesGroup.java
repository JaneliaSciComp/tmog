/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The shared and specific properties for a group of targets.
 * This container simplifies parsing of the XML configuration file
 * (see {@link XmlTargetDataFile}) and is only used for that purpose.
 * The shared properties "cached" here are ultimately referenced
 * within each TargetProperties instance.
 *
 * @author Eric Trautman
 */
public class TargetPropertiesGroup {

    private Map<String, String> sharedProperties;
    private List<TargetProperties> list;

    public TargetPropertiesGroup() {
        this.sharedProperties = new HashMap<String, String>();
        this.list = new ArrayList<TargetProperties>();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void addSharedProperty(String name,
                                  String value) {
        sharedProperties.put(name, value);
    }

    public List<TargetProperties> getList() {
        return list;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void addTargetProperties(TargetProperties targetProperties) {
        targetProperties.setSharedProperties(sharedProperties);
        list.add(targetProperties);
    }

}
