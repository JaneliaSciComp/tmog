/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.utils.PathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class validates that a file can be moved using the current mount.
 * Validation is performed at initialization (not for each row),
 * so using a row validator here is a bit of a hack.
 *
 * @author Eric Trautman
 */
public class MoveValidator
        extends SimpleRowValidator {

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    @SuppressWarnings("UnusedDeclaration")
    public MoveValidator() {
    }

    /**
     * Verifies that the plugin is ready for use.
     *
     * @param  config  the plugin configuration.
     *
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config)
            throws ExternalSystemException {

        final File fromDirectory = getDirectory("fromDirectory", config);
        final File toDirectory = getDirectory("toDirectory", config);

        // test files are like: /groups/flylight/flylight/tmog/move-test/from/test-0.txt

        List<File> fromFileList = new ArrayList<File>();
        for (int i = 0; i < 10; i++) {
            final File fromFile = new File(fromDirectory, "test-" + i + ".txt");
            if (fromFile.exists()) {
                fromFileList.add(fromFile);
            }
        }

        boolean moveConsistentlyFailed = true;
        if (fromFileList.size() == 0) {
            moveConsistentlyFailed = false;
            LOG.warn("failed to find any move test files in " +
                     fromDirectory.getAbsolutePath());
        }

        for (File fromFile : fromFileList) {
            final File toFile = new File(toDirectory, fromFile.getName());
            if (fromFile.renameTo(toFile)) {
                if (toFile.renameTo(fromFile)) {
                    moveConsistentlyFailed = false;
                    LOG.info("successfully restored " +
                             toFile.getAbsolutePath() + " to " +
                             fromFile.getAbsolutePath() + " for test");
                    break;
                } else {
                    LOG.warn("failed to restore " +
                             toFile.getAbsolutePath() + " to " +
                             fromFile.getAbsolutePath() + " for test");
                }
            } else {
                LOG.warn("failed to move " +
                         fromFile.getAbsolutePath() + " to " +
                         toFile.getAbsolutePath() + " for test");
            }
        }

        if (moveConsistentlyFailed) {
            throw new ExternalSystemException(
                    VERIFICATION_FAILURE_MSG +
                    "The drive has been mapped by the user '" +
                    System.getProperty("user.name") +
                    "'.  Please confirm that this user has appropriate " +
                    "access to the share.");
        }
    }

    private File getDirectory(String parameterName,
                              PluginConfiguration config)
            throws ExternalSystemException {
        final String directoryName = config.getProperty(parameterName);
        if ((directoryName == null) || (directoryName.length() == 0)) {
            throw new ExternalSystemException(
                    "Move Validator plug-in failed initialization. " +
                    "Parameter '" + parameterName + "' must be specified.");
        }

        final File directory = new File(PathUtil.convertPath(directoryName));

        if (! directory.canWrite()) {
            throw new ExternalSystemException(
                    VERIFICATION_FAILURE_MSG + "The test '" + parameterName +
                    "' ( " + directory.getAbsolutePath() +
                    " ) is not writable.");
        }

        return directory;
    }

    /** Does nothing as this plug-in validates at start-up only. */
    public void validate(String sessionName,
                         PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
    }

    /**
     * The logger for this class.
     */
    private static final Logger LOG =
            Logger.getLogger(MoveValidator.class);

    private static final String VERIFICATION_FAILURE_MSG =
            "Failed to verify that moves are permitted for the current mapped drive.  ";

}