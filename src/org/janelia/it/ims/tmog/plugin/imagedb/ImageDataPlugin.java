/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PluginUtil;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowListener;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This plug-in persists transmogrifier row data to an image database.
 *
 * @author Eric Trautman
 */
public class ImageDataPlugin implements RowListener {

    /** The data access object for retrieving and updating image data. */
    private ImageDao dao;

    private String dbConfigurationKey;

    /**
     * Maps display names to property types for the set of data fields
     * configured to be persisted by this plug-in.
     */
    private Map<String, String> displayNameToPropertyTypeMap;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public ImageDataPlugin() {
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
    public void init(PluginConfiguration config) throws ExternalSystemException {
        this.displayNameToPropertyTypeMap = new HashMap<String, String>();
        Map<String, String> props = config.getProperties();
        String value;
        for (String key : props.keySet()) {
            value = props.get(key);
            if ("db.config.key".equals(key)) {
                this.dbConfigurationKey = value;
            } else {
                this.displayNameToPropertyTypeMap.put(key, value);
            }
        }

        if ((this.dbConfigurationKey == null) ||
            (this.dbConfigurationKey.length() < 1)) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "Please specify a value for the db.config.key " +
                    "plug-in property.");
        }

        if (this.displayNameToPropertyTypeMap.size() < 1) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "Please configure at least one data field to " +
                    "be stored by defining the field's display name " +
                    "and type name as a property for this plug-in.");
        }

        try {
            setDao();
            dao.checkConnection();
        } catch (ExternalSystemException e) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    e.getMessage(),
                    e);
        }        
    }

    /**
     * Processes the specified copy event.
     *
     * @param  eventType  type of copy event.
     * @param  row        details about the event.
     *
     * @return the rename field row for processing (with any
     *         updates from this plugin).
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public RenamePluginDataRow processEvent(EventType eventType,
                                            PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        RenamePluginDataRow dataRow = PluginUtil.castRenameRow(row, this);
        if (EventType.END_SUCCESS.equals(eventType)) {
            completedSuccessfulCopy(dataRow);
        }
        return dataRow;
    }

    /**
     * Processes completed copy successfully event.
     *
     * @param  row  the row information for the event.
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    private void completedSuccessfulCopy(RenamePluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        Image image;
        String fileName = null;
        File renamedFile = row.getRenamedFile();
        if (renamedFile != null) {
            fileName = renamedFile.getAbsolutePath();
        }
        try {
            image = new Image(row, displayNameToPropertyTypeMap);
            image = dao.addImage(image);
            if (LOG.isInfoEnabled()) {
                LOG.info("successfully persisted image metadata (" + image +
                         ") for " + fileName);
            }
        } catch (Exception e) {
            throw new ExternalSystemException(
                    "Failed to save image data for " + fileName +
                    ".  Detailed data is: " + row, e);
        }
    }

    /**
     * Create the dao for this manager if it does not already exist.
     *
     * @throws ExternalSystemException if any error occurs during creation.
     */
    private synchronized void setDao() throws ExternalSystemException {
        if (dao == null) {
            dao = new ImageDao(dbConfigurationKey);
        }
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(ImageDataPlugin.class);

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize Image Data plug-in.  ";
}