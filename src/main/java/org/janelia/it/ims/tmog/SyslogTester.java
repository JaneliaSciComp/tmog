package org.janelia.it.ims.tmog;

import java.util.Arrays;

import org.apache.logging.log4j.Logger;

public class SyslogTester {

    private static final Logger LOG = org.apache.logging.log4j.LogManager.getLogger(SyslogTester.class);

    public static void main(String[] args) {
        if (args.length == 0) {
            final int maxCharacters = 1980; // anything > 970 causes truncation
            final StringBuilder sb = new StringBuilder(maxCharacters);
            for (int i = 0; i < maxCharacters; i += 10) {
                sb.append(String.format("%03d-------", i));
            }
            LOG.info(sb.toString());
        } else {
            Arrays.stream(args).forEach(LOG::info);
        }
    }
}
