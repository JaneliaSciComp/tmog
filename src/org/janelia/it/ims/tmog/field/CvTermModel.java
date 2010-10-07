/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.janelia.it.annotation.model.cv.Cv;
import org.janelia.it.annotation.model.cv.CvTerm;
import org.janelia.it.annotation.model.cv.CvTermSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * This model supports selecting a controlled vocabulary term from a
 * predefined set of terms.  Terms are retrieved at start-up
 *
 * @author Eric Trautman
 */
public class CvTermModel
        extends ValidValueModel {

    private String serviceUrl;
    private boolean displayNamePrefixedForValues;

    public CvTermModel() {
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
     * Clears any existing values in this model and adds a new set of values
     * retrieved via http request.
     */
    public void retrieveAndSetValidValues() {

        if ((serviceUrl == null) || (serviceUrl.length() == 0)) {
            throw new IllegalArgumentException(
                    "Please specify a serviceUrl attribute " +
                    "in the configuration for the " +
                    getDisplayName() + " field.");
        }

        CvTermSet cvTermSet = retrieveTermSet();

        // if we successfully retrieved the term set,
        // clear any existing data and then add the new values
        clearValidValues();

        String displayName;
        String name;
        ValidValue validValue;
        for (CvTerm cvTerm : cvTermSet.getTerms()) {
            name = cvTerm.getName();
            displayName = cvTerm.getDisplayName();
            if ((displayName == null) || (displayName.length() == 0)) {
                displayName = name;
            } else if (displayNamePrefixedForValues) {
                displayName = name + ": " + displayName;
            }

            validValue = new ValidValue(displayName, name);
            addValidValue(validValue);
        }
    }

    private CvTermSet retrieveTermSet() {
        CvTermSet cvTermSet = null;
        InputStream responseStream = null;

        int responseCode;
        GetMethod method = new GetMethod(serviceUrl);
        try {
            HttpClient httpClient = new HttpClient();
            LOG.info("sending GET " + serviceUrl);
            responseCode = httpClient.executeMethod(method);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException(
                        "The request for '" + serviceUrl +
                        "' failed with response code " + responseCode + ".");
            }

            JAXBContext ctx = JAXBContext.newInstance(Cv.class);
            Unmarshaller unm = ctx.createUnmarshaller();
            responseStream = method.getResponseBodyAsStream();
            Cv cv = (Cv) unm.unmarshal(responseStream);
            cvTermSet = cv.getTerms();

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to submit request to '" + serviceUrl +
                    "'.  Please verify the configured url is accurate and " +
                    "that the corresponding service is available.", e);
        } catch (JAXBException e) {
            throw new IllegalStateException(
                    "Failed to parse response for '" + serviceUrl + "'", e);
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

        LOG.info("retrieved " + cvTermSet.size() +
                 " results for " + serviceUrl);

        return cvTermSet;
    }

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(CvTermModel.class);

}
