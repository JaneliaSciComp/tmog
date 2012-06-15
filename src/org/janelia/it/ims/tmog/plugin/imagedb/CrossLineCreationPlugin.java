/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowListener;
import org.janelia.it.utils.StringUtil;

import java.util.Map;

/**
 * This plug-in adds cross lines to the Image database.
 *
 * @author Eric Trautman
 */
public class CrossLineCreationPlugin
        implements RowListener {

    /** The data access object for retrieving and updating image data. */
    private SageImageDao dao;

    /** The key used to locate jdbc connection configuration for the plug-in. */
    private String dbConfigurationKey;

    /** Sets the line name for the current data row. */
    private CompositeSetter lineNameSetter;

    /** Sets the line lab name for the current data row. */
    private CompositeSetter labNameSetter;

    /** Sets the first parent's line name for the current data row. */
    private CompositeSetter parentALineNameSetter;

    /** Sets the first parent's line lab name for the current data row. */
    private CompositeSetter parentALabNameSetter;

    /** Sets the second parent's line name for the current data row. */
    private CompositeSetter parentBLineNameSetter;

    /** Sets the second parent's line lab name for the current data row. */
    private CompositeSetter parentBLabNameSetter;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public CrossLineCreationPlugin() {
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
        Map<String, String> props = config.getProperties();
        this.dbConfigurationKey = props.remove("db.config.key");
        if ((this.dbConfigurationKey == null) ||
            (this.dbConfigurationKey.length() < 1)) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "Please specify a value for the db.config.key " +
                    "plug-in property.");
        }

        this.lineNameSetter = getCompositeSetter("line",
                                                 props);
        this.labNameSetter = getCompositeSetter("line-lab",
                                                props);
        this.parentALineNameSetter = getCompositeSetter("parent-a-line", 
                                                        props);
        this.parentALabNameSetter = getCompositeSetter("parent-a-line-lab",
                                                       props);
        this.parentBLineNameSetter = getCompositeSetter("parent-b-line",
                                                        props);
        this.parentBLabNameSetter = getCompositeSetter("parent-b-line-lab",
                                                       props);

        try {
            setDao();
            dao.checkAvailability();
        } catch (ExternalSystemException e) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Notifies this plug-in that an event has occurred.
     *
     * @param  eventType  type of event.
     * @param  row        details about the event.
     *
     * @return the field row for processing (with any updates from this plugin).
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        if (EventType.END_ROW_SUCCESS.equals(eventType)) {
            row = addLineIfNecessary(row);
        }
        return row;
    }

    /**
     * Adds a cross line for the current data row if it does not already exist.
     *
     * @param  row  the row information for the event.
     *
     * @return row (unchanged).
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    private PluginDataRow addLineIfNecessary(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        try {
            Line parentA = new Line(parentALineNameSetter.getValue(row),
                                    parentALabNameSetter.getValue(row));
            Line parentB = new Line(parentBLineNameSetter.getValue(row),
                                    parentBLabNameSetter.getValue(row));

            Line child = new Line(lineNameSetter.getValue(row),
                                  labNameSetter.getValue(row));
            child.defaultGenotypeToLineName();

            child.setParentA(parentA);
            child.setParentB(parentB);

            dao.addLine(child);

        } catch (ExternalSystemException e) {
            throw new ExternalSystemException(
                    "Failed to add cross line.  Detailed data is: " + row, e);
        }

        return row;
    }

    /**
     * Create a composite setter for the specified plug-in property.
     *
     * @param  key    name of the property.
     * @param  props  set of all properties.
     *
     * @return composite setter.
     *
     * @throws ExternalSystemException
     *   if any error occurs during creation.
     */
    private CompositeSetter getCompositeSetter(String key,
                                               Map<String, String> props)
            throws ExternalSystemException {
        CompositeSetter setter;
        final String value = props.get(key);
        if (StringUtil.isDefined(value)) {
            setter = new CompositeSetter(key, value, props);
        } else {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "The required '" + key +
                    "' plug-in property is missing.");
        }
        return setter;
    }

    /**
     * Create the dao for this manager if it does not already exist.
     *
     * @throws ExternalSystemException
     *   if any error occurs during creation.
     */
    private synchronized void setDao() throws ExternalSystemException {
        if (dao == null) {
            dao = new SageImageDao(dbConfigurationKey);
        }
    }

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize Cross Line Creation plug-in.  ";

}