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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This plug-in transfers and renames a "companion" file that is related to
 * the primary row file being renamed.  The relationship is derived from a
 * common pattern in the names of both files.  The plug-in was originally
 * developed to transfer/rename Zeiss log files as companions to
 * source lsm files being renamed.
 *
 * @author Eric Trautman
 */
public class CompanionFileRowListener
        implements RowListener {

    /** Name of the property that identifies the common name pattern. */
    public static final String COMMON_NAME_PATTERN_PROPERTY =
            "commonNamePattern";

    /** Name of the property that identifies the suffix for companion files. */
    public static final String COMPANION_SUFFIX_PROPERTY =
            "companionSuffix";

    /**
     * Name of the property that identifies whether the original
     * companion file should be deleted after being renamed.
     */
    public static final String DELETE_AFTER_RENAME_PROPERTY =
            "deleteAfterCopy";

    /**
     * Name of the property that identifies a name or path
     * for testing the group pattern.
     */
    public static final String TEST_PROPERTY = "testName";

    /** Pattern used derive common name for related files. */
    private Pattern commonNamePattern;

    /** Suffix of companion file. */
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

        final String patternString =
                helper.getRequiredProperty(COMMON_NAME_PATTERN_PROPERTY);
        try {
            this.commonNamePattern = Pattern.compile(patternString);
        } catch (Exception e) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "The " + COMMON_NAME_PATTERN_PROPERTY + " value '" +
                    patternString + "' could not be parsed.  " + e.getMessage(),
                    e);
        }

        this.companionSuffix =
                helper.getRequiredProperty(COMPANION_SUFFIX_PROPERTY);

        this.isOriginalFileDeletedAfterCopy = Boolean.parseBoolean(
                helper.getRequiredProperty(DELETE_AFTER_RENAME_PROPERTY));

        final String testName = helper.getRequiredProperty(TEST_PROPERTY);
        Matcher m = this.commonNamePattern.matcher(testName);
        if (! m.matches()) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "The " + TEST_PROPERTY + " value '" + testName +
                    "' does not match the " + COMMON_NAME_PATTERN_PROPERTY +
                    " '" + patternString + "'.");
        }

        if (m.groupCount() == 0) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "The " +
                    COMMON_NAME_PATTERN_PROPERTY + " '" + patternString +
                    "' must contain parentheses to identify " +
                    "the common portion of each file name.");
        }

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
        Matcher m = commonNamePattern.matcher(file.getName());
        if (m.matches() && (m.groupCount() > 0)) {
            final String companionFileName = m.group(1) + companionSuffix;
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
