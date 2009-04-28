/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.target;

import org.janelia.it.ims.tmog.filefilter.AlphabeticComparator;

import java.io.File;
import java.util.Comparator;

/**
 * This class encapsulates data targets that are files.
 */
public class FileTarget implements Target {

    private File file;
    private File rootPath;

    public FileTarget(File file) {
        this(file, null);
    }

    public FileTarget(File file,
                      File rootPath) {
        this.file = file;
        this.rootPath = rootPath;
    }

    public File getFile() {
        return file;
    }

    /**
     * @return the root path selected when this target was located. 
     */
    public File getRootPath() {
        return rootPath;
    }

    /**
     * @return the target instance.
     */
    public Object getInstance() {
        return file;
    }

    /**
     * @return the target name.
     */
    public String getName() {
        String name = null;
        if (file != null) {
            name = file.getName();
        }
        return name;
    }

    private static final AlphabeticComparator FILE_NAME_COMPARATOR =
            new AlphabeticComparator();

    /**
     * Comparator for sorting file targets by file name.
     */
    public static final Comparator<FileTarget> ALPHABETIC_COMPARATOR =
            new Comparator<FileTarget>() {
                public int compare(FileTarget o1,
                                   FileTarget o2) {
                    return FILE_NAME_COMPARATOR.compare(o1.getFile(),
                                                        o2.getFile());
                }
    };

}
