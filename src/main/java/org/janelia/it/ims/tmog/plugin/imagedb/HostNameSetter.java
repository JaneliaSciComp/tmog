/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class sets the hostname property for an image.
 *
 * @author Eric Trautman
 */
public class HostNameSetter
        extends SimpleSetter {

    /**
     * The type value for hostname data stored in the image_property table.
     */
    public static final String TYPE = "hostname";
    
    /**
     * Value constructor.
     *
     * @param  fieldName  the display name of the data field that contains
     *                    created by information.
     */
    public HostNameSetter(String fieldName) {
        super(TYPE, fieldName);
    }

    /**
     * Adds a created by property type and value to the specified image.
     *
     * @param  row    current row being processed.
     * @param  image  image to be updated.
     */
    public void setProperty(PluginDataRow row,
                            Image image) {
        String value = row.getCoreValue(TYPE);
        if (value == null) {
            value = HOSTNAME;
        }
        if (value != null) {
            image.addProperty(TYPE, value);
        }
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(HostNameSetter.class);

    private static final String HOSTNAME;
    static {
        String hostName = null;
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            hostName = localHost.getHostName();
        } catch (UnknownHostException e) {
            LOG.error("failed to determine hostname", e);
        }
        HOSTNAME = hostName;
    }
}