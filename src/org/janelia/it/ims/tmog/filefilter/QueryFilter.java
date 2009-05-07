/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.filefilter;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.target.FileTargetNamer;

import java.io.BufferedReader;
import java.io.File;
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
                       FileTargetNamer targetNamer) {
        this.queryUrl = queryUrl;
        this.includeMatchedFiles = includeMatchedFiles;
        this.targetNamer = targetNamer;
        this.queryResults = new HashSet<String>(1024);
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
            method.releaseConnection();
        }

        LOG.info("retrieved " + queryResults.size() + " results");
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

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(QueryFilter.class);
}