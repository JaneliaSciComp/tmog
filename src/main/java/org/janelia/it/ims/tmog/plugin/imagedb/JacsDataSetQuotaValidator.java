/*
 * Copyright (c) 2017 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.StaticDataModel;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PropertyTokenList;
import org.janelia.it.ims.tmog.plugin.SimpleRowValidator;

/**
 * This validator queries the JFS storage quota service to prevent processing
 * of samples for JACS data sets that have exceeded storage limits.
 *
 * Since this validation is nice-to-have (and not critical), any connection or data issues
 * with the quota service are simply logged.
 * Validation only fails (or warns) when a successfully parsed quota service response
 * indicates a quota has been exceeded (or is close to being exceeded).
 *
 * To reduce network traffic, quota service responses are cached for a default period of one hour.
 *
 * NOTE:
 *
 * The quota service expects requests to contain a subject name instead of
 * a data set. This validator maps data sets to subject names based upon the
 * JACS convention of prefixing data set names with the owner's subject name
 * (e.g. the subject name for data set 'nerna_polarity_case_3' is 'nerna').
 *
 * @author Eric Trautman
 */
public class JacsDataSetQuotaValidator
        extends SimpleRowValidator {

    public static final String DATA_SET_COLUMN_PROPERTY = "dataSetColumnName";
    public static final String SUBJECT_NAME_PATTERN_PROPERTY = "subjectNamePattern";
    public static final String SERVICE_URL_PROPERTY = "serviceUrl";
    public static final String TEST_DATA_SET_PROPERTY = "testDataSet";
    public static final String CLEAR_CACHE_DURATION_PROPERTY = "clearCacheDuration";

    public static final String DEFAULT_DATA_SET_COLUMN_NAME = "Data Set";
    public static final String DEFAULT_SUBJECT_NAME_PATTERN = "^([^_]+)_.*";

    private HttpClient httpClient;

    /** Parsed configuration tokens for deriving a row specific URL. */
    private PropertyTokenList urlTokens;

    /** Name of column containing data set name. */
    private String dataSetColumnName;

    /** Pattern for parsing web service subject name from data set name. */
    private Pattern subjectNamePattern;

    /**
     * The maximum amount of time (in milliseconds) between cache references before the cache should be cleared.
     * This is intended to keep the cache from getting stale.
     */
    private long clearCacheDuration;

    /** Time the cache was last referenced. */
    private long lastCacheAccessTime;

    /** Maps data set names to retrieved (cached) quota data. */
    private Map<String, DataSetQuota> dataSetToQuotaMap;

    /**
     * Empty constructor required by {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public JacsDataSetQuotaValidator() {
        this.clearCacheDuration = 60 * 60 * 1000; // one hour
        this.lastCacheAccessTime = System.currentTimeMillis();
        this.dataSetToQuotaMap = new ConcurrentHashMap<>();
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

        final String serviceUrl = getRequiredProperty(SERVICE_URL_PROPERTY, config);
        final String testDataSet = getRequiredProperty(TEST_DATA_SET_PROPERTY, config);

        dataSetColumnName = config.getProperty(DATA_SET_COLUMN_PROPERTY);
        if (dataSetColumnName == null) {
            dataSetColumnName = DEFAULT_DATA_SET_COLUMN_NAME;
        }

        String subjectNamePatternString = config.getProperty(SUBJECT_NAME_PATTERN_PROPERTY);
        if (subjectNamePatternString == null) {
            subjectNamePatternString = DEFAULT_SUBJECT_NAME_PATTERN;
        }

        final String configuredClearCacheDuration = config.getProperty(CLEAR_CACHE_DURATION_PROPERTY);

        try {
            subjectNamePattern = Pattern.compile(subjectNamePatternString);

            if (configuredClearCacheDuration != null) {
                this.clearCacheDuration = Long.parseLong(configuredClearCacheDuration);
            }

            this.urlTokens = new PropertyTokenList(serviceUrl, config.getProperties());

        } catch (Exception e) {
            throw new ExternalSystemException(
                    "Failed to initialize ResourceValidator plugin.  " +
                    e.getMessage(),
                    e);
        }

        try {
            getQuota(testDataSet);
        } catch (Exception e) {
            LOG.warn("Failed to retrieve quota information for test data set.  " +
                     "Ignoring error since service may simply be unavailable.", e);
        }
    }

    /**
     * Validate that the resource for the specified row exists.
     *
     * @param  sessionName  unique name for session being validated.
     * @param  row          the user supplied information to be validated.
     *
     * @throws ExternalDataException
     *   if the data is not valid.
     *
     * @throws ExternalSystemException
     *   if any error occurs while validating the data.
     */
    public void validate(String sessionName,
                         PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        final String dataSet = row.getCoreValue(dataSetColumnName);

        clearCacheIfStale();

        DataSetQuota dataSetQuota = dataSetToQuotaMap.get(dataSet);
        if (dataSetQuota == null) {

            try {
                dataSetQuota = getQuota(dataSet);
            } catch (Exception e) {
                dataSetQuota = UNDEFINED_QUOTA;
                LOG.warn("failed to validate quota for data set '" + dataSet + "', ignoring error", e);
            }

            dataSetToQuotaMap.put(dataSet, dataSetQuota);
        }


        if (dataSetQuota.isFail()) {
            throw new ExternalDataException("Quota validation for data set '" + dataSet + "' failed.\n\n" +
                                            "Detailed error is:\n" + dataSetQuota.getDetails());
        } else if (dataSetQuota.isNewWarning()) {
            throw new ExternalDataException("A quota warning for data set '" + dataSet + "' exists.\n" +
                                            "You can ignore this warning by resubmitting.\n\n" +
                                            "Detailed warning is:\n" + dataSetQuota.getDetails());
        }

    }

    private String getRequiredProperty(String propertyName,
                                       PluginConfiguration config)
            throws ExternalSystemException {
        final String value = config.getProperty(propertyName);
        if ((value == null) || (value.length() == 0)) {
            throw new ExternalSystemException(
                    "Failed to initialize JacsDataSetQuotaValidator plugin.  The '" +
                    propertyName + "' property must be defined.");
        }
        return value;
    }

    private synchronized DataSetQuota getQuota(String dataSet) {

        DataSetQuota dataSetQuota;

        // construct URL
        final String subjectName;
        final Matcher m = subjectNamePattern.matcher(dataSet);
        if (m.matches() && (m.groupCount() == 1)) {
            subjectName = m.group(1);
        } else {
            throw new IllegalArgumentException(
                    "Failed to determine subject name for data set '" + dataSet +
                    "'.  Please verify the configured " + SUBJECT_NAME_PATTERN_PROPERTY + " is accurate.");
        }

        final Map<String, DataField> fieldMap = new HashMap<>();
        fieldMap.put(dataSetColumnName, new StaticDataModel(dataSetColumnName, subjectName));
        final List<String> urlList = urlTokens.deriveValues(fieldMap, true);
        final String url = urlList.get(0);

        GetMethod method = new GetMethod(url);
        try {

            LOG.info("getQuota: sending GET " + url + " for data set '" + dataSet + "'");

            int responseCode = httpClient.executeMethod(method);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IllegalArgumentException(
                        "HTTP request failed with response code " + responseCode + ".  " + getErrorContext(url));
            }

            dataSetQuota = DataSetQuota.fromJson(method.getResponseBodyAsString());

        } catch (IOException e) {
            throw new IllegalArgumentException("HTTP request failed.  " + getErrorContext(url), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse HTTP response.  " + getErrorContext(url), e);
        } finally {
            method.releaseConnection();
        }

        LOG.info("getQuota: retrieved " + dataSetQuota);

        return dataSetQuota;
    }


    private String getErrorContext(String url) {
        return "Please verify the configured " + SERVICE_URL_PROPERTY + " '" + url +
               "' is accurate and that the corresponding service is available.";
    }

    /**
     * Clears the cache if it has not been accessed recently.
     */
    private synchronized void clearCacheIfStale() {

        if ((System.currentTimeMillis() - lastCacheAccessTime) > clearCacheDuration) {
            LOG.info("clearCacheIfStale: clearing cache containing " + dataSetToQuotaMap.size() + " items");
            dataSetToQuotaMap.clear();
        }

        lastCacheAccessTime = System.currentTimeMillis();
    }

    private static class DataSetQuota {

        private String state;
        private String details;
        private transient boolean isOldWarning;

        public DataSetQuota() {
            this.state = "UNKNOWN";
            this.details = "Not available";
            this.isOldWarning = false;
        }

        public static DataSetQuota fromJson(String queryResponseString) {
            return GSON.fromJson(queryResponseString, DataSetQuota.class);
        }

        public boolean isFail() {
            // TODO: remove hack to skip blocking of failure cases requested by Oz and Rob
            return false; //"FAIL".equalsIgnoreCase(state);
        }

        public synchronized boolean isNewWarning() {
            boolean isNewWarning = false;
            // TODO: remove hack to skip blocking of failure cases requested by Oz and Rob
//            if ((! isOldWarning) && "WARN".equalsIgnoreCase(state)) {
            if ((! isOldWarning) && ("WARN".equalsIgnoreCase(state) || "FAIL".equalsIgnoreCase(state))) {
                isOldWarning = true;
                isNewWarning = true;
            }
            return isNewWarning;
        }

        public String getDetails() {
            return details;
        }

        @Override
        public String toString() {
            return GSON.toJson(this);
        }
    }

    private static final Logger LOG = LogManager.getLogger(JacsDataSetQuotaValidator.class);
    private static final Gson GSON = new Gson();
    private static final DataSetQuota UNDEFINED_QUOTA = new DataSetQuota();

}