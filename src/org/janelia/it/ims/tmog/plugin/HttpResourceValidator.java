/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.config.PluginConfiguration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class validates that a resource exists by submitting an
 * HTTP Head request to a web service.
 *
 * @author Eric Trautman
 */
public class HttpResourceValidator
        implements RowValidator {

    /**
     * Name of the property that identifies the tokenized query service URL.
     */
    public static final String SERVICE_URL_NAME = "serviceUrl";

    /**
     * Name of the property that identifies an existing resource URL
     * to use for testing during initialization.
     */
    public static final String TEST_URL_NAME = "testUrl";

    /**
     * Name of the property that identifies the error message template
     * to use for resources that are not found.
     */
    public static final String ERROR_MESSAGE_NAME = "errorMessage";

    /**
     * Name of the property to use for overriding the default clear
     * cache duration (60,000 milliseconds).
     */
    public static final String CLEAR_CACHE_DURATION_NAME = "clearCacheDuration";

    /** HTTP client for validation requests. */
    private HttpClient httpClient;

    /** Parsed configuration tokens for deriving a row specific URL. */
    private List<PropertyToken> urlTokens;

    /** Parsed configuration tokens for invalid resource error messages. */
    private List<PropertyToken> errorMessageTokens;

    /**
     * The maximum amount of time (in milliseconds) between cache
     * references before the cache should be cleared.  This is intended to
     * keep the cache from getting stale.
     */
    private long clearCacheDuration;

    /** Time the cache was last referenced. */
    private long lastCacheAccessTime;

    /**
     * Cache for previously validated names.
     */
    private Set<String> validNames;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public HttpResourceValidator() {
        this.clearCacheDuration = 60 * 1000; // one minute
        this.lastCacheAccessTime = System.currentTimeMillis();
        this.validNames = new HashSet<String>();
        this.httpClient = new HttpClient();
    }

    /**
     * Verifies that the plugin is ready for use.
     *
     * @param  config  the plugin configuration.
     *
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config) throws ExternalSystemException {

        final String serviceUrl = getRequiredProperty(SERVICE_URL_NAME, config);
        final String testUrl = getRequiredProperty(TEST_URL_NAME, config);
        final String errorMessage = getRequiredProperty(ERROR_MESSAGE_NAME,
                                                        config);
        final String configuredClearCacheDuration =
                config.getProperty(CLEAR_CACHE_DURATION_NAME);

        try {
            if (configuredClearCacheDuration != null) {
                this.clearCacheDuration =
                        Long.parseLong(configuredClearCacheDuration);
            }

            this.urlTokens = PropertyToken.parseTokens(serviceUrl);

            if (! isResourceFound(testUrl)) {
                throw new IllegalArgumentException(
                        "The " + TEST_URL_NAME + " property '" + testUrl +
                        "' identifies a non-existent resource.");
            }

            this.errorMessageTokens = PropertyToken.parseTokens(errorMessage);

        } catch (Exception e) {
            throw new ExternalSystemException(
                    "Failed to initialize ResourceValidator plugin.  " +
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Validate that the resource for the specified row exists.
     *
     * @param  row  the user supplied meta-data to be validated.
     *
     * @throws ExternalDataException
     *   if the data is not valid.
     *
     * @throws ExternalSystemException
     *   if any error occurs while validating the data.
     */
    public void validate(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        String url = null;
        try {
            url = getServiceUrl(row);
            clearCacheIfStale();
            boolean isValid = validNames.contains(url);
            if (! isValid) {
                if (isResourceFound(url)) {
                    addNameToCache(url);
                } else {
                    final String msg =
                            PropertyToken.deriveString(row, errorMessageTokens);
                    throw new ExternalDataException(msg);
                }
            }
        } catch (ExternalDataException e) {
            throw e;
        } catch (ExternalSystemException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalSystemException(
                    "Failed to retrieve resource '" + url +
                    "' because of a system error.", e);
        }
    }

    private String getRequiredProperty(String propertyName,
                                       PluginConfiguration config)
            throws ExternalSystemException {
        final String value = config.getProperty(propertyName);
        if ((value == null) || (value.length() == 0)) {
            throw new ExternalSystemException(
                    "Failed to initialize HttpResourceValidator plugin.  The '" + 
                    propertyName + "' property must be defined.");
        }
        return value;
    }

    private String getServiceUrl(PluginDataRow row)
            throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        String tokenValue;
        for (PropertyToken token : urlTokens) {
            tokenValue = token.getValue(row);
            if (! token.isLiteral()) {
                tokenValue = URLEncoder.encode(tokenValue, "UTF-8");
            }
            sb.append(tokenValue);
        }
        return sb.toString();
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
                        ") returned for " + url + ".  " + getErrorContext(url));
            }
        } catch (IOException e) {
            throw new ExternalSystemException(
                    "Failed to confirm that resource at " + url + " exists.  " +
                    getErrorContext(url), e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }

        return isFound;
    }

    private String getErrorContext(String url) {
        return "Please verify the configured " + SERVICE_URL_NAME +
               " '" + url + "' is accurate and that the corresponding " +
               "service is available.";
    }

    /**
     * Clears the cache if it has not been accessed recently.
     */
    private synchronized void clearCacheIfStale() {

        if ((System.currentTimeMillis() - lastCacheAccessTime) >
            clearCacheDuration) {

            LOG.info("clearing cache containing " +
                     validNames.size() + " items");
            validNames.clear();
        }

        lastCacheAccessTime = System.currentTimeMillis();
    }

    /**
     * Adds the specified name to the cache.
     *
     * @param  name  name to add.
     */
    private synchronized void addNameToCache(String name) {
        validNames.add(name);
    }

    private static final Log LOG =
            LogFactory.getLog(HttpResourceValidator.class);
}