/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.config;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class encapsulates configuration information about the
 * application output directory.
 *
 * @author Eric Trautman
 */
public class OutputDirectory {

    /** Formatter for deriving date based output directories. */
    private static final SimpleDateFormat FORMAT =
            new SimpleDateFormat("yyyyMMdd");

    private String basePath;
    private boolean isManuallyChosen;

    public OutputDirectory() {
    }

    public String getBasePath() {
        return basePath;
    }

    public boolean isManuallyChosen() {
        return isManuallyChosen;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public void setManuallyChosen(boolean manuallyChosen) {
        isManuallyChosen = manuallyChosen;
    }

    public void verify() throws ConfigurationException {
        if (! isManuallyChosen) {
            File baseDirectory = new File(basePath);
            if (! baseDirectory.exists()) {
                throw new ConfigurationException(
                        "The output directory base path (" +
                        baseDirectory.getAbsolutePath() +
                        ") does not exist.");
            }
            if (! baseDirectory.isDirectory()) {
                throw new ConfigurationException(
                        "The output directory base path (" +
                        baseDirectory.getAbsolutePath() +
                        ") is not a directory.");
            }
            if (! baseDirectory.canWrite()) {
                throw new ConfigurationException(
                        "The output directory base path (" +
                        baseDirectory.getAbsolutePath() +
                        ") is not writable.");
            }
        }
    }

    public String getDerivedPath(File sourceDirectory,
                                 File[] sourceFiles) {
        File derivedPath;
        if (! isManuallyChosen) {
            long earliestMod = sourceDirectory.lastModified();
            for (File sourceFile : sourceFiles) {
                long fileMod = sourceFile.lastModified();
                if (fileMod < earliestMod) {
                    earliestMod = fileMod;
                }
            }
            String name = FORMAT.format(new Date(earliestMod));
            derivedPath = new File(basePath, name);
        } else {
            derivedPath = new File(basePath);
        }

        return derivedPath.getAbsolutePath();
    }
}
