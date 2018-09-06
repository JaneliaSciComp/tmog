/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.target;

import java.io.File;

/**
 * This class derives desired target names from files.
 *
 * @author Eric Trautman
 */
public class FileTargetNamer extends TargetNamer {

    private String rootPath;

    /**
     * Contructs a namer.
     *
     * @param  pattern             pattern for derivation.
     *
     * @param  patternGroupNumber  identifies desired pattern group
     *                             to be used as the target name
     *                             (must be greater than zero).
     *
     * @param  rootPath            root path to remove from target
     *                             name (or null if not relevant).
     *
     * @throws IllegalArgumentException
     *   if the group number is not greater than zero.
     */
    public FileTargetNamer(String pattern,
                           Integer patternGroupNumber,
                           String rootPath)
            throws IllegalArgumentException {
        super(pattern,  patternGroupNumber);
        this.rootPath = rootPath;
    }

    /**
     * @param  file  the target file.
     *
     * @return a name derived from the specified full name.
     */
    public String getName(File file) {
        String name = super.getName(file.getName());
        if (rootPath != null) {
            // TODO: revisit target namer configuration and creation

// The following block was commented out after wormtracker no longer needed
// to add normalized relative path information to the target name.
// We still need to come up with a cleaner solution for deriving target names.

//            String absName = file.getAbsolutePath();
//            if (absName.startsWith(rootPath)) {
//                int start = rootPath.length() + 1;
//                int stop = absName.lastIndexOf(File.separatorChar);
//                if ((stop != -1) && (start < stop)) {
//                    stop = stop + 1;
//                    name = absName.substring(start, stop) + name;
//                    name = name.replace('\\', '/');
//                }
//            }
        }
        return name;
    }

}