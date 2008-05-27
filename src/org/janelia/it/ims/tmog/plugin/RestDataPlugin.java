/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.DataField;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;

/**
 * This plug-in converts task completion events into HTTP POST requests
 * for a REST data server.
 *
 * @author Eric Trautman
 */
public class RestDataPlugin implements RowListener {

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(RestDataPlugin.class);

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
    public RestDataPlugin() {
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

        this.dataServerUrlName = config.getProperty("dataServerUrl");
        if ((this.dataServerUrlName == null) ||
            (this.dataServerUrlName.length() == 0)) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "Please configure a dataServerUrl for the plug-in.");
        } else if (! this.dataServerUrlName.endsWith("/")) {
            this.dataServerUrlName += '/';
        }

        try {
            HttpMethod method = new GetMethod(this.dataServerUrlName);
            sendRequest(method);
        } catch (Throwable t) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + t.getMessage(),
                    t);
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
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        if (EventType.END_SUCCESS.equals(eventType)) {
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
     * Creates a new HTTP client instance and executes the specified method,
     * logging the request.
     *
     * @param  method  request to execute.
     *
     * @throws ExternalSystemException
     *   if any errors occur.
     */
    private void sendRequest(HttpMethod method)
            throws ExternalSystemException {

        int responseCode;
        URI requestUri = null;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("sendRequest: uri=");
            requestUri = method.getURI();
            sb.append(requestUri);
            sb.append(", method=");
            sb.append(method.getName());
            if (method instanceof PostMethod) {
                sb.append(", postParameters=");
                sb.append(Arrays.asList(((PostMethod) method).getParameters()));
            }
            LOG.info(sb.toString());

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

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new ExternalSystemException(
                    "The request for '" + requestUri +
                    "' failed with response code " + responseCode + ".");
        }
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

        PostMethod method = new PostMethod(this.dataServerUrlName +
                                           resourceField);

        DataRow dataRow = row.getDataRow();
        String fieldValue;
        for (DataField field : dataRow.getFields()) {
            if (! resourceFieldName.equals(field.getDisplayName())) {
                fieldValue = field.getCoreValue();
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    method.addParameter(field.getDisplayName(), fieldValue);
                }
            }
        }

        sendRequest(method);
    }

    private void logFailure(EventType eventType,
                            PluginDataRow row,
                            Throwable t) {
        LOG.error("Failed to process " + eventType +
                  " event for " + row, t);
    }

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize REST data manager plug-in.  ";
}