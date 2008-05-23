/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.filefilter;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
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

    public QueryFilter(String queryUrl,
                       boolean includeMatchedFiles) {
        this.queryUrl = queryUrl;
        this.includeMatchedFiles = includeMatchedFiles;
        this.queryResults = new HashSet<String>(1024);
        BufferedReader in = null;
        try {
            URL url = new URL(queryUrl);
            URLConnection connection = url.openConnection();
            in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                this.queryResults.add(inputLine);
            }
        } catch (IOException e) {
            LOG.warn("Failed to execute file query '" + queryUrl + "'", e);
            // TODO: should filter errors propogate to user?
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.error("failed to close query input stream, " +
                              "ignoring error", e);
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("queryResults=" + queryResults);
        }
    }

    public String getQueryUrl() {
        return queryUrl;
    }

    public boolean isIncludeMatchedFiles() {
        return includeMatchedFiles;
    }

    public Set<String> getQueryResults() {
        return queryResults;
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
        String fileName = pathname.getName();
        boolean isAccepted;
        if (includeMatchedFiles) {
            isAccepted = queryResults.contains(fileName);
        } else {
            isAccepted = ! queryResults.contains(fileName);
        }
        return isAccepted;
    }

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(QueryFilter.class);
}