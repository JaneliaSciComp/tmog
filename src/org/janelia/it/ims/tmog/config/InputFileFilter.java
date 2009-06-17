/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.config;

import org.janelia.it.ims.tmog.filefilter.FileNamePatternFilter;
import org.janelia.it.ims.tmog.filefilter.FileNamePatternWithQueryFilter;
import org.janelia.it.ims.tmog.target.FileTargetNamer;

import java.io.File;
import java.io.FileFilter;

/**
 * This class encapsulates configuration information about the
 * input directory file filter.
 *
 * TODO: revisit target namer configuration and creation
 *
 * @author Eric Trautman
 */
public class InputFileFilter {

    public static final String LSM_PATTERN_STRING = ".*\\.lsm";

    private String patternString;
    private Integer patternGroupNumber;
    private String includeQueryUrl;
    private String excludeQueryUrl;
    private FileFilter filter;
    private boolean recursiveSearch;

    public InputFileFilter() {
        this.setPatternString(LSM_PATTERN_STRING);
        this.recursiveSearch = false;
    }

    public String getPatternString() {
        return patternString;
    }

    public void setPatternString(String patternString) {
        this.patternString = patternString;
        this.filter = new FileNamePatternFilter(patternString);
    }

    public Integer getPatternGroupNumber() {
        return patternGroupNumber;
    }

    public void setPatternGroupNumber(Integer patternGroupNumber) {
        if (patternGroupNumber < 1) {
            throw new IllegalArgumentException(
                    "input file filter pattern group number " +
                    "must be greater than zero");
        }
        this.patternGroupNumber = patternGroupNumber;
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

    public boolean isRecursiveSearch() {
        return recursiveSearch;
    }

    public void setRecursiveSearch(boolean recursiveSearch) {
        this.recursiveSearch = recursiveSearch;
    }

    /**
     * @param  rootDirectory  root directory for all input files.
     *
     * @return a file filter based upon configured parameters.
     *
     * @throws IllegalArgumentException
     *   if a service at the configured query URL cannot be reached. 
     */
    public FileFilter getFilter(File rootDirectory)
            throws IllegalArgumentException {

        // rebuild query filters for each request
        if (excludeQueryUrl != null) {
            filter = new FileNamePatternWithQueryFilter(patternString,
                                                        excludeQueryUrl,
                                                        false,
                                                        getTargetNamer(rootDirectory));
        } else if (includeQueryUrl != null) {
            filter = new FileNamePatternWithQueryFilter(patternString,
                                                        includeQueryUrl,
                                                        true,
                                                        getTargetNamer(rootDirectory));
        }
        return filter;
    }

    /**
     * @param  rootDirectory  root directory for all input files.
     *
     * @return a target namer based upon configured parameters.
     */
    public FileTargetNamer getTargetNamer(File rootDirectory) {
        FileTargetNamer namer = null;
        if (patternGroupNumber != null) {
            namer = new FileTargetNamer(patternString,
                                        patternGroupNumber,
                                        rootDirectory.getAbsolutePath());
        }
        return namer;
    }
}


