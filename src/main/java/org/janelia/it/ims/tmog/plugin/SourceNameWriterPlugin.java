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
import java.util.LinkedList;

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

    /**
     * Indicates how many parent directories should be included in
     * the relative paths written for each file.
     */
    private int pathDepth = 0;

    private String fileNamePrefix;

    @Override
    public void init(PluginConfiguration config)
            throws ExternalSystemException {
        super.init(config);

        String pathDepthString = config.getProperty("pathDepth");
        if (pathDepthString != null) {
            boolean isPathDepthInvalid;
            try {
                this.pathDepth = Integer.parseInt(pathDepthString);
                isPathDepthInvalid = (this.pathDepth < 0);
            } catch (NumberFormatException e) {
                isPathDepthInvalid = true;
            }

            if (isPathDepthInvalid) {
                throw new ExternalSystemException(
                        getInitFailureMessage() +
                        "The pathDepth property '" + pathDepthString +
                        "' must be a number greater than or equal to zero.");
            }
        }

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

            LinkedList<String> fileNames = new LinkedList<String>();
            File parent = fromFile.getParentFile();
            for (int i = 0; i < pathDepth; i++) {
                if (parent == null) {
                    break;
                } else {
                    fileNames.push(parent.getName());
                }
                parent = parent.getParentFile();
            }

            StringBuilder sb = new StringBuilder(128);
            for (String name : fileNames) {
                sb.append(name);
                sb.append('/');
            }
            sb.append(fromFile.getName());
            sb.append('\n');

            sourceName = sb.toString();
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