/*
 * Copyright (c) 2018 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PropertyToken;
import org.janelia.it.ims.tmog.plugin.RelativePathUtil;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowListener;
import org.janelia.it.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This plug-in persists transmogrifier row data to an image database.
 *
 * @author Eric Trautman
 */
public class ImageDataPlugin implements RowListener {

    /** The writer used to persist image property data. */
    private ImagePropertyWriter propertyWriter;

    /**
     * List of property setter instances for the data fields
     * configured to be persisted by this plug-in.
     */
    private List<ImagePropertySetter> propertySetters;

    /**
     * Flag indicating whether existing data should be kept when
     * an image's relative path changes.
     */
    private boolean keepExistingData = false;

    /**
     * The number of parent directories to include in each image's
     * relative path.
     */
    private int relativePathDepth = 1;

    /**
     * The number of parent directories to include in each image's
     * previous relative path (used for identifying whether an
     * image already exists).
     */
    private int previousRelativePathDepth = 1;

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
        this.propertySetters = new ArrayList<>();
        Map<String, String> props = config.getProperties();
        String dbConfigurationKey = null;
        String xmlBaseDirectoryName = null;
        HostNameSetter hostNameSetter = new HostNameSetter(HostNameSetter.TYPE);
        String value;
        ImagePropertySetter propertySetter;
        for (String key : props.keySet()) {
            value = props.get(key);
            if ("db.config.key".equals(key)) {
                dbConfigurationKey = value;
            } else if ("xml.base.directory".equals(key)) {
                xmlBaseDirectoryName = value;
            } else if ("exclude.host.name".equals(key)) {
                hostNameSetter = null;
            } else if ("keep.existing.data".equals(key)) {
                keepExistingData = Boolean.parseBoolean(value);                
            } else if ("relativePathDepth".equals(key)) {
                relativePathDepth =
                        getIntValue("relativePathDepth", value);
            } else if ("previousRelativePathDepth".equals(key)) {
                previousRelativePathDepth =
                        getIntValue("previousRelativePathDepth", value);
            } else {
                try {
                    propertySetter = getPropertySetter(key, value, props);
                } catch (IllegalArgumentException e) {
                    throw new ExternalSystemException(
                            INIT_FAILURE_MSG + e.getMessage());
                }
                this.propertySetters.add(propertySetter);
            }
        }

        // include host name setter unless it has been explicitly excluded
        if (hostNameSetter != null) {
            this.propertySetters.add(hostNameSetter);
        }

        if (StringUtil.isDefined(dbConfigurationKey)) {

            if (StringUtil.isDefined(xmlBaseDirectoryName)) {
                throw new ExternalSystemException(
                        INIT_FAILURE_MSG +
                        "Please specify either a 'db.config.key' or " +
                        "an 'xml.base.directory' plug-in property, " +
                        "but not both for the same instance.");
            }

            try {
                propertyWriter = new SageImageDao(dbConfigurationKey);
                propertyWriter.checkAvailability();
            } catch (ExternalSystemException e) {
                throw new ExternalSystemException(
                        INIT_FAILURE_MSG +
                        e.getMessage(),
                        e);
            }
        } else if (StringUtil.isDefined(xmlBaseDirectoryName)) {

            try {
                propertyWriter =
                        new ImagePropertyFileWriter(xmlBaseDirectoryName);
                propertyWriter.checkAvailability();
            } catch (ExternalSystemException e) {
                throw new ExternalSystemException(
                        INIT_FAILURE_MSG +
                        e.getMessage(),
                        e);
            }

        } else {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "Please specify a value for the 'db.config.key' " +
                    "or 'xml.base.directory' plug-in property.");                   
        }

        if (this.propertySetters.size() < 1) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "Please configure at least one data field to " +
                    "be stored by defining the field's display name " +
                    "and type name as a property for this plug-in.");
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
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalSystemException {
        if (EventType.END_ROW_SUCCESS.equals(eventType)) {
            saveImageProperties(row);
        }
        return row;
    }

    /**
     * Processes completed copy successfully event.
     *
     * @param  row  the row information for the event.
     *
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    private void saveImageProperties(PluginDataRow row)
            throws ExternalSystemException {

        String relativePath = null;
        try {
            relativePath = RelativePathUtil.getRelativePath(
                    row.getTargetFile(), relativePathDepth);
            String previousPath = null;
            if (row instanceof RenamePluginDataRow) {
                final RenamePluginDataRow renameRow = (RenamePluginDataRow) row;                
                previousPath = RelativePathUtil.getRelativePath(
                        renameRow.getFromFile(),
                        previousRelativePathDepth);
            }

            Image image = new Image(row,
                                    propertySetters,
                                    relativePath,
                                    previousPath,
                                    (! keepExistingData));
            propertyWriter.saveProperties(image);
        } catch (Exception e) {
            throw new ExternalSystemException(
                    "Failed to save image properties for " +
                    relativePath + ".  Detailed data is: " + row, e);
        }
    }

    private static ImagePropertySetter getPropertySetter(String propertyType,
                                                         String fieldName,
                                                         Map<String, String> props)
            throws IllegalArgumentException {

        ImagePropertySetter propertySetter;
        if (CaptureDateSetter.TYPE.equals(propertyType)) {
            propertySetter = new CaptureDateSetter(fieldName);
        } else if (BaseUrlSetter.TYPE.equals(propertyType)) {
            propertySetter = new BaseUrlSetter(fieldName);
        } else if (BasePathSetter.TYPE.equals(propertyType)) {
            propertySetter = new BasePathSetter(fieldName);
        } else if (ImageSourceSetter.TYPE.equals(propertyType)) {
            propertySetter = new ImageSourceSetter(fieldName);
        } else if (FamilySetter.TYPE.equals(propertyType)) {
            propertySetter = new FamilySetter(fieldName);
        } else if (CreatedBySetter.TYPE.equals(propertyType)) {
            propertySetter = new CreatedBySetter(fieldName);
        } else if (DisplaySetter.TYPE.equals(propertyType)) {
            propertySetter = new DisplaySetter(fieldName);
        } else if (MicroscopeFilenameSetter.TYPE.equals(propertyType)) {
            if ((fieldName == null) || (fieldName.length() == 0)) {
                propertySetter = new MicroscopeFilenameSetter();
            } else {
                propertySetter = new SimpleSetter(propertyType, fieldName);
            }
        } else if (RelativePathSetter.TYPE.equals(propertyType)) {
            propertySetter = new RelativePathSetter(fieldName);
        } else if (FieldGroupSetter.isFieldGroupType(propertyType)) {
            propertySetter = new FieldGroupSetter(propertyType, fieldName);
        } else if (fieldName.contains(PropertyToken.TOKEN_ID)) {
            propertySetter = new CompositeSetter(propertyType,
                                                 fieldName,
                                                 props);
        } else {
            propertySetter = new SimpleSetter(propertyType, fieldName);
        }

        return propertySetter;
    }

    private int getIntValue(String propertyName,
                            String valueString)
            throws ExternalSystemException {
        int value;
        try {
            value = Integer.parseInt(valueString);
        } catch (NumberFormatException e) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "Please specify a valid '" +
                    propertyName + "' value.", e);
        }
        return value;
    }

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize Image Data plug-in.  ";
}