/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Configured logical to physical path name mappings for the current OS.
 *
 * NOTE: This class is not currently used, but may be useful in the future.
 *
 * @author Eric Trautman
 */
public class PathMapConfiguration {

    private Map<String, File> pathMap;

    public PathMapConfiguration() {
        pathMap = new HashMap<String, File>();
    }

    public File getPath(String logicalName) {
        return pathMap.get(logicalName);
    }

    public boolean addPath(MappedPath mappedPath) {

        boolean pathAdded = false;
        if (mappedPath.isRelevantForCurrentOs()) {
            File file = new File(mappedPath.getPath());
            boolean fileExists = false;
            try {
                fileExists = file.exists();
            } catch (Throwable t) {
                LOG.warn("ignoring existence check failure for " +
                         file.getAbsolutePath(), t);
            }
            if (fileExists) {
                pathMap.put(mappedPath.getLogicalName(), file);
                pathAdded = true;
            }
        }

        return pathAdded;
    }

    private static final Logger LOG =
            Logger.getLogger(PathMapConfiguration.class);
}