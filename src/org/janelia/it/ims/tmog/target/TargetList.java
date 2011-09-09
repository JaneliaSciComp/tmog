/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.target;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of targets coupled with a message that summarizes
 * the processing performed to produce the list.
 *
 * @author Eric Trautman
 */
public class TargetList {

    private List<FileTarget> list;
    private StringBuilder summary;

    public TargetList() {
        this.list = new ArrayList<FileTarget>();
        this.summary = new StringBuilder();
    }

    public List<FileTarget> getList() {
        return list;
    }

    public void addTarget(FileTarget target) {
        list.add(target);
    }

    public String getSummary() {
        return summary.toString();
    }

    public void appendToSummary(String message) {
        summary.append(message);
    }
}
