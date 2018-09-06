/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.filefilter;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This filter accepts files whose names match a specified pattern.
 *
 * @author Eric Trautman
 */
public class FileNamePatternFilter extends javax.swing.filechooser.FileFilter
        implements java.io.FileFilter {
    
    private String patternString;
    private Pattern pattern;
    private boolean includeDirectories;

    public FileNamePatternFilter(String patternString) {
        this.patternString = patternString;
        this.pattern = Pattern.compile(patternString);
        this.includeDirectories = false;
    }

    public String getDescription() {
        return patternString + " Files";
    }

    public void setIncludeDirectories(boolean includeDirectories) {
        this.includeDirectories = includeDirectories;
    }

    public boolean accept(File pathname) {
        boolean isAccepted = (includeDirectories && pathname.isDirectory());
        if ((! isAccepted) && (pathname.isFile())) {
            String fileName = pathname.getName();
            Matcher matcher = pattern.matcher(fileName);
            isAccepted = matcher.matches();
        }
        return isAccepted;
    }
}
