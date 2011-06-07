/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.PluginConfiguration;

/**
 * This class sleeps for a configured number of seconds (or a default of
 * 10 seconds) each time a {@link EventType#END_ROW_FAIL} or
 * {@link EventType#END_ROW_SUCCESS} copy event is received.
 * It was originally intended to be used to simulate long running copies
 * during testing, but could be used for other purposes.
 * To use the class, add the following to the transmogrifier_config.xml file:
 *
 * <br/>
 * <xmp>
 *
 *   <rowListener className="org.janelia.it.ims.tmog.plugin.SleepingRowListener">
 *     <property name="seconds" value="30"/>
 *   </rowListener>
 *
 * </xmp>
 *
 * @author Eric Trautman
 */
public class SleepingRowListener implements RowListener {

    /** The logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(SleepingRowListener.class);

    private int seconds;

    public SleepingRowListener() {
        this.seconds = 10;
    }

    public void init(PluginConfiguration config)
            throws ExternalSystemException {
        String secondsStr = config.getProperty("seconds");
        if (secondsStr != null) {
            try {
                seconds = Integer.valueOf(secondsStr);
            } catch (NumberFormatException e) {
                throw new ExternalSystemException(
                        "Invalid SleepingRowListener seconds property value: '" +
                        secondsStr + "'.  Value must be a valid number.");
            }
            if (seconds < 0) {
                throw new ExternalSystemException(
                        "Invalid SleepingRowListener seconds property value: " +
                        seconds +
                        ".  Value must be greater than or equal to 0.");
            }
        }
    }

    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        switch (eventType) {
            case END_ROW_FAIL:
                sleep();
                break;
            case END_ROW_SUCCESS:
                sleep();
                break;
            case START_ROW:
                break;
        }
        return row;
    }

    private void sleep() {
        try {
            LOG.info("sleeping for " + seconds + " seconds ...");
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            LOG.error(e);
        }
    }
}
