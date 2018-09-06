/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.apache.commons.digester.Digester;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.StaticDataModel;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PropertyTokenList;
import org.janelia.it.ims.tmog.plugin.RelativePathUtil;
import org.janelia.it.ims.tmog.plugin.RowListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This plug-in invokes the sageLoader via HTTP request.
 *
 * @author Eric Trautman
 */
public class SageLoaderPlugin
        implements RowListener {

    public static final String RELATIVE_PATH_DEPTH_NAME = "relativePathDepth";
    public static final String SERVICE_URL_NAME = "serviceUrl";
    public static final String ITEM_PARAMETER_NAME = "item";
    public static final String OWNER_PARAMETER_NAME = "owner";
    public static final String CONFIG_PARAMETER_NAME = "config";
    public static final String GRAMMAR_PARAMETER_NAME = "grammar";
    public static final String LAB_PARAMETER_NAME = "lab";
    public static final String[] REQUIRED_QUERY_PARAMETERS =
            {CONFIG_PARAMETER_NAME, GRAMMAR_PARAMETER_NAME, LAB_PARAMETER_NAME};
    public static final String TEST_URL_NAME = "testUrl";

    private static final String RELATIVE_PATH_TOKEN_NAME = "relativePath";

    /** The number of parent directories to include in item value. */
    private int relativePathDepth = 1;

    /** HTTP client for sageLoader requests. */
    private HttpClient httpClient;

    /** Parsed configuration tokens for deriving a row specific URL. */
    private PropertyTokenList urlTokens;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public SageLoaderPlugin() {
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
        Map<String, String> serviceQueryParameters =
                new LinkedHashMap<String, String>();

        // item parameter is always the file's relative path
        serviceQueryParameters.put(ITEM_PARAMETER_NAME,
                                   "${" + RELATIVE_PATH_TOKEN_NAME + '}');

        final Map<String, String> props = config.getProperties();
        String value;
        for (String key : props.keySet()) {
            value = props.get(key);
            if (RELATIVE_PATH_DEPTH_NAME.equals(key)) {
                this.relativePathDepth = getPathDepth(value);
            } else if (SERVICE_URL_NAME.equals(key)) {
                serviceUrl = value;
            } else if (TEST_URL_NAME.equals(key)) {
                testUrl = value;
            } else {
                if (! value.contains(PropertyTokenList.TOKEN_ID)) {
                    value = getEncodedValue(value);
                }
                serviceQueryParameters.put(key, value);
            }
        }

        // validate configuration
        checkRequiredProperty(SERVICE_URL_NAME, serviceUrl);
        for (String pName : REQUIRED_QUERY_PARAMETERS) {
            checkRequiredProperty(pName, serviceQueryParameters.get(pName));
        }
        checkRequiredProperty(TEST_URL_NAME, testUrl);

        if (! props.containsKey(OWNER_PARAMETER_NAME)) {
            serviceQueryParameters.put(OWNER_PARAMETER_NAME, "tmog");
        }

        // build properly encoded full service URL from config parameters
        StringBuilder serviceUrlWithParameters = new StringBuilder();
        serviceUrlWithParameters.append(serviceUrl);
        char sepChar = '?';
        String parameterValue;
        for (String name : serviceQueryParameters.keySet()) {
            parameterValue = serviceQueryParameters.get(name);
            serviceUrlWithParameters.append(sepChar);
            serviceUrlWithParameters.append(name);
            serviceUrlWithParameters.append('=');
            serviceUrlWithParameters.append(parameterValue);
            sepChar = '&';
        }

        LOG.info("init: service URL with parameters is " +
                 serviceUrlWithParameters);

        try {

            this.urlTokens =
                    new PropertyTokenList(serviceUrlWithParameters.toString(),
                                          props);

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
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        if (EventType.END_ROW_SUCCESS.equals(eventType)) {
            runSageLoader(row);
        }
        return row;
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
                    "The '" + RELATIVE_PATH_DEPTH_NAME + "' property '" +
                    value + "' must be a positive integer value.");
        }
        return depth;
    }

    private String getEncodedValue(String value)
            throws ExternalSystemException {
        String encodedValue;
        try {
            encodedValue = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    ".  Failed to encode '" + value + "'", e);
        }
        return encodedValue;
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

    private void runSageLoader(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        String currentUrl = null;
        try {
            final Map<String, DataField> fieldMap =
                    new HashMap<String, DataField>(
                            row.getDisplayNameToFieldMap());
            final String relativePath = RelativePathUtil.getRelativePath(
                    row.getTargetFile(), relativePathDepth);

            fieldMap.put(RELATIVE_PATH_TOKEN_NAME,
                         new StaticDataModel(RELATIVE_PATH_TOKEN_NAME,
                                             relativePath));

            List<String> urlList = urlTokens.deriveValues(fieldMap, true);
            for (String url : urlList) {
                currentUrl = url;
                postSageLoaderRequest(currentUrl, relativePath);
            }
        } catch (ExternalDataException e) {
            throw e;
        } catch (ExternalSystemException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalSystemException(
                    "Failed to retrieve resource '" + currentUrl +
                    "' because of a system error.", e);
        }

    }

    private void postSageLoaderRequest(String url,
                                       String relativePath)
            throws ExternalDataException, ExternalSystemException {

        int responseCode;
        PostMethod method = null;
        try {
            method = new PostMethod(url);
            responseCode = httpClient.executeMethod(method);
            LOG.info("postSageLoaderRequest: " + responseCode +
                     " returned for " + url);
            if (responseCode == HttpURLConnection.HTTP_ACCEPTED) {
                final String statusLink =
                        getStatusLink(method.getResponseBodyAsStream());
                LOG.info("postSageLoaderRequest: status link for " +
                         relativePath + " is " + statusLink);
            } else {
                LOG.info("postSageLoaderRequest: request failed for " +
                         relativePath);
            }
        } catch (Exception e) {
            LOG.error("failed to post sageLoader request " + url, e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }

    }

    private Digester getStatusDigester(List<String> statusLinkList) {

        Digester digester = new Digester();
        digester.setValidating(false);

        digester.push(statusLinkList);
        final String statusLinkPath = "currentTaskStatus/href";
        digester.addCallMethod(statusLinkPath, "add", 1);
        digester.addCallParam(statusLinkPath, 0);

        return digester;
    }

    protected String getStatusLink(InputStream responseStream) {

        String statusLink = null;

        try {
            ArrayList<String> statusLinkList = new ArrayList<String>();
            Digester digester = getStatusDigester(statusLinkList);
            digester.parse(responseStream);
            if (statusLinkList.size() > 0) {
                statusLink = statusLinkList.get(0);
            }
        } catch (Exception e) {
            LOG.error("failed to parse sageLoader response", e);
        }

        return statusLink;
    }

    private static final Log LOG =
            LogFactory.getLog(SageLoaderPlugin.class);

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize SageLoader plug-in.  ";
}