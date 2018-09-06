/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.apache.commons.digester.Digester;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.janelia.it.utils.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This model supports selecting a value from a predefined set of values.
 * Selectable values are retrieved at start-up via HTTP request.
 *
 * @author Eric Trautman
 */
public class HttpValidValueModel
        extends ValidValueModel {

    private String serviceUrl;
    private boolean displayNamePrefixedForValues;
    private String valueCreationPath;
    private String relativeActualValuePath;
    private String relativeValueDisplayNamePath;

    private List<ValidValue> staticValues;

    private static Map<String, HttpValidValueModel> urlToModelMap =
            new ConcurrentHashMap<String, HttpValidValueModel>();

    public HttpValidValueModel() {
        this.staticValues = null;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public boolean isDisplayNamePrefixedForValues() {
        return displayNamePrefixedForValues;
    }

    public void setDisplayNamePrefixedForValues(boolean displayNamePrefixedForValues) {
        this.displayNamePrefixedForValues = displayNamePrefixedForValues;
    }

    /**
     * Sets the XML path for creating a new value object
     * to be added to this model's list of valid values.
     *
     * @param  valueCreationPath  XML path for creating a new value.
     */
    public void setValueCreationPath(String valueCreationPath) {
        this.valueCreationPath = valueCreationPath;
    }

    /**
     * Sets the XML path (relative to the valueCreationPath) for setting the
     * current value object's actual value.
     *
     * @param  relativeActualValuePath  relative XML path for actual value.
     */
    public void setRelativeActualValuePath(String relativeActualValuePath) {
        this.relativeActualValuePath = relativeActualValuePath;
    }

    /**
     * Sets the XML path (relative to the valueCreationPath) for setting the
     * current value object's display name.
     *
     * @param  relativeValueDisplayNamePath  relative XML path for display name.
     */
    public void setRelativeValueDisplayNamePath(String relativeValueDisplayNamePath) {
        this.relativeValueDisplayNamePath = relativeValueDisplayNamePath;
    }

    /**
     * Clears any existing values in this model and adds a new set of values
     * retrieved via http request.
     *
     * @throws IllegalArgumentException
     *   if any errors occur during retrieval.
     *
     */
    public void retrieveAndSetValidValues()
            throws IllegalArgumentException {

        checkRequiredConfigurationParameter("serviceUrl",
                                            serviceUrl);
        checkRequiredConfigurationParameter("valueCreationPath",
                                            valueCreationPath);
        checkRequiredConfigurationParameter("relativeActualValuePath",
                                            relativeActualValuePath);

        final String cacheKey = getCacheKey();
        HttpValidValueModel cachedModel = urlToModelMap.get(cacheKey);
        if (cachedModel == null) {
            setValidValuesFromService();
            prefixDisplayNamesAndSortAsNeeded();
            urlToModelMap.put(cacheKey, this);
        } else {
            // same config, use cached values directly
            setValuesFromModel(cachedModel);
        }

    }

    /**
     * @return a key for the cache that incorporates all the configuration
     *         values needed to identify an equivalent configuration
     *         whose values can be shared from the cache
     *         (see {@link #prefixDisplayNamesAndSortAsNeeded}).
     */
    private String getCacheKey() {
        return serviceUrl + "|" +
               isDisplayNamePathDefined() + "|" +
               displayNamePrefixedForValues;
    }

    private boolean isDisplayNamePathDefined() {
        return StringUtil.isDefined(relativeValueDisplayNamePath);
    }

    private Digester getDigester() {

        Digester digester = new Digester();
        digester.setValidating(false);

        digester.push(this);
        digester.addObjectCreate(valueCreationPath,
                                 ValidValue.class);
        digester.addSetNext(valueCreationPath, "addValidValue");

        String path = valueCreationPath + "/" + relativeActualValuePath;
        digester.addCallMethod(path, "setValue", 1);
        digester.addCallParam(path, 0);

        if (isDisplayNamePathDefined()) {
            path = valueCreationPath + "/" + relativeValueDisplayNamePath;
            digester.addCallMethod(path, "setDisplayName", 1);
            digester.addCallParam(path, 0);
        }

        return digester;
    }

    private void setValidValuesFromService() {

        if (staticValues == null) {
            staticValues = new ArrayList<ValidValue>(getValidValues());
        }

        clearValidValues();

        InputStream responseStream = null;

        int responseCode;
        GetMethod method = new GetMethod(serviceUrl);
        try {
            Digester digester = getDigester();
            HttpClient httpClient = new HttpClient();
            LOG.info("sending GET " + serviceUrl);
            responseCode = httpClient.executeMethod(method);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException(
                        "HTTP request failed with response code " +
                        responseCode + ".  " + getServiceUrlErrorContext());
            }

            responseStream = method.getResponseBodyAsStream();
            digester.parse(responseStream);

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "HTTP request failed.  " + getServiceUrlErrorContext(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to parse HTTP response.  " +
                    getServiceUrlErrorContext(), e);
        } finally {
            if (responseStream != null) {
                try {
                    responseStream.close();
                } catch (IOException e) {
                    LOG.error("failed to close response stream, " +
                              "ignoring error", e);
                }
            }
            method.releaseConnection();
        }

        LOG.info("retrieved " + this.size() +
                 " results for " + serviceUrl);

        // add any static values to the end of the list
        for (ValidValue value : staticValues) {
            addValidValue(value);
        }

    }

    private void prefixDisplayNamesAndSortAsNeeded() {
        if (isDisplayNamePathDefined()) {
            if (displayNamePrefixedForValues) {
                for (ValidValue validValue : getValidValues()) {
                    validValue.setDisplayNamePrefixedWithValue(true);
                }
            } else {
                sortValues(DISPLAY_NAME_COMPARATOR);
            }
        }
    }

    private void checkRequiredConfigurationParameter(String parameterName,
                                                     String parameterValue)
            throws IllegalArgumentException {
        if (! StringUtil.isDefined(parameterValue)) {
            throw new IllegalArgumentException(
                    "No " + parameterName + " was specified.  " +
                    "Please check the configuration for the '" +
                    getDisplayName() + "' field.");
        }
    }

    private String getServiceUrlErrorContext() {
        return "Please verify the serviceUrl '" + serviceUrl +
               "' configured for the '" + getDisplayName() +
               "' field is accurate and that the corresponding " +
               "service is available.";
    }

    private static final Logger LOG = 
            Logger.getLogger(HttpValidValueModel.class);

    private static final Comparator<ValidValue> DISPLAY_NAME_COMPARATOR =
            new Comparator<ValidValue>() {
                @Override
                public int compare(ValidValue o1,
                                   ValidValue o2) {
                    int result;
                    final String displayName1 = o1.getDisplayName();
                    final String displayName2 = o2.getDisplayName();
                    if (displayName1 == null) {
                        if (displayName2 == null) {
                            final String value1 = o1.getValue();
                            final String value2 = o2.getValue();
                            result = value1.compareTo(value2);
                        } else {
                            result = -1;
                        }
                    } else if (displayName2 == null) {
                        result = 1;
                    } else {
                        result = displayName1.compareTo(displayName2);
                    }
                    return result;
                }
            };
}
