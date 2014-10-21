/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PropertyTokenList;
import org.janelia.it.ims.tmog.plugin.RowListener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This plug-in invokes the JACS sample removal API.
 *
 * @author Eric Trautman
 */
public class JacsSampleRemovalPlugin
        implements RowListener {

    public static final String SERVICE_URL_NAME = "serviceUrl";
    public static final String TEST_URL_NAME = "testUrl";

    /** HTTP client for sageLoader requests. */
    private HttpClient httpClient;

    /** Parsed configuration tokens for deriving a row specific URL. */
    private PropertyTokenList urlTokens;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public JacsSampleRemovalPlugin() {
        this.httpClient = new HttpClient();
    }

    /**
     * Verifies that the plugin is ready for use by checking external
     * dependencies.
     *
     * @param  config  the plugin configuration.
     *
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config)
            throws ExternalSystemException {

        String serviceUrl = null;
        String testUrl = null;

        final Map<String, String> props = config.getProperties();
        String value;
        for (String key : props.keySet()) {
            value = props.get(key);
            if (SERVICE_URL_NAME.equals(key)) {
                serviceUrl = value;
            } else if (TEST_URL_NAME.equals(key)) {
                testUrl = value;
            }
        }

        // validate configuration
        checkRequiredProperty(SERVICE_URL_NAME, serviceUrl);
        checkRequiredProperty(TEST_URL_NAME, testUrl);

        LOG.info("init: service URL with parameters is " + serviceUrl);

        try {
            this.urlTokens = new PropertyTokenList(serviceUrl, props);

            if (! isResourceFound(testUrl)) {
                throw new IllegalArgumentException(
                        "The " + TEST_URL_NAME + " property '" + testUrl +
                        "' identifies a non-existent resource.");
            }

        } catch (Exception e) {
            LOG.error(e);
            throw new ExternalSystemException(INIT_FAILURE_MSG + e.getMessage(),
                                              e);
        }

    }

    /**
     * Processes the specified event.
     *
     * @param  eventType  type of event.
     * @param  row        details about the event.
     *
     * @return the field row for processing (with any
     *         updates from this plugin).
     *
     * @throws org.janelia.it.ims.tmog.plugin.ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws org.janelia.it.ims.tmog.plugin.ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        if (EventType.END_ROW_SUCCESS.equals(eventType)) {
            removeSample(row);
        }
        return row;
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

    private void removeSample(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        String currentUrl = null;
        try {
            final Map<String, DataField> fieldMap = new HashMap<String, DataField>(row.getDisplayNameToFieldMap());
            List<String> urlList = urlTokens.deriveValues(fieldMap, true);
            for (String url : urlList) {
                currentUrl = url;
                submitDeleteSampleRequest(currentUrl);
            }
        } catch (ExternalDataException e) {
            throw e;
        } catch (ExternalSystemException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalSystemException(
                    "Failed to submit delete request for '" + currentUrl +
                    "' because of a system error.", e);
        }

    }

    private void submitDeleteSampleRequest(String url)
            throws ExternalDataException, ExternalSystemException {

        int responseCode;
        DeleteMethod method = null;
        try {
            method = new DeleteMethod(url);
            responseCode = httpClient.executeMethod(method);
            LOG.info("submitDeleteSampleRequest: " + responseCode + " returned for " + url);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                LOG.error("submitDeleteSampleRequest: request failed for " + url);
            }
        } catch (Exception e) {
            LOG.error("failed to submit delete request for " + url, e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }

    }

    private static final Log LOG = LogFactory.getLog(JacsSampleRemovalPlugin.class);

    private static final String INIT_FAILURE_MSG = "Failed to initialize JACS Sample Removal plug-in.  ";
}