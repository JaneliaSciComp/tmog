/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.PluginConfiguration;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This plug-in writes the source (from) file name for the current
 * transmogrifier row to a file.
 *
 * @author Eric Trautman
 */
public class SourceNameWriterPlugin
        extends RowWriterPlugin {

    // TODO: consider using FileChannel to manage concurrent access
    //       to a single file instead of creating separate files for
    //       each session

    private String fileNamePrefix;

    @Override
    public void init(PluginConfiguration config)
            throws ExternalSystemException {
        super.init(config);

        // TODO: consider using UUID class to generate unique file name

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'-'HHmmss");
        String hostAddress = null;
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            hostAddress = localHost.getHostAddress();
        } catch (UnknownHostException e) {
            LOG.warn("failed to determine host address", e);
        }

        this.fileNamePrefix = "file-names-" + sdf.format(new Date()) + 
                              "-" + hostAddress + "-";
    }

    @Override
    protected String getRowRepresentation(PluginDataRow row) {
        String sourceName = null;
        if (row instanceof RenamePluginDataRow) {
            final File fromFile = ((RenamePluginDataRow) row).getFromFile();
            sourceName = fromFile.getName() + "\n";
        }
        return sourceName;
    }

    @Override
    protected File getFile(PluginDataRow row,
                           File baseDirectory) {
        final Thread currentThread = Thread.currentThread();
        final String fileName = fileNamePrefix + currentThread.getId() + ".txt";
        File fileDirectory = baseDirectory;
        if (fileDirectory == null) {
            final File targetFile = row.getTargetFile();
            fileDirectory = targetFile.getParentFile();
        }
        return new File(fileDirectory, fileName);
    }

    @Override
    protected String getInitFailureMessage() {
        return "Failed to initialize source name writer plug-in.  ";
    }

    private static final Logger LOG =
            Logger.getLogger(SourceNameWriterPlugin.class);
}