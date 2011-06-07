/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.wormtracker;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowListener;
import org.janelia.it.utils.StringUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;

/**
 * This plug-in creates HTTP requests to store collected data in the
 * worm tracker database using the worm tracker web service.
 *
 * @author Eric Trautman
 */
public class WormTrackerDataPlugin implements RowListener {

    /** The logger for this class. */
    private static final Log LOG =
            LogFactory.getLog(WormTrackerDataPlugin.class);

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize the Worm Tracker Data plug-in.  ";

    /**
     * URL for the data server
     * (should include everything except the resource name).
     */
    private String dataServerUrlName;

    /**
     * Name of the data field that contains the resource
     * name to include in each request.
     */
    private String resourceFieldName;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public WormTrackerDataPlugin() {
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

        this.dataServerUrlName = config.getProperty("dataServerUrl");
        if ((this.dataServerUrlName == null) ||
            (this.dataServerUrlName.length() == 0)) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "Please configure a dataServerUrl for the plug-in.");
        } else if (! this.dataServerUrlName.endsWith("/")) {
            this.dataServerUrlName += '/';
        }

        URI uri = null;
        int responseCode;
        try {
            HttpMethod method = new GetMethod(this.dataServerUrlName +
                                              "is-service-available");
            uri = method.getURI();
            LOG.info("init: execute GET " + uri);
            HttpClient httpClient = new HttpClient();
            responseCode = httpClient.executeMethod(method);

        } catch (UnknownHostException e) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "The host for '" +
                    this.dataServerUrlName + "' cannot be found.",
                    e);
        } catch (Throwable t) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "A request for '" + uri +
                    "' could not be completed because: " + t.getMessage(),
                    t);
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "The request for '" + uri +
                    "' failed with response code " + responseCode + ".");
        }

        this.resourceFieldName = config.getProperty("resourceField");
        if ((this.resourceFieldName == null) ||
            (this.resourceFieldName.length() == 0)) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "Please configure a resourceField for the plug-in.");
        }
    }

    /**
     * Processes the specified event.
     *
     * @param  eventType  type of event.
     * @param  row        details about the event.
     *
     * @return the data row for processing (with any
     *         updates from this plugin).
     *
     * @throws org.janelia.it.ims.tmog.plugin.ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        if (EventType.END_ROW_SUCCESS.equals(eventType)) {
            try {
                sendRequest(row);
            } catch (ExternalDataException e) {
                logFailure(eventType, row, e);
                throw e;
            } catch (ExternalSystemException e) {
                logFailure(eventType, row, e);
                throw e;
            } catch (Throwable t) {
                logFailure(eventType, row, t);
                throw new ExternalSystemException(t.getMessage(), t);
            }
        }

        return row;
    }

    /**
     * Converts the specified row data into an HTTP POST request and then
     * sends the request.
     *
     * @param  row  data to convert.
     *
     * @throws ExternalDataException
     *   if the specified row does not contain a resource field.
     *
     * @throws ExternalSystemException
     *   if any other errors occur.
     */
    private void sendRequest(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        String resourceField = row.getCoreValue(resourceFieldName);
        if ((resourceField == null) || (resourceField.length() == 0)) {
            throw new ExternalDataException(
                    "The required resource field '" + resourceFieldName +
                    "' has not been provided.  " +
                    "Please verify your configuration.");
        }

        String encodedResource;
        try {
            encodedResource = URLEncoder.encode(resourceField,
                                                "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ExternalSystemException(
                    "Unable to find UTF-8 encoding.", e);
        }

        PutMethod method = new PutMethod(this.dataServerUrlName +
                                         encodedResource);

        StringBuilder xml = new StringBuilder(256);

        xml.append("<experiment>");
        DataRow dataRow = row.getDataRow();
        String fieldName;
        String fieldValue;
        // TODO: handle nested fields
        for (DataField field : dataRow.getFields()) {
            fieldName = field.getDisplayName();
            if (! resourceFieldName.equals(fieldName)) {
                fieldValue = field.getCoreValue();
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    xml.append("<property type=\"");
                    xml.append(StringUtil.getDefinedXmlValue(fieldName));
                    xml.append("\">");
                    xml.append(StringUtil.getDefinedXmlValue(fieldValue));
                    xml.append("</property>");
                }
            }
        }
        xml.append("</experiment>");


        int responseCode;
        URI requestUri = null;
        try {
            requestUri = method.getURI();
            
            StringRequestEntity requestEntity =
                    new StringRequestEntity(
                            xml.toString(),
                            "application/janelia-wormtracker+xml",
                            "UTF-8");
            method.setRequestEntity(requestEntity);

            if (LOG.isInfoEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("sendRequest: PUT ");
                sb.append(requestUri);
                sb.append(", xml=");
                sb.append(xml);
                LOG.info(sb.toString());
            }

            HttpClient httpClient = new HttpClient();
            responseCode = httpClient.executeMethod(method);

        } catch (IOException e) {
            throw new ExternalSystemException(
                    "The request for '" + requestUri +
                    "' failed.  The detailed error was: " + e.getMessage() +
                    ".");
        } finally {
            method.releaseConnection();
        }

        if (responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
            throw new ExternalSystemException(
                    "The request for '" + requestUri +
                    "' failed with response code " + responseCode + ".");
        }
    }

    private void logFailure(EventType eventType,
                            PluginDataRow row,
                            Throwable t) {
        LOG.error("Failed to process " + eventType +
                  " event for " + row, t);
    }

}