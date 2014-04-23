/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.utils;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 *  This class provides utilities for working with files.
 *
 * @author Eric Trautman
 */
public class FileUtil {

    /**
     * This implementation works around limitations in the {@link java.io.File#canWrite()} method
     * to catch access issues with network mounted file systems.
     * It attempts to write an empty 'check file' to the specified directory and immediately removes
     * the file if it is written.
     *
     * @param  directory  directory to evaluate.
     *
     * @return true if the current user is allowed to write a file to the specified directory;
     *         otherwise false.
     */
    public static boolean canWriteToDirectory(File directory) {

        boolean canWrite = false;
        String checkFilePathString = null;
        File checkFile = null;

        if (directory.canWrite()) {

            FileOutputStream fos = null;
            try {
                // this is likely overkill, but decided to use a time stamp and random number
                // to decrease chance of failing due to concurrent checks with same name
                final int randomInt = RANDOM.nextInt(10000);
                final String pattern = "'check-write-'yyyyMMddHHmmssSSS'-" + randomInt + ".txt'";

                final SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                checkFile = new File(directory, sdf.format(new Date()));
                checkFilePathString = checkFile.getAbsolutePath();

                fos = new FileOutputStream(checkFile); // this will fail without network file system access
                canWrite = true;
            } catch (Exception e) {
                LOG.error("failed to create " + checkFilePathString, e);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        LOG.error("failed to close " + checkFilePathString + ", ignoring error", e);
                    }
                }
            }

        }

        if ((checkFile != null) && (checkFile.exists())) {
            try {
                if (! checkFile.delete()) {
                    LOG.error("failed to delete " + checkFilePathString + ", ignoring error");
                }
            } catch (Exception e) {
                LOG.error("failed to delete " + checkFilePathString + ", ignoring error", e);
            }
        }

        return canWrite;
    }

    private static final Logger LOG = Logger.getLogger(FileUtil.class);
    private static final Random RANDOM = new Random();
}
