/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.filefilter;

import org.apache.log4j.Logger;
import org.janelia.it.utils.PathUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This filter accepts files whose names match a specified pattern
 * and are in (or not in) a specified list.
 *
 * @author Eric Trautman
 */
public class FileListFilter
        extends javax.swing.filechooser.FileFilter
        implements java.io.FileFilter {

    private File fileList;
    private boolean includeMatchedFiles;
    private Pattern pattern;
    private int patternGroupNumber;

    private Set<String> fileNamesFromList;

    public FileListFilter(String fileListName,
                          boolean includeMatchedFiles,
                          String patternString,
                          Integer patternGroupNumber) {

        final String convertedFileListName = PathUtil.convertPath(fileListName);

        this.fileList = new File(convertedFileListName);
        this.includeMatchedFiles = includeMatchedFiles;

        if (patternString == null) {
            throw new IllegalArgumentException(
                    "patternString must be specified for file list filter");
        }

        if (patternGroupNumber == null) {
            throw new IllegalArgumentException(
                    "patternGroupNumber must be specified for file list filter");
        }

        try {
            this.pattern = Pattern.compile(patternString);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "failed to parse file list filter patternString '" +
                    patternString + "'", e);
        }

        this.patternGroupNumber = patternGroupNumber;
        
        loadNamesFromFileOrDirectory();
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Files ");
        if (! includeMatchedFiles) {
            sb.append("not ");
        }
        sb.append("listed in ");
        sb.append(fileList.getAbsolutePath());
        return sb.toString();
    }

    @Override
    public boolean accept(File f) {

        boolean isAccepted = false;

        String nameToFilter = PathUtil.convertPathToUnix(f.getAbsolutePath());

        Matcher matcher = pattern.matcher(nameToFilter);

        if (matcher.matches()) {

            if (matcher.groupCount() >= patternGroupNumber) {
                nameToFilter = matcher.group(patternGroupNumber);
            }

            if (fileNamesFromList.contains(nameToFilter)) {
                isAccepted = includeMatchedFiles;
            } else {
                isAccepted = (! includeMatchedFiles);
            }
        }

        return isAccepted;
    }

    private void loadNamesFromFileOrDirectory()
            throws IllegalArgumentException {

        this.fileNamesFromList = new HashSet<String>(1024);

        String error = null;
        if (! fileList.exists()) {
            error = "does not exist";
        } else if (! fileList.canRead()) {
            error = "can not be read";
        }
        if (error != null) {
            throw new IllegalArgumentException(
                    "Filter file list '" + fileList.getAbsolutePath() + "' " +
                    error +
                    ".  Please verify the configured url is accurate.");
        }

        LOG.info("looking for filter files in " + fileList.getAbsolutePath());

        if (fileList.isDirectory()) {
            int count = 0;
            for (File fileInDirectory : fileList.listFiles()) {
                if (fileInDirectory.isFile() && fileInDirectory.canRead()) {
                    count++;
                    loadNamesFromFile(fileInDirectory);
                }
            }
            LOG.info("loaded " + count + " filter files in directory " +
                     fileList.getAbsolutePath());
        } else {
            loadNamesFromFile(fileList);
        }

        if (fileNamesFromList.size() == 0) {
            LOG.warn("no file names found in " + fileList.getAbsolutePath());
        } else {
            LOG.info("added " + fileNamesFromList.size() +
                     " file names from " + fileList.getAbsolutePath() +
                     " to filter");
        }
    }

    private void loadNamesFromFile(File file) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                this.fileNamesFromList.add(inputLine);
            }

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to load input filter file names from " +
                    file.getAbsolutePath() +
                    "'.  Please verify the configured url is accurate.", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.error("failed to close file input stream, " +
                              "ignoring error", e);
                }
            }
        }
    }

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(FileListFilter.class);
}
