package org.janelia.it.ims.imagerenamer.plugin;

import org.apache.log4j.Logger;
import org.janelia.it.ims.imagerenamer.config.PluginConfiguration;

/**
 * This class sleeps for a configured number of seconds (or a default of
 * 10 seconds) each time a {@link EventType#END_FAIL} or
 * {@link EventType#END_SUCCESS} copy event is received.
 * It is intended to be used to simulate long running copies during testing.
 * To use the class, add the following to the rename_config.xml file:
 *
 * <br/>
 * <xmp>
 *
 *   <copyListener className="org.janelia.it.ims.imagerenamer.plugin.SleepingCopyListener">
 *     <property name="seconds" value="30"/>
 *   </copyListener>
 *
 * </xmp>
 *
 * @author Eric Trautman
 */
public class SleepingCopyListener implements CopyListener {

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(SleepingCopyListener.class);

    private int seconds;

    public SleepingCopyListener() {
        this.seconds = 10;
    }

    public void init(PluginConfiguration config) throws ExternalSystemException {
        String secondsStr = config.getProperty("seconds");
        if (secondsStr != null) {
            try {
                seconds = Integer.valueOf(secondsStr);
            } catch (NumberFormatException e) {
                throw new ExternalSystemException(
                        "Invalid SleepingCopyListener seconds property value: '" +
                        secondsStr + "'.  Value must be a valid number.");
            }
            if (seconds < 0) {
                throw new ExternalSystemException(
                        "Invalid SleepingCopyListener seconds property value: " +
                        seconds +
                        ".  Value must be greater than or equal to 0.");
            }
        }
    }

    public RenamePluginDataRow processEvent(EventType eventType,
                                       RenamePluginDataRow row)
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
