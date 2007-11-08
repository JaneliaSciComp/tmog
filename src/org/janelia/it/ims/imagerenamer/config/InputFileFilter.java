package org.janelia.it.ims.imagerenamer.config;

import org.janelia.it.ims.imagerenamer.filefilter.FileNamePatternFilter;

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

    public FileFilter getFilter() {
        return filter;
    }
}


