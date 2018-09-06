/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.dataFile;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.ObjectParamRule;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PropertyTokenList;
import org.janelia.it.ims.tmog.plugin.RowUpdater;
import org.janelia.it.utils.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This plug-in loads data from an HTTP web service
 * that can be used to populate fields.
 *
 * @author Eric Trautman
 */
public class DataResourcePlugin
        implements RowUpdater {

    /**
     * Name of the property that identifies the tokenized query service URL.
     */
    public static final String SERVICE_URL_PROPERTY_NAME = "plugin.service-url";

    /**
     * Name of the property that identifies an existing resource URL
     * to use for testing during initialization.
     */
    public static final String TEST_URL_PROPERTY_NAME = "plugin.test-url";

    /**
     * Name of the property to use for overriding the default clear
     * cache duration (60,000 milliseconds - 1 minute).
     */
    public static final String CLEAR_CACHE_DURATION_PROPERTY_NAME =
            "plugin.clear-cache-duration";

    /**
     * Name of property that defines the root XPath for a data item
     * parsed from the HTTP response stream.
     */
    public static final String ROOT_XPATH_PROPERTY_NAME = "plugin.root-xpath";

    /** Parsed configuration tokens for deriving a row specific URL. */
    private PropertyTokenList urlTokens;

    /** Maps row fields to their XPath relative to the root XPath.  */
    private Map<String, String> rowFieldNameToXPathMap;

    /** HTTP client for issuing requests. */
    private HttpClient httpClient;

    /** Digester instance used to parse HTTP responses. */
    private Digester digester;

    /**
     * The maximum amount of time (in milliseconds) between cache
     * references before the cache should be cleared.  This is intended to
     * keep the cache from getting stale.
     */
    private long clearCacheDuration;

    /** Time the cache was last referenced. */
    private long lastCacheAccessTime;

    /** Cache of row derived URLs to parsed response data items. */
    private Map<String, Item> urlToItemCache;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public DataResourcePlugin() {
        this.clearCacheDuration = 60 * 1000; // one minute
        this.lastCacheAccessTime = System.currentTimeMillis();
        this.rowFieldNameToXPathMap = new HashMap<String, String>();
        this.httpClient = new HttpClient();
        this.urlToItemCache = new HashMap<String, Item>();
    }

    /**
     * Initializes the plug-in and verifies that it is ready for use by
     * checking external dependencies.
     *
     * @param  config  the plugin configuration.
     *
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config) throws ExternalSystemException {

        final Map<String, String> props = config.getProperties();

        rowFieldNameToXPathMap.clear();

        String serviceUrl = null;
        String testUrl = null;
        String rootXPath = null;

        String value;
        for (String key : props.keySet()) {

            value = props.get(key);

            if (SERVICE_URL_PROPERTY_NAME.equals(key)) {

                serviceUrl = value;
                urlTokens = new PropertyTokenList(serviceUrl,
                                                  config.getProperties());

            } else if (TEST_URL_PROPERTY_NAME.equals(key)) {

                testUrl = value;

            } else if (CLEAR_CACHE_DURATION_PROPERTY_NAME.equals(key)) {

                clearCacheDuration = Long.parseLong(value);

            } else if (ROOT_XPATH_PROPERTY_NAME.equals(key)) {

                rootXPath = value;

            } else if (StringUtil.isDefined(key) &&
                       StringUtil.isDefined(value)) {

                rowFieldNameToXPathMap.put(key, value);
            }
        }

        checkRequiredProperty(SERVICE_URL_PROPERTY_NAME, serviceUrl);
        checkRequiredProperty(ROOT_XPATH_PROPERTY_NAME, rootXPath);

        if (rowFieldNameToXPathMap.size() == 0) {
            throw new ExternalSystemException(
                    getInitFailureMsg() +
                    "At least one field to XPath mapping must be specified.");
        }

        setDigester(rootXPath);

        addEmptyItemToCache();
        
        if (testUrl != null) {
            final Item testItem = fetchItem(testUrl);
            if (testItem == null) {
                throw new ExternalSystemException(
                        getInitFailureMsg() + "The " + TEST_URL_PROPERTY_NAME +
                        " property '" + testUrl +
                        "' identifies a non-existent resource.");
            } else if (testItem.size() == 0) {
                throw new ExternalSystemException(
                        getInitFailureMsg() + "The " + TEST_URL_PROPERTY_NAME +
                        " property '" + testUrl +
                        "' does not return any mapped values.  There may " +
                        "be a problem with the configured XPath values.");
            }
        }

        LOG.info("init: mapped " + rowFieldNameToXPathMap.size() +
                 " data resource fields");
    }

    /**
     * Allows plug-in to update the specified row.
     *
     * @param  row  row to be updated.
     *
     * @return the data field row for processing (with any
     *         updates from this plugin).
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     *
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow updateRow(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        final Item item = getMappedItemForRow(row);
        if (item != null) {
            for (String fieldName : rowFieldNameToXPathMap.keySet()) {
                row.applyPluginDataValue(fieldName,
                                         item.getPropertyValue(fieldName));
            }
        }
        return row;
    }

    protected Item getMappedItemForRow(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        Item item = null;

        final String url = getUrlForRow(row);
        if (url != null) {
            clearCacheIfStale();
            item = urlToItemCache.get(url);
            if ((item == null) && (! urlToItemCache.containsKey(url))) {
                item = fetchItem(url);
                cacheItem(url, item);
            }
        }

        return item;
    }

    protected String getUrlForRow(PluginDataRow row) {
        String url = null;
        final Map<String, DataField> fieldMap = row.getDisplayNameToFieldMap();
        final List<String> urlList = urlTokens.deriveValues(fieldMap, true);
        if (urlList.size() > 0) {
            url = urlList.get(0);
        }
        return url;
    }

    protected String getInitFailureMsg() {
        return "Failed to initialize Data Resource plug-in.  ";
    }

    protected Map<String, String> getRowFieldNameToXPathMap() {
        return rowFieldNameToXPathMap;
    }

    protected synchronized void removeItem(String url) {
        urlToItemCache.remove(url);
    }

    private synchronized void cacheItem(String url,
                                        Item item) {
        urlToItemCache.put(url, item);
    }

    private void checkRequiredProperty(String name,
                                       String value)
            throws ExternalSystemException {
        if ((value == null) || (value.length() == 0)) {
            throw new ExternalSystemException(
                    getInitFailureMsg() + "The '" + name +
                    "' property must be defined.");
        }
    }

    private void setDigester(String rootXPath) {

        digester = new Digester();
        digester.setValidating(false);

        digester.addObjectCreate(rootXPath, Item.class);

        String path;
        for (String fieldName : rowFieldNameToXPathMap.keySet()) {
            path = rootXPath + "/" + rowFieldNameToXPathMap.get(fieldName);
            digester.addObjectCreate(path, Property.class);
            digester.addCallMethod(path, "setNameAndValue", 2);
            digester.addRule(path, new ObjectParamRule(0, fieldName));
            digester.addCallParam(path, 1);
            digester.addSetNext(path, "addProperty");
        }
    }

    private synchronized void clearCacheIfStale() {

        if ((System.currentTimeMillis() - lastCacheAccessTime) > clearCacheDuration) {
            LOG.info("clearing cache containing " +
                     urlToItemCache.size() + " items");
            urlToItemCache.clear();
            addEmptyItemToCache();
        }

        lastCacheAccessTime = System.currentTimeMillis();
    }

    /**
     * Adds empty URL to cache so that we don't waste time making a request
     * for it later.
     */
    private void addEmptyItemToCache() {
        final Map<String, DataField> emptyMap =
                new HashMap<String, DataField>();
        final List<String> urlList = urlTokens.deriveValues(emptyMap, true);
        if (urlList.size() > 0) {
            urlToItemCache.put(urlList.get(0), null);
        }
    }

    private Item fetchItem(String url)
            throws ExternalSystemException {

        Item item = null;

        int responseCode;
        InputStream responseStream = null;
        GetMethod method = null;
        try {
            method = new GetMethod(url);
            responseCode = httpClient.executeMethod(method);
            LOG.info("fetchItem: " + responseCode + " returned for " + url);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                responseStream = method.getResponseBodyAsStream();
                item = (Item) digester.parse(responseStream);
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                item = null;
            } else {
                throw new ExternalSystemException(
                        "Unexpected response code (" + responseCode +
                        ") returned for " + url + ".");
            }
        } catch (IOException e) {
            throw new ExternalSystemException(
                    "Failed to send request for " + url + ".", e);
        } catch (Exception e) {
            throw new ExternalSystemException(
                    "Failed to parse HTTP response for " + url + ".", e);
        } finally {
            if (responseStream != null) {
                try {
                    responseStream.close();
                } catch (IOException e) {
                    LOG.error("failed to close response stream, " +
                              "ignoring error", e);
                }
            }
            if (method != null) {
                method.releaseConnection();
            }
        }

        return item;
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(DataResourcePlugin.class);
}