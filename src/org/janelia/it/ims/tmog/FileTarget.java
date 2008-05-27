/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog;

import java.io.File;

/**
 * This class encapsulates data targets that are files.
 */
public class FileTarget implements Target {
    File file;

    public FileTarget(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
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
}
