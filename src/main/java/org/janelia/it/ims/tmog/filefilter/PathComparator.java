/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.filefilter;

import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;
import java.util.Comparator;

/**
 * This comparator will sort file names based upon their full path.
 *
 * @author Eric Trautman
 */
public class PathComparator implements Comparator<FileTarget> {

    public int compare(FileTarget o1, FileTarget o2) {
        final File f1 = o1.getFile();
        final File f2 = o2.getFile();
        final String path1 = f1.getAbsolutePath();
        final String path2 = f2.getAbsolutePath();
        return path1.compareTo(path2);
    }
}