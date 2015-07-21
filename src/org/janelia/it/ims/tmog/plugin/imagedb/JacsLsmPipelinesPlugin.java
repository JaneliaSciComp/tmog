/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.StaticDataModel;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PropertyTokenList;
import org.janelia.it.ims.tmog.plugin.RelativePathUtil;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowListener;
import org.janelia.it.ims.tmog.plugin.SessionListener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This plug-in invokes the JACS lsm pipelines API.
 *
 * @author Eric Trautman
 */
public class JacsLsmPipelinesPlugin
        implements RowListener, SessionListener {

    public static final String DATA_SET_COLUMN_PROPERTY = "dataSetColumnName";
    public static final String RELATIVE_PATH_DEPTH_PROPERTY = "relativePathDepth";
    public static final String SERVICE_URL_PROPERTY = "serviceUrl";
    public static final String TEST_URL_PROPERTY = "testUrl";

    public static final String DEFAULT_DATA_SET_COLUMN_NAME = "Data Set";


    /** The number of parent directories to include in item value. */
    private int relativePathDepth = 1;

    private String dataSetColumnName;

    /** HTTP client for sageLoader requests. */
    private HttpClient httpClient;

    /** Parsed configuration tokens for deriving a row specific URL. */
    private PropertyTokenList urlTokens;

    /**
     * Plug-in instances are shared across all session threads,
     * so we need to track relative paths for successfully processed
     * LSM files for each thread.
     */
    private Map<Thread, Map<String, Set<String>>> threadToDataSetPathMap;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public JacsLsmPipelinesPlugin() {
        this.httpClient = new HttpClient();
        this.threadToDataSetPathMap = new ConcurrentHashMap<>();
    }

    /**
     * Verifies that the plugin is ready for use by checking external
     * dependencies.
     *
     * @param  config  the plugin configuration.
     *
     * @throws org.janelia.it.ims.tmog.plugin.ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config)
            throws ExternalSystemException {

        this.dataSetColumnName = DEFAULT_DATA_SET_COLUMN_NAME;

        String serviceUrl = null;
        String testUrl = null;

        final Map<String, String> props = config.getProperties();
        String value;
        for (String key : props.keySet()) {
            value = props.get(key);
            switch (key) {
                case SERVICE_URL_PROPERTY:
                    serviceUrl = value;
                    break;
                case TEST_URL_PROPERTY:
                    testUrl = value;
                    break;
                case DATA_SET_COLUMN_PROPERTY:
                    this.dataSetColumnName = value;
                    break;
                case RELATIVE_PATH_DEPTH_PROPERTY:
                    this.relativePathDepth = getPathDepth(value);
                    break;
            }
        }

        // validate configuration
        checkRequiredProperty(SERVICE_URL_PROPERTY, serviceUrl);
        checkRequiredProperty(TEST_URL_PROPERTY, testUrl);

        LOG.info("init: service URL with parameters is " + serviceUrl);

        try {
            this.urlTokens = new PropertyTokenList(serviceUrl, props);

            if (! isResourceFound(testUrl)) {
                throw new IllegalArgumentException(
                        "The " + TEST_URL_PROPERTY + " property '" + testUrl +
                        "' identifies a non-existent resource.");
            }

        } catch (Exception e) {
            LOG.error(e);
            throw new ExternalSystemException(INIT_FAILURE_MSG + e.getMessage(),
                                              e);
        }

    }

    @Override
    public List<DataRow> startSession(List<DataRow> modelRows)
            throws ExternalDataException, ExternalSystemException {
        return null; // nothing to do here
    }

    /**
     * For successful events, saves the path of the LSM file.
     *
     * @param  eventType  type of event.
     * @param  row        details about the event.
     *
     * @return the specified row unchanged.
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow processEvent(RowListener.EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        if (RowListener.EventType.END_ROW_SUCCESS.equals(eventType)) {
            if (row instanceof RenamePluginDataRow) {

                final Thread currentThread = Thread.currentThread();
                Map<String, Set<String>> dataSetPathMap = threadToDataSetPathMap.get(currentThread);
                if (dataSetPathMap == null) {
                    dataSetPathMap = new HashMap<>();
                    threadToDataSetPathMap.put(currentThread, dataSetPathMap);
                }

                final String dataSet = row.getCoreValue(dataSetColumnName);

                Set<String> lsmPathSet = dataSetPathMap.get(dataSet);
                if (lsmPathSet == null) {
                    lsmPathSet = new HashSet<>();
                    dataSetPathMap.put(dataSet, lsmPathSet);
                }

                final String relativePath = RelativePathUtil.getRelativePath(row.getTargetFile(), relativePathDepth);
                lsmPathSet.add(relativePath);
            }
        }

        return row;
    }

    @Override
    public void endSession(String message)
            throws ExternalDataException, ExternalSystemException {
        submitLaunchRequestForCurrentThread();
    }

    private void checkRequiredProperty(String propertyName,
                                       String value)
            throws ExternalSystemException {
        if ((value == null) || (value.length() == 0)) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "The '" +
                    propertyName + "' property must be defined.");
        }
    }

    private int getPathDepth(String value) throws ExternalSystemException {
        int depth = relativePathDepth;
        boolean invalidValue;
        try {
            depth = Integer.parseInt(value);
            invalidValue = (depth < 1);
        } catch (NumberFormatException e) {
            invalidValue = true;
        }
        if (invalidValue) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "The '" + RELATIVE_PATH_DEPTH_PROPERTY + "' property '" +
                    value + "' must be a positive integer value.");
        }
        return depth;
    }

    private boolean isResourceFound(String url)
            throws ExternalSystemException {

        boolean isFound = false;

        int responseCode;
        HeadMethod method = null;
        try {
            method = new HeadMethod(url);
            responseCode = httpClient.executeMethod(method);
            LOG.info("isResourceFound: " + responseCode +
                     " returned for " + url);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                isFound = true;
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                isFound = false;
            } else {
                throw new ExternalSystemException(
                        "Unexpected response code (" + responseCode +
                        ") returned for " + url + ".");
            }
        } catch (IOException e) {
            throw new ExternalSystemException(
                    "Failed to confirm that resource at " + url + " exists.",
                    e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }

        return isFound;
    }

    private void submitLaunchRequestForCurrentThread() {

        final Thread currentThread = Thread.currentThread();
        final Map<String, Set<String>> dataSetPathMap = threadToDataSetPathMap.get(currentThread);

        try {
            for (final String dataSet : dataSetPathMap.keySet()) {
                submitLaunchRequestForDataSet(dataSet, dataSetPathMap.get(dataSet));
            }
        } finally {
            threadToDataSetPathMap.remove(currentThread);
        }

    }

    private void submitLaunchRequestForDataSet(final String dataSet,
                                               final Set<String> lsmPathSet) {

        int responseCode;
        String url = null;
        PostMethod method = null;
        try {
            // construct URL
            final Map<String, DataField> fieldMap = new HashMap<>();
            fieldMap.put(dataSetColumnName, new StaticDataModel(dataSetColumnName, dataSet));
            final List<String> urlList = urlTokens.deriveValues(fieldMap, true);
            url = urlList.get(0);

            method = new PostMethod(url);

            final String json = convertPathsToJson(lsmPathSet);
            method.setRequestEntity(new StringRequestEntity(json, "application/json", StandardCharsets.UTF_8.name()));

            responseCode = httpClient.executeMethod(method);

            LOG.info("submitLaunchRequest: " + responseCode + " returned for " + url);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                LOG.error("submitLaunchRequest: request failed for " + url);
            }

        } catch (Exception e) {
            LOG.error("failed to submit launch request for " + url, e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }

    }

    private String convertPathsToJson(final Set<String> lsmPathSet) {
        final StringBuilder sb = new StringBuilder(4096);
        sb.append('[');
        int count = 0;
        for (final String path : lsmPathSet) {
            if (count > 0) {
                sb.append(',');
            }
            sb.append('"');
            sb.append(path);
            sb.append('"');
            count++;
        }
        sb.append(']');
        return sb.toString();
    }

    private static final Log LOG = LogFactory.getLog(JacsLsmPipelinesPlugin.class);

    private static final String INIT_FAILURE_MSG = "Failed to initialize JACS LSM Pipelines plug-in.  ";

}