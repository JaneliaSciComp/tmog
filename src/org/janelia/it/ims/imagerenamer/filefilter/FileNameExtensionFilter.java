/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.filefilter;

import java.io.File;

/**
 * This filter accepts files whose names end with a specific extension.
 *
 * @author Peter Davies
 */
public class FileNameExtensionFilter extends javax.swing.filechooser.FileFilter
        implements java.io.FileFilter {
    private String fileNameExtension;

    public FileNameExtensionFilter(String fileNameExtension) {
        this.fileNameExtension = fileNameExtension;
    }

    public String getFileNameExtension() {
        return fileNameExtension;
    }

    public String getDescription() {
        return fileNameExtension + " Files";
    }

    public boolean accept(File pathname) {
        String fileName = pathname.getName();
        int fileNameLen = fileName.length();
        int extLen = fileNameExtension.length();

        return fileName.regionMatches(true, // ignoreCase
                                      (fileNameLen - extLen),
                                      fileNameExtension,
                                      0,
                                      extLen);
    }
}
