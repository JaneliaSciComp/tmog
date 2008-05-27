package org.janelia.it.ims.tmog.plugin;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.PluginConfiguration;

/**
 * This class sleeps for a configured number of seconds (or a default of
 * 10 seconds) each time a {@link EventType#END_FAIL} or
 * {@link EventType#END_SUCCESS} copy event is received.
 * It is intended to be used to simulate long running copies during testing.
 * To use the class, add the following to the transmogrifier_config.xml file:
 *
 * <br/>
 * <xmp>
 *
 *   <copyListener className="org.janelia.it.ims.tmog.plugin.SleepingCopyListener">
 *     <property name="seconds" value="30"/>
 *   </copyListener>
 *
 * </xmp>
 *
 * @author Eric Trautman
 */
public class SleepingRowListener implements RowListener {

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(SleepingRowListener.class);

    private int seconds;

    public SleepingRowListener() {
        this.seconds = 10;
    }

    public void init(PluginConfiguration config) throws ExternalSystemException {
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
            case END_FAIL:
                sleep();
                break;
            case END_SUCCESS:
                sleep();
                break;
            case START:
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
