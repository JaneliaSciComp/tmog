/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.utils.filexfer.FileCopyFailedException;
import org.janelia.it.utils.filexfer.SafeFileTransfer;

import java.io.File;

/**
 * This plug-in transfers and renames a "companion" file that is related to
 * the primary row file being renamed.  The relationship is derived from
 * both files sharing the same basic name with different extensions/suffixes.
 * The plug-in was originally developed to transfer/rename Zeiss log files
 * as companions to source lsm files being renamed.
 *
 * @author Eric Trautman
 */
public class CompanionFileRowListener
        implements RowListener {

    /** Name of the property that identifies the source file suffix. */
    public static final String SOURCE_SUFFIX_PROPERTY =
            "sourceSuffix";

    /** Name of the property that identifies the companion file suffix. */
    public static final String COMPANION_SUFFIX_PROPERTY =
            "companionSuffix";

    /**
     * Name of the property that identifies whether the original
     * companion file should be deleted after being renamed.
     */
    public static final String DELETE_AFTER_RENAME_PROPERTY =
            "deleteAfterCopy";

    /** Suffix for all source files. */
    private String sourceSuffix;

    /** Suffix for all companion files. */
    private String companionSuffix;

    /**
     * Indicates if the original companion file should be removed
     * after transfer.
     */
    private boolean isOriginalFileDeletedAfterCopy;

    /**
     * Initializes the plugin and verifies that it is ready for use.
     *
     * @param config the plugin configuration.
     * @throws ExternalSystemException if the plugin can not be initialized.
     */
    @Override
    public void init(PluginConfiguration config)
            throws ExternalSystemException {

        final PluginPropertyHelper helper =
                new PluginPropertyHelper(config,
                                         INIT_FAILURE_MSG);
        this.sourceSuffix =
                helper.getRequiredProperty(SOURCE_SUFFIX_PROPERTY);
        this.companionSuffix =
                helper.getRequiredProperty(COMPANION_SUFFIX_PROPERTY);

        this.isOriginalFileDeletedAfterCopy = Boolean.parseBoolean(
                helper.getRequiredProperty(DELETE_AFTER_RENAME_PROPERTY));
    }

    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        if ((eventType == EventType.END_ROW_SUCCESS) &&
            (row instanceof RenamePluginDataRow)) {
            transferCompanionFile((RenamePluginDataRow) row);
        }
        return row;
    }

    private File getCompanionFile(File file) {
        File companionFile = null;
        final String fileName = file.getName();
        if (fileName.endsWith(sourceSuffix)) {
            final int stop = fileName.length() - sourceSuffix.length();
            final String companionFileName = fileName.substring(0, stop) +
                                             companionSuffix;
            companionFile = new File(file.getParentFile(),
                                     companionFileName);
        }
        return companionFile;
    }

    private void transferCompanionFile(RenamePluginDataRow row)
            throws ExternalSystemException {

        final File fromCompanionFile = getCompanionFile(row.getFromFile());
        if ((fromCompanionFile != null) && fromCompanionFile.exists()) {

            final File renamedCompanionFile =
                    getCompanionFile(row.getRenamedFile());
            if (renamedCompanionFile == null) {
                throw new ExternalSystemException(
                        "Failed to derive companion file target for " +
                        fromCompanionFile.getAbsolutePath() + '.');
            }

            try {
                SafeFileTransfer.copy(fromCompanionFile,
                                      renamedCompanionFile,
                                      false);
            } catch (FileCopyFailedException e) {
                throw new ExternalSystemException(
                        "Failed to rename companion file " +
                        fromCompanionFile.getAbsolutePath() + " to " +
                        renamedCompanionFile.getAbsolutePath() + '.',
                        e);
            }

            if (isOriginalFileDeletedAfterCopy) {

                boolean isDeleteSuccessful = false;
                Exception deleteException = null;

                try {
                    isDeleteSuccessful = fromCompanionFile.delete();
                } catch (Exception e) {
                    deleteException = e;
                }

                if (! isDeleteSuccessful) {
                    LOG.warn("failed to remove " +
                             fromCompanionFile.getAbsolutePath() +
                             " after rename",
                             deleteException);
                }
            }

        }
    }

    private static final Logger LOG =
            Logger.getLogger(CompanionFileRowListener.class);

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize the Companion File Row Listener Plugin.  ";
}
