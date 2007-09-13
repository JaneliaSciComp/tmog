/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.apache.log4j.net;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This appender extends the SyslogAppender so that stack trace information
 * is included in a single syslog message insetad in multiple messages.
 * <p/>
 * Based on empirical evidence from tests at Janelia, messages are
 * truncated at 1824 characters (longer messages seem to get dropped).
 * A recommended maximum of 1024 bytes for syslog messages is documented in
 * <a href="ftp://ftp.rfc-editor.org/in-notes/rfc3164.txt">
 * ftp://ftp.rfc-editor.org/in-notes/rfc3164.txt</a>.
 * Other useful syslog information can be found at:
 * <a href="http://www.monitorware.com/Common/en/Articles/syslog-described.php">
 * http://www.monitorware.com/Common/en/Articles/syslog-described.php</a>
 *
 * @author Eric Trautman
 */
public class SingleLineStackTraceSyslogAppender extends SyslogAppender {

    /**
     * The recommended maximum number of bytes for syslog messages.
     * See section 6.1 (Packet Parameters) of
     * <a href="ftp://ftp.rfc-editor.org/in-notes/rfc3164.txt">
     * The BSD syslog Protocol</a> for details.
     */
    public static final int RECOMMENDED_MAX_MESSAGE_SIZE = 1024;

    /**
     * The apparent max message size for Janelia's syslog implementation.
     */
    public static final int MAGIC_MAX_MESSAGE_SIZE = 1824;

    public SingleLineStackTraceSyslogAppender() {
        super();
    }

    public SingleLineStackTraceSyslogAppender(Layout layout,
                                              int syslogFacility) {
        super(layout, syslogFacility);
    }

    public SingleLineStackTraceSyslogAppender(Layout layout,
                                              String syslogHost,
                                              int syslogFacility) {
        super(layout, syslogHost, syslogFacility);
    }


    public void append(LoggingEvent event) {
        if (!isAsSevereAsThreshold(event.getLevel()))
            return;

        // We must not attempt to append if sqw is null.
        if (sqw == null) {
            errorHandler.error(
                    "No syslog host is set for SyslogAppedender named \"" +
                    this.name + "\".");
            return;
        }

        StringBuilder sb = new StringBuilder(MAGIC_MAX_MESSAGE_SIZE);
        if (facilityPrinting) {
            sb.append(facilityStr);
        }
        sb.append(layout.format(event));
        String[] s = event.getThrowableStrRep();
        String stLine;
        if (s != null) {
            int len = s.length;
            if (len > 0) {
                sb.append("| ");
                sb.append(s[0]);
                for (int i = 1; i < len; i++) {
                    sb.append("\n| ");
                    stLine = s[i];
                    if (stLine.charAt(0) == '\t') {
                        stLine = stLine.substring(1);
                    }
                    sb.append(stLine);
                }
            }
        }

        sqw.setLevel(event.getLevel().getSyslogEquivalent());

        String msg;
        if (sb.length() > MAGIC_MAX_MESSAGE_SIZE) {
            msg = sb.substring(0, MAGIC_MAX_MESSAGE_SIZE);
        } else {
            msg = sb.toString();
        }
        sqw.write(msg);
    }
}
