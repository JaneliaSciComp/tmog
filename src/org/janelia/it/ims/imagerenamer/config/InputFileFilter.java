/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.config;

import org.janelia.it.ims.imagerenamer.filefilter.FileNamePatternFilter;
import org.janelia.it.ims.imagerenamer.filefilter.QueryFilter;

import java.io.FileFilter;

/**
 * This class encapsulates configuration information about the
 * input directory file filter.
 *
 * @author Eric Trautman
 */
public class InputFileFilter {

    public static final String LSM_PATTERN_STRING = ".*\\.lsm";

    private String patternString;
    private String includeQueryUrl;
    private String excludeQueryUrl;
    private FileFilter filter;

    public InputFileFilter() {
        this.setPatternString(LSM_PATTERN_STRING);
    }

    public String getPatternString() {
        return patternString;
    }

    public void setPatternString(String patternString) {
        this.patternString = patternString;
        this.filter = new FileNamePatternFilter(patternString);
    }

    public String getExcludeQueryUrl() {
        return excludeQueryUrl;
    }

    public void setExcludeQueryUrl(String excludeQueryUrl) {
        this.excludeQueryUrl = excludeQueryUrl;
    }

    public String getIncludeQueryUrl() {
        return includeQueryUrl;
    }

    public void setIncludeQueryUrl(String includeQueryUrl) {
        this.includeQueryUrl = includeQueryUrl;
    }

    public FileFilter getFilter() {
        // rebuild query filters for each request
        if (excludeQueryUrl != null) {
            filter = new QueryFilter(excludeQueryUrl, false);
        } else if (includeQueryUrl != null) {
            filter = new QueryFilter(includeQueryUrl, true);
        }
        return filter;
    }
}


