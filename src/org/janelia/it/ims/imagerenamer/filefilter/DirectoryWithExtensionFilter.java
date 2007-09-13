/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.filefilter;

import java.io.File;

/**
 * This filter accepts directories that either:
 * <ul>
 *   <li> contain files with a specific extension, </li>
 *   <li> contain directories that contain files with a specific extension, or </li>
 *   <li> contain directories that contain other directories. </li>
 * </ul>
 *
 * @author Eric Trautman
 */
public class DirectoryWithExtensionFilter extends javax.swing.filechooser.FileFilter
        implements java.io.FileFilter {

    private FileNameExtensionFilter fileNameExtensionFilter;
    private DirectoryOnlyFilter directoryOnlyFilter;

    public DirectoryWithExtensionFilter(String fileNameExtension) {
        this.fileNameExtensionFilter =
                new FileNameExtensionFilter(fileNameExtension);
        this.directoryOnlyFilter = new DirectoryOnlyFilter();
    }

    public String getDescription() {
        return "Directories with " + fileNameExtensionFilter.getDescription();
    }

    public boolean accept(File pathname) {
        boolean isAcceptable = false;
        if (pathname.isDirectory()) {
            File[] subDirectoryFiles = pathname.listFiles(directoryOnlyFilter);

            for (File subDirectoryFile : subDirectoryFiles) {
                if (subDirectoryFile.isDirectory()) {
                    isAcceptable = true;
                    break;
                }
            }

            if (! isAcceptable) {
                File[] extenstionFiles =
                        pathname.listFiles(fileNameExtensionFilter);
                isAcceptable = (extenstionFiles.length > 0);
            }
        } else {
            isAcceptable = fileNameExtensionFilter.accept(pathname);
        }
        return isAcceptable;
    }
}
