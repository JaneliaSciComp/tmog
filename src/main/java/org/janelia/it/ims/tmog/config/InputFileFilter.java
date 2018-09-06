/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config;

import org.janelia.it.ims.tmog.filefilter.FileListFilter;
import org.janelia.it.ims.tmog.filefilter.FileNamePatternFilter;
import org.janelia.it.ims.tmog.filefilter.FileNamePatternWithQueryFilter;
import org.janelia.it.ims.tmog.target.FileTargetNamer;
import org.janelia.it.ims.tmog.target.TargetDataFile;

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
    private String excludeQueryUrl;
    private String includeQueryUrl;
    private String excludeList;
    private String includeList;
    private FileFilter filter;
    private boolean recursiveSearch;
    private boolean filterDuplicates;
    private TargetDataFile targetDataFile;

    public InputFileFilter() {
        this.setPatternString(LSM_PATTERN_STRING);
        this.recursiveSearch = false;
        this.filterDuplicates = false;
    }

    public void setPatternString(String patternString) {
        this.patternString = patternString;
        this.filter = new FileNamePatternFilter(patternString);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setPatternGroupNumber(Integer patternGroupNumber) {
        if (patternGroupNumber < 1) {
            throw new IllegalArgumentException(
                    "input file filter pattern group number " +
                    "must be greater than zero");
        }
        this.patternGroupNumber = patternGroupNumber;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setExcludeQueryUrl(String excludeQueryUrl) {
        this.excludeQueryUrl = excludeQueryUrl;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setIncludeQueryUrl(String includeQueryUrl) {
        this.includeQueryUrl = includeQueryUrl;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setExcludeList(String excludeList) {
        this.excludeList = excludeList;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setIncludeList(String includeList) {
        this.includeList = includeList;
    }

    public boolean isRecursiveSearch() {
        return recursiveSearch;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setRecursiveSearch(boolean recursiveSearch) {
        this.recursiveSearch = recursiveSearch;
    }

    public boolean isFilterDuplicates() {
        return filterDuplicates;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setFilterDuplicates(boolean filterDuplicates) {
        this.filterDuplicates = filterDuplicates;
    }

    public TargetDataFile getTargetDataFile() {
        return targetDataFile;
    }

    public boolean hasTargetDataFile() {
        return (targetDataFile != null);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setTargetDataFile(TargetDataFile targetDataFile) {
        this.targetDataFile = targetDataFile;
    }

    /**
     * Validates the configured settings.
     *
     * @throws ConfigurationException
     *   if any of the settings are invalid.
     */
    public void verify() throws ConfigurationException {
        if (hasTargetDataFile()) {
            targetDataFile.verify();
        }
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
        } else if (excludeList != null) {
            filter = new FileListFilter(excludeList,
                                        false,
                                        patternString,
                                        patternGroupNumber);
        } else if (includeList != null) {
            filter = new FileListFilter(includeList,
                                        true,
                                        patternString,
                                        patternGroupNumber);
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


