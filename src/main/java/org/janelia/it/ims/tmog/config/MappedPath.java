/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config;

import org.janelia.it.utils.PathUtil;

/**
 * A mapping of a logical name to an operating system specific path.
 *
 * NOTE: This class is not currently used, but may be useful in the future.
 * 
 * @author Eric Trautman
 */
public class MappedPath {

    public enum OsType { MAC, UNIX, WINDOWS }

    private String logicalName;
    private OsType osType;
    private String path;

    public MappedPath() {
        this(null, OsType.WINDOWS, null);
    }

    public MappedPath(String logicalName,
                      OsType osType,
                      String path) {
        this.logicalName = logicalName;
        this.osType = osType;
        this.path = path;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public void setLogicalName(String logicalName) {
        this.logicalName = logicalName;
    }

    public String getOsType() {
        return String.valueOf(osType);
    }

    public boolean isRelevantForCurrentOs() {
        boolean isRelevant;
        if (PathUtil.ON_MAC) {
            isRelevant = (osType == OsType.MAC);
        } else if (PathUtil.ON_UNIX) {
            isRelevant = (osType == OsType.UNIX);
        } else {
            isRelevant = (osType == OsType.WINDOWS);
        }
        return isRelevant;
    }

    public void setOsType(String osType) throws IllegalArgumentException {
        this.osType = OsType.valueOf(osType);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "MappedPath{" +
               "logicalName='" + logicalName + '\'' +
               ", osType=" + osType +
               ", path='" + path + '\'' +
               '}';
    }
}