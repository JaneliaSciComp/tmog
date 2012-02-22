/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.filefilter;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.target.FileTargetNamer;
import org.janelia.it.utils.PathUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.Set;

/**
 * This filter accepts files whose names either match or do not match
 * a set of names returned from a REST query.
 *
 * @author Eric Trautman
 */
public class QueryFilter extends javax.swing.filechooser.FileFilter
        implements java.io.FileFilter {

    private String queryUrl;
    private boolean includeMatchedFiles;
    private Set<String> queryResults;
    private FileTargetNamer targetNamer;

    public QueryFilter(String queryUrl,
                       boolean includeMatchedFiles,
                       FileTargetNamer targetNamer)
            throws IllegalArgumentException {
        
        this.queryUrl = queryUrl;
        this.includeMatchedFiles = includeMatchedFiles;
        this.targetNamer = targetNamer;
        this.queryResults = new HashSet<String>(1024);

        if (queryUrl.startsWith("file://")) {
            loadNamesFromFileOrDirectory(queryUrl);
        } else {
            executeHttpQuery(queryUrl);
        }

        LOG.info("retrieved " + queryResults.size() +
                 " results from " + queryUrl);
    }

    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Files ");
        if (! includeMatchedFiles) {
            sb.append("not ");
        }
        sb.append("in ");
        sb.append(queryUrl);
        return sb.toString();
    }

    public boolean accept(File pathname) {
        String targetName;
        if (targetNamer == null) {
            targetName = pathname.getName();
        } else {
            targetName = targetNamer.getName(pathname);
        }
        
        boolean isAccepted;
        if (includeMatchedFiles) {
            isAccepted = queryResults.contains(targetName);
        } else {
            isAccepted = ! queryResults.contains(targetName);
        }
        return isAccepted;
    }

    private void loadNamesFromFileOrDirectory(String queryUrl) {

        // remove leading 'file://'
        final String fileName =queryUrl.substring(7);
        final String convertedFileName = PathUtil.convertPath(fileName);
        final File file = new File(convertedFileName);

        String error = null;
        if (! file.exists()) {
            error = "does not exist";
        } else if (! file.canRead()) {
            error = "can not be read";
        }
        if (error != null) {
            throw new IllegalArgumentException(
                    "Query filter file '" + file.getAbsolutePath() + "' " +
                    error +
                    ".  Please verify the configured url is accurate.");
        }

        LOG.info("looking for filter files in " + file.getAbsolutePath());

        if (file.isDirectory()) {
            int count = 0;
            for (File fileInDirectory : file.listFiles()) {
                if (fileInDirectory.isFile() && fileInDirectory.canRead()) {
                    count++;
                    loadNamesFromFile(fileInDirectory);
                }
            }
            LOG.info("loaded " + count + " filter files in directory " + 
                     file.getAbsolutePath());
        } else {
            loadNamesFromFile(file);
        }

        if (queryResults.size() == 0) {
            LOG.warn("no file names found in " + file.getAbsolutePath());
        }
    }

    private void loadNamesFromFile(File file) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                this.queryResults.add(inputLine);
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

    private void executeHttpQuery(String queryUrl) {
        BufferedReader in = null;
        int responseCode;
        URI requestUri = null;
        GetMethod method = new GetMethod(queryUrl);
        try {
            method.setRequestHeader("Accept", "text/plain");
            HttpClient httpClient = new HttpClient();
            LOG.info("sending GET " + queryUrl);
            responseCode = httpClient.executeMethod(method);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException(
                        "The request for '" + requestUri +
                        "' failed with response code " + responseCode + ".");
            }

            in = new BufferedReader(
                    new InputStreamReader(method.getResponseBodyAsStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                this.queryResults.add(inputLine);
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to submit filter request to '" + queryUrl +
                    "'.  Please verify the configured url is accurate and " +
                    "that the corresponding service is available.", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.error("failed to close query input stream, " +
                              "ignoring error", e);
                }
            }
            method.releaseConnection();
        }
    }

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(QueryFilter.class);
}