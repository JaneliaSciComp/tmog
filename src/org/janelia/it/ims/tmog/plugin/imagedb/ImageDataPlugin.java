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
import java.util.ArrayList;
import java.util.List;
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
     * List of property setter instances for the data fields
     * configured to be persisted by this plug-in.
     */
    List<ImagePropertySetter> propertySetters;

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
        this.propertySetters = new ArrayList<ImagePropertySetter>();
        Map<String, String> props = config.getProperties();
        String value;
        ImagePropertySetter propertySetter;
        for (String key : props.keySet()) {
            value = props.get(key);
            if ("db.config.key".equals(key)) {
                this.dbConfigurationKey = value;
            } else {
                try {
                    propertySetter = getPropertySetter(key, value);
                } catch (IllegalArgumentException e) {
                    throw new ExternalSystemException(
                            INIT_FAILURE_MSG + e.getMessage());
                }
                this.propertySetters.add(propertySetter);
            }
        }

        if ((this.dbConfigurationKey == null) ||
            (this.dbConfigurationKey.length() < 1)) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "Please specify a value for the db.config.key " +
                    "plug-in property.");
        }

        if (this.propertySetters.size() < 1) {
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
            image = new Image(row, propertySetters);
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

    private static ImagePropertySetter getPropertySetter(String propertyType,
                                                         String fieldName)
            throws IllegalArgumentException {

        ImagePropertySetter propertySetter;
        if (CaptureDateSetter.TYPE.equals(propertyType)) {
            propertySetter = new CaptureDateSetter(fieldName);
        } else if (FamilySetter.TYPE.equals(propertyType)) {
            propertySetter = new FamilySetter(fieldName);
        } else if (CreatedBySetter.TYPE.equals(propertyType)) {
            propertySetter = new CreatedBySetter();
        } else if (fieldName.contains(CompositeSetter.TOKEN_ID)) {
            propertySetter = new CompositeSetter(propertyType, fieldName);
        } else {
            propertySetter = new SimpleSetter(propertyType, fieldName);
        }

        return propertySetter;
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(ImageDataPlugin.class);

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize Image Data plug-in.  ";
}