/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.filefilter;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * This filter accepts directories that are not part of its
 * excluded directory set.
 *
 * @author Peter Davies
 * @author Eric Trautman
 */
public class DirectoryOnlyFilter extends javax.swing.filechooser.FileFilter
        implements java.io.FileFilter {

    private Set<File> excludedDirectories;

    public DirectoryOnlyFilter() {
        this(new HashSet<File>());
    }

    public DirectoryOnlyFilter(String excludedDirectoryName) {
        this();
        File excludedDirectory = new File(excludedDirectoryName);
        if (excludedDirectory.exists()) { 
            excludedDirectories.add(excludedDirectory);
        }
    }

    public DirectoryOnlyFilter(HashSet<File> excludedDirectories) {
        this.excludedDirectories = excludedDirectories;
    }

    public Set<File> getExcludedDirectories() {
        return excludedDirectories;
    }

    public String getDescription() {
        return "Directories Only";
    }

    public boolean accept(File file) {
        return file.isDirectory() && (! excludedDirectories.contains(file));
    }
}
