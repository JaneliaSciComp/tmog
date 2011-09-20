/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.mwt;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowValidator;

import java.io.File;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class validates manually entered protocol time specifications
 * match time data recorded in the corresponding multi-worm tracker
 * .set XML file.
 *
 * The multi-worm tracker .set XML files are expected to have the same
 * path and core name as the files being processed (just with a .set suffix).
 *
 * @author Eric Trautman
 */
public class SetFileValidator
        implements RowValidator {

    private MessageFormat timesFieldNameFormat =
            new MessageFormat("Stimulus {0} Times");

    // TODO: may need to manage cache cleanup if memory becomes an issue

    /** Cache of parsed set files. */
    private Map<String, SetFile> parsedSetFiles;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public SetFileValidator() {
        parsedSetFiles = new ConcurrentHashMap<String, SetFile>();
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
        Map<String, String> props = config.getProperties();
        String timesFieldNameFormatString =
                props.get("timesFieldNameFormatString");
        if (timesFieldNameFormatString != null) {
            if (! timesFieldNameFormatString.contains("{0}")) {
                throw new ExternalSystemException(
                        "Failed to initialize SetFileValidator plug-in.  " +
                        "The 'timesFieldNameFormatString' property " +
                        "must contain '{0}' for the stimulus index.");

            }
            timesFieldNameFormat =
                    new MessageFormat(timesFieldNameFormatString);
        }

        try {
            timesFieldNameFormat.format(new Object[]{1});
        } catch (Exception e) {
            throw new ExternalSystemException(
                    "Failed to initialize SetFileValidator plug-in.  " +
                    "Please verify the 'timesFieldNameFormatString' property " +
                    "value '" + timesFieldNameFormat.toPattern() + "'.  " +
                    "The specific error is: " + e.getMessage(), e);
        }
    }

    /**
     * Validates derived value(s) for the current row.
     *
     * @param  row  the user supplied meta-data to be validated.
     *
     * @throws ExternalDataException
     *   if the data is not valid.
     *
     * @throws ExternalSystemException
     *   if any error occurs while validating the data.
     */
    public void validate(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        if (row instanceof RenamePluginDataRow) {
            final File fromFile = ((RenamePluginDataRow) row).getFromFile();
            final String setFileName = getSetFileAbsolutePath(fromFile);
            SetFile parsedSetFile = parsedSetFiles.get(setFileName);
            if (parsedSetFile == null) {
                File setFile = new File(setFileName);
                if ((! setFile.exists()) || (! setFile.canRead())) {
                    throw new ExternalDataException(
                            "Validation for " + fromFile.getAbsolutePath() +
                            " cannot be performed because " + setFileName +
                            " does not exist or is not readable.");
                }
                parsedSetFile = new SetFile();
                try {
                    parsedSetFile.parse(setFile);
                } catch (IllegalArgumentException e) {
                    throw new ExternalDataException(
                            "Validation for " + fromFile.getAbsolutePath() +
                            " failed because " + setFileName +
                            " could not be parsed.  " +
                            "The specific error is: " + e.getMessage(), e);
                }

                addParsedSetFileToCache(setFileName, parsedSetFile);

                LOG.info("validate: parsed " + setFileName);
            }

            validateStimulusTimes(row, fromFile, parsedSetFile);
        }

    }

    private String getSetFileAbsolutePath(File fromFile) {
        String setFileAbsolutePath = null;
        String fromFileAbsolutePath = fromFile.getAbsolutePath();
        int lastDot = fromFileAbsolutePath.lastIndexOf('.') + 1;
        if (lastDot > 0) {
            setFileAbsolutePath = fromFileAbsolutePath.substring(0, lastDot) +
                                  "set";
        }
        return setFileAbsolutePath;
    }

    private synchronized void addParsedSetFileToCache(String setFileName,
                                                      SetFile parsedSetFile) {
        parsedSetFiles.put(setFileName, parsedSetFile);
    }

    private void validateStimulusTimes(PluginDataRow row,
                                       File fromFile,
                                       SetFile parsedSetFile)
            throws ExternalDataException {

        int stimulusIndex = 1;
        String timesFieldName;
        String timesFieldValue;
        String expectedTimeSpec;
        for (StimulusTimes times : parsedSetFile.getStimulusTimesList()) {
            expectedTimeSpec = times.getSpecification();
            timesFieldName =
                    timesFieldNameFormat.format(new Object[]{stimulusIndex});
            timesFieldValue = row.getCoreValue(timesFieldName);
            if (timesFieldValue == null) {
                throw new ExternalDataException(
                        "The '" + timesFieldName + "' value is missing for " +
                        fromFile.getAbsolutePath() +
                        ".  The expected value is '" + expectedTimeSpec + "'.");
            }
            if (! timesFieldValue.equals(expectedTimeSpec)) {
                throw new ExternalDataException(
                        "The '" + timesFieldName + "' value for " +
                        fromFile.getAbsolutePath() +
                        " does not match the expected value of '" +
                        expectedTimeSpec + "'.");
            }
            stimulusIndex++;
        }

    }

    private static final Logger LOG = Logger.getLogger(SetFileValidator.class);
}