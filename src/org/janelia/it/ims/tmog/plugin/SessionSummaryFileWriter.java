/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.utils.PathUtil;

import java.io.File;
import java.io.FileWriter;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class handles session events "published" by the transmogrifier tool.
 * It writes the summary message from the session into a file
 * so that the summary can be referenced later.
 *
 * @author Eric Trautman
 */
public class SessionSummaryFileWriter implements SessionListener {

    /**
     * The logger for this class.
     */
    private static final Logger LOG =
            Logger.getLogger(SessionSummaryFileWriter.class);

    /**
     * The configured directory for all summary files.
     */
    private File directory;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public SessionSummaryFileWriter() {
    }

    /**
     * @return the session summary file's parent directory.
     */
    public File getDirectory() {
        return directory;
    }

    /**
     * Verifies that the plugin is ready for use by checking external
     * dependencies.
     *
     * @param config the plugin configuration.
     * @throws ExternalSystemException if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config) throws ExternalSystemException {
        try {
            String directoryName = config.getProperty("directory");
            directoryName = PathUtil.convertPath(directoryName);
            directory = new File(directoryName);
            if (directory.exists()) {
                if (directory.isDirectory()) {
                    if (!directory.canWrite()) {
                        throw new ExternalSystemException(
                                "Unable to write to session listener directory: " +
                                directory.getAbsolutePath());
                    }
                } else {
                    throw new ExternalSystemException(
                            "Configured directory (" +
                            directory.getAbsolutePath() +
                            ") for session listener is not a directory.");
                }
            } else {
                throw new ExternalSystemException(
                        "Unable to find session listener directory: " +
                        directory.getAbsolutePath());
            }
        } catch (Throwable t) {
            throw new ExternalSystemException(
                    "Failed to initialize file session plugin.  " +
                    t.getMessage(),
                    t);
        }
    }

    /**
     * Processes the specified session event.
     *
     * @param eventType type of session event.
     * @param message   details about the event.
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public void processEvent(SessionListener.EventType eventType,
                             String message)
            throws ExternalDataException, ExternalSystemException {
        switch (eventType) {
            case END:
                File sessionFile = getSessionFile();
                FileWriter fileWriter = null;
                try {
                    fileWriter = new FileWriter(sessionFile);
                    fileWriter.write(message);
                    closeWriter(fileWriter, sessionFile);
                } catch (Throwable t) {
                    closeWriter(fileWriter, sessionFile);
                    throw new ExternalSystemException(
                            "Failed to write session log file: " +
                            sessionFile.getAbsolutePath(), t);
                }
                break;
        }
    }

    /**
     * Utility to create a reasonably unique filename for the summary.
     *
     * @return a file for the transmogrifier session summary.
     */
    private File getSessionFile() {
        StringBuilder sb = new StringBuilder();
        sb.append(SDF.format(new Date()));

        // try a little harder to make sure summary file names are unique
        SecureRandom sr = new SecureRandom();
        byte[] randomByte = new byte[1];
        sr.nextBytes(randomByte);
        int randomNumber = Math.abs((int) randomByte[0]);
        if (randomNumber < 10) {
            sb.append("00");
        } else if (randomNumber < 100) {
            sb.append("0");
        }
        sb.append(randomNumber);

        sb.append("-session-summary.log");

        File parentDirectory = getDirectory();
        return new File(parentDirectory, sb.toString());
    }

    /**
     * Utility to close the rename session summary file writer.
     *
     * @param fileWriter  the writer used to write to the file.
     * @param sessionFile the file being written.
     */
    private void closeWriter(FileWriter fileWriter,
                             File sessionFile) {
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (Throwable t) {
                LOG.warn("failed to close session log file: " +
                         sessionFile.getAbsolutePath(), t);
            }
        }
    }

    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("yyyyMMdd'-'HHmmss'-'SSS");
}
