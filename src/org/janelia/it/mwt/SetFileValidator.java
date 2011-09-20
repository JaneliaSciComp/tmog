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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private String timesFieldNamePattern = "Stimulus {0} Times";
    private String baseFileNamePattern =
            "(.+?)(_\\d+k)?\\.(png|blob|blobs|set|summary)";
    private int baseFileNameGroupIndex = 1;

    private MessageFormat timesFieldNameFormat;
    private Pattern compiledBaseFileNamePattern;

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

        final String configuredTimesFieldNamePattern =
                props.get("timesFieldNamePattern");
        if (configuredTimesFieldNamePattern != null) {
            if (! configuredTimesFieldNamePattern.contains("{0}")) {
                throw new ExternalSystemException(
                        INIT_FAIL_MSG +
                        "The 'timesFieldNameFormatString' property " +
                        "must contain '{0}' for the stimulus index.");

            }
            timesFieldNamePattern = configuredTimesFieldNamePattern;
        }

        timesFieldNameFormat = new MessageFormat(timesFieldNamePattern);

        try {
            timesFieldNameFormat.format(new Object[]{1});
        } catch (Exception e) {
            throw new ExternalSystemException(
                    INIT_FAIL_MSG +
                    "Please verify the 'timesFieldNameFormatString' property " +
                    "value '" + timesFieldNamePattern + "'.  " +
                    "The specific error is: " + e.getMessage(), e);
        }

        final String configuredBaseFileNamePattern =
                props.get("baseFileNamePattern");
        if (configuredBaseFileNamePattern != null) {
            baseFileNamePattern = configuredBaseFileNamePattern;
        }

        try {
            compiledBaseFileNamePattern = Pattern.compile(baseFileNamePattern);
        } catch (Exception e) {
            throw new ExternalSystemException(
                    INIT_FAIL_MSG +
                    "Please verify the 'baseFileNamePattern' property " +
                    "value '" + baseFileNamePattern + "'.  " +
                    "The specific error is: " + e.getMessage(), e);
        }

        final String configuredBaseFileNameGroupIndex =
                props.get("baseFileNameGroupIndex");
        if (configuredBaseFileNameGroupIndex != null) {
            try {
                baseFileNameGroupIndex =
                        Integer.parseInt(configuredBaseFileNameGroupIndex);
            } catch (Exception e) {
                throw new ExternalSystemException(
                        INIT_FAIL_MSG +
                        "Please verify the 'baseFileNameGroupIndex' property " +
                        "value '" + configuredBaseFileNameGroupIndex + "'.  " +
                        "The specific error is: " + e.getMessage(), e);
            }
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
            if (setFileName == null) {
                throw new ExternalDataException(
                        "Validation for " + fromFile.getAbsolutePath() +
                        " cannot be performed because the set file name " +
                        "cannot be derived.  The set file name pattern is '" +
                        baseFileNamePattern + "'.");
            }

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
        Matcher m = compiledBaseFileNamePattern.matcher(fromFileAbsolutePath);
        if (m.matches() && (m.groupCount() >= baseFileNameGroupIndex)) {
            setFileAbsolutePath = m.group(baseFileNameGroupIndex) + ".set";
        } else {
            LOG.warn("getSetFileAbsolutePath: " +
                     "failed to derive set file path for " +
                     fromFileAbsolutePath + ", baseFileNamePattern is '" +
                     baseFileNamePattern + "', baseFileNameGroupIndex is " +
                     baseFileNameGroupIndex);
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

    private static final String INIT_FAIL_MSG =
            "Failed to initialize SetFileValidator plug-in.  ";

}