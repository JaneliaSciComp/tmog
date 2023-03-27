/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.MDC;
import org.apache.logging.log4j.Logger;

/**
 * This class contains methods that support application logging.
 *
 * @author Eric Trautman
 */
public class LoggingUtils {

    /** The logger for this class. */
    private static final Logger LOG = org.apache.logging.log4j.LogManager.getLogger(LoggingUtils.class);

    public static void setLoggingContext() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            String hostname = addr.getHostName();
            MDC.put("hostname", hostname);
        } catch (UnknownHostException e) {
            LOG.warn("unable to determine local host name", e);
        }
    }


}
