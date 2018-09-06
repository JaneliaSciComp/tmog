/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.target;

import java.io.File;
import java.util.Comparator;

/**
 * This class encapsulates data targets that are files.
 */
public class FileTarget implements Target {

    private File file;
    private File rootPath;
    private FileTargetNamer namer;
    private TargetProperties properties;

    public FileTarget(File file) {
        this(file, null, null);
    }

    public FileTarget(File file,
                      File rootPath) {
        this(file, rootPath, null);
    }

    public FileTarget(File file,
                      File rootPath,
                      FileTargetNamer namer) {
        this.file = file;
        this.rootPath = rootPath;
        this.namer = namer;
    }

    public FileTarget(File file,
                      TargetProperties properties) {
        this(file, null, null);
        this.properties = properties;
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
            if (namer == null) {
                name = file.getName();                
            } else {
                name = namer.getName(file);
            }
        }
        return name;
    }

    /**
     * @param  propertyName  name of desired property.
     *
     * @return the property value for this target
     *         or null if the property is not defined.
     */
    public String getProperty(String propertyName) {
        String value = null;
        if (properties != null) {
            value = properties.getValue(propertyName);
        }
        return value;
    }

    /**
     * Comparator for sorting file targets by file name.
     */
    public static final Comparator<FileTarget> ALPHABETIC_COMPARATOR =
            new Comparator<FileTarget>() {
                public int compare(FileTarget o1,
                                   FileTarget o2) {
                    return o1.getName().compareTo(o2.getName());
                }
    };

}
