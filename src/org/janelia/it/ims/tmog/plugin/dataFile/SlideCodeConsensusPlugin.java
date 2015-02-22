/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.dataFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowValidator;

import java.util.List;
import java.util.Map;

/**
 * This plugin retrieves slide code consensus values from an image data web service and utilizes them
 * to update existing rows and validate consistency.  The plugin also ensures that data entered for
 * new slide codes is consistent within the current session.
 *
 * @author Eric Trautman
 */
public class SlideCodeConsensusPlugin
        extends DataResourcePlugin
        implements RowValidator {

    @Override
    public String getInitFailureMsg() {
        return "Failed to initialize Slide Code Consensus plug-in.  ";
    }

    @Override
    public void startSessionValidation(String sessionName,
                                       List<DataRow> allRows)
            throws ExternalSystemException {
        // nothing to do
    }

    @Override
    public void validate(String sessionName,
                         PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        final Item item = getMappedItemForRow(row);
        final Map<String, String> rowFieldNameToXPathMap = getRowFieldNameToXPathMap();
        final String slideCode = row.getCoreValue("Slide Code");

        String rowValue;
        String consensusValue;

        if (item.size() == 0) {

            // nothing was returned from web service, so treat this row as the definitive source
            for (String fieldName : rowFieldNameToXPathMap.keySet()) {
                rowValue = row.getCoreValue(fieldName);
                item.addProperty(new Property(fieldName, rowValue));
            }

            // mark consensus as being derived from the current session
            item.addProperty(new Property(DERIVED_FROM_CURRENT_SESSION_KEY, "true"));

        } else {

            for (String fieldName : rowFieldNameToXPathMap.keySet()) {

                consensusValue = item.getPropertyValue(fieldName);
                rowValue = row.getCoreValue(fieldName);

                if (((consensusValue == null) && (rowValue != null) && (rowValue.length() != 0)) ||
                    ((consensusValue != null) && (! consensusValue.equals(rowValue)))) {

                    // remove item from cache in case source value is the problem
                    removeItem(getUrlForRow(row));

                    String message =
                            "The " + fieldName + " value '" + rowValue +
                            "' differs from the " + slideCode + " slide code consensus value '" + consensusValue +
                            "' ";

                    if (item.hasPropertyValue(DERIVED_FROM_CURRENT_SESSION_KEY)) {
                        message += "derived from the current session.";
                    } else {
                        message += "stored from a prior session.";
                    }

                    LOG.warn(message);

                    throw new ExternalDataException(message);
                }

            }

        }

    }

    @Override
    public void stopSessionValidation(String sessionName) {
        // nothing to do
    }

    private static final Log LOG = LogFactory.getLog(SlideCodeConsensusPlugin.class);

    private static final String DERIVED_FROM_CURRENT_SESSION_KEY = "DERIVED_FROM_CURRENT_SESSION";
}