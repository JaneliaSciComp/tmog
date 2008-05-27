/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */
package org.janelia.it.utils;

import java.util.Locale;

/**
 * This class provides utilities for managing file path names.
 *
 * @author Eric Trautman
 */
public class PathUtil {

    /**
     * Converts the specified path as needed for the current operating system.
     * Primarily, conversion involves setting the appropriate file separator.
     * It also involves converting between smb://server/dir on Mac and
     * \\server\dir on Windows for paths that refernce remote file servers.
     *
     * @param  srcPath  the path to convert.
     *
     * @return the converted path.
     */
    public static String convertPath(String srcPath) {
        String targetPath = srcPath;
        if (ON_MAC) {
            targetPath = convertPathToMac(srcPath);
        } else if (ON_WINDOWS) {
            targetPath = convertPathToWindows(srcPath);
        } else if (ON_UNIX) {
            targetPath = convertPathToUnix(srcPath);
        }
        return targetPath;
    }

    /**
     * Converts the specified path for the Mac operating system.
     *
     * @param  srcPath  the path to convert.
     *
     * @return the converted path.
     */
    public static String convertPathToMac(String srcPath) {
        String targetPath = srcPath;
        if (srcPath != null) {
            targetPath = srcPath.replace('\\', '/');
            if (targetPath.startsWith("//")) {
                if (targetPath.length() > 2) {
                    int start = targetPath.indexOf('/', 2);
                    if (start != -1) {
                        targetPath = MAC_VOLUMES_ROOT +
                                     targetPath.substring(start);
                    } else {
                        targetPath = MAC_VOLUMES_ROOT;
                    }
                } else {
                    targetPath = MAC_VOLUMES_ROOT;
                }
            } else {
                targetPath = removeColonPrefix(targetPath);
            }
        }
        return targetPath;
    }

    /**
     * Converts the specified path for the Windows operating system.
     *
     * @param  srcPath  the path to convert.
     *
     * @return the converted path.
     */
    public static String convertPathToWindows(String srcPath) {
        String targetPath = srcPath;
        if (srcPath != null) {
            targetPath = srcPath.replace('/', '\\');
            if (srcPath.startsWith(MAC_SAMBA_PROTOCOL)) {
                targetPath = removeColonPrefix(targetPath);
            }
        }
        return targetPath;
    }

    /**
     * Converts the specified path for the Unix operating system.
     *
     * @param  srcPath  the path to convert.
     *
     * @return the converted path.
     */
    public static String convertPathToUnix(String srcPath) {
        String targetPath = srcPath;
        if (srcPath != null) {
            targetPath = srcPath.replace('\\', '/');
            targetPath = removeColonPrefix(targetPath);
        }
        return targetPath;
    }

    /**
     * Removes the first colon and any prior text from the specified path.
     *
     * @param  srcPath  path to parse.
     *
     * @return the converted path.
     */
    public static String removeColonPrefix(String srcPath) {
        String targetPath = srcPath;
        if (srcPath != null) {
            int firstColon = srcPath.indexOf(":");
            if (firstColon != -1) {
                int start = firstColon + 1;
                if (srcPath.length() > start) {
                    targetPath = srcPath.substring(start);
                } else {
                    targetPath = "";
                }
            }
        }
        return targetPath;
    }

    // Logic to detect runtime operating system taken from:
    //   org/apache/tools/ant/taskdefs/condition/Os.java

    private static final String OS_NAME =
        System.getProperty("os.name").toLowerCase(Locale.US);

    private static final String PATH_SEP =
        System.getProperty("path.separator");

    private static final boolean ON_WINDOWS =
            PATH_SEP.equals(";") && (OS_NAME.indexOf("netware") == -1);

    private static final boolean ON_MAC = OS_NAME.indexOf("mac") > -1;

    private static final boolean ON_UNIX =
            PATH_SEP.equals(":") &&
            (OS_NAME.indexOf("openvms") == -1) &&
            ((! ON_MAC) || OS_NAME.endsWith("x"));

    private static final String MAC_SAMBA_PROTOCOL =
            "smb://";
    private static final String MAC_VOLUMES_ROOT =
            "/Volumes";
}