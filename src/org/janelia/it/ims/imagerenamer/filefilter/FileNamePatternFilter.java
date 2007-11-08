/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.filefilter;

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

    public FileNamePatternFilter(String patternString) {
        this.patternString = patternString;
        this.pattern = Pattern.compile(patternString);
    }

    public String getPatternString() {
        return patternString;
    }

    public String getDescription() {
        return patternString + " Files";
    }

    public boolean accept(File pathname) {
        String fileName = pathname.getName();
        Matcher matcher = pattern.matcher(fileName);
        return matcher.matches();
    }
}
