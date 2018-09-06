/*
 * Copyright (c) 2017 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.dataFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowUpdater;
import org.janelia.it.ims.tmog.plugin.RowValidator;
import org.janelia.it.ims.tmog.view.component.NarrowOptionPane;
import org.janelia.it.utils.StringUtil;

import javax.swing.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks for inconsistencies between the objective information embedded in the source file name and
 * the objective information recorded in the lsm file by the Zeiss software.
 * If an inconsistency is found, data set and tile information is used to derive the correct objective value.
 * If the derived value differs from the recorded value, the row is updated with the derived value.
 * In all other cases, the recorded value is left as is.
 *
 * @author Eric Trautman
 */
public class ObjectiveCorrectionPlugin
        implements RowUpdater, RowValidator {

    private static final Log LOG = LogFactory.getLog(ObjectiveCorrectionPlugin.class);

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize Objective Correction plug-in.  ";

    public static final String DEFAULT_DATA_SET_COLUMN_NAME = "Data Set";
    public static final String DEFAULT_OBJECTIVE_COLUMN_NAME = "Objective";
    public static final String DEFAULT_TILE_COLUMN_NAME = "Tile";

    public static final String DEFAULT_FILE_NAME_PATTERN = ".*_(\\d\\d[Xx])_.*";
    public static final String DEFAULT_RECORDED_VALUE_PATTERN = ".*(\\d\\dx).*";

    public static final String FIX_20X_OBJECTIVE_NAME = "tmog fix 20x objective";
    public static final String FIX_40X_OBJECTIVE_NAME = "tmog fix 40x objective";
    public static final String FIX_63X_OBJECTIVE_NAME = "tmog fix 63x objective";

    public static final String TILE_PROPERTY_MAPPING_PREFIX = "tile-";
    public static final String SHOW_CONFIRMATION_DIALOG = "show-confirmation-dialog";

    private String dataSetColumnName;
    private String objectiveColumnName;
    private String tileColumnName;

    private Pattern fileNamePattern;
    private Pattern recordedValuePattern;
    private Map<String, String> dataSetToObjectiveMap;
    private Map<String, String> tileToObjectiveMap;
    private boolean showFixConfirmationDialog;

    @Override
    public void init(PluginConfiguration config)
            throws ExternalSystemException {
        this.dataSetColumnName = DEFAULT_DATA_SET_COLUMN_NAME;
        this.objectiveColumnName = DEFAULT_OBJECTIVE_COLUMN_NAME;
        this.tileColumnName = DEFAULT_TILE_COLUMN_NAME;

        this.fileNamePattern = Pattern.compile(DEFAULT_FILE_NAME_PATTERN);
        this.recordedValuePattern = Pattern.compile(DEFAULT_RECORDED_VALUE_PATTERN);

        Map<String, String> dataSetMap = new HashMap<>();
        dataSetMap.put("heberleinlab_central_brain_npf_flpl_20x", FIX_20X_OBJECTIVE_NAME);

        dataSetMap.put("asoy_mb_lexa_gal4_40X_1024px", FIX_40X_OBJECTIVE_NAME);
        dataSetMap.put("asoy_mb_lexa_gal4_40X_512px", FIX_40X_OBJECTIVE_NAME);

        dataSetMap.put("nerna_optic_central_border", FIX_63X_OBJECTIVE_NAME);
        dataSetMap.put("nerna_optic_lobe_left", FIX_63X_OBJECTIVE_NAME);
        dataSetMap.put("nerna_optic_lobe_right", FIX_63X_OBJECTIVE_NAME);
        dataSetMap.put("nerna_optic_span", FIX_63X_OBJECTIVE_NAME);
        dataSetMap.put("nerna_other", FIX_63X_OBJECTIVE_NAME);
        dataSetMap.put("nerna_whole_brain", FIX_63X_OBJECTIVE_NAME);
        dataSetMap.put("wolfft_central_tile", FIX_63X_OBJECTIVE_NAME);

        // if tile group = Brain, Ventral Nerve Cord, or VNC-Verify-ver,
        // then objective = 20X
        // else objective = 63X or 40X

        Map<String, String> tileMap = new HashMap<>();
        tileMap.put("brain", FIX_20X_OBJECTIVE_NAME);
        tileMap.put("brain_with_lamina", FIX_20X_OBJECTIVE_NAME);
        tileMap.put("ventral_nerve_cord", FIX_20X_OBJECTIVE_NAME);
        tileMap.put("vnc_verify", FIX_20X_OBJECTIVE_NAME);

        final Map<String, String> props = config.getProperties();
        String value;
        String coreObjective;
        String tileName;
        for (String key : props.keySet()) {
            value = props.get(key);

            if (SHOW_CONFIRMATION_DIALOG.equals(key)) {

                showFixConfirmationDialog = Boolean.parseBoolean(value);

            } else {

                coreObjective = getCoreObjective("configured objective value", value, recordedValuePattern);
                if (coreObjective == null) {
                    throw new ExternalSystemException(INIT_FAILURE_MSG + "A core value cannot be parsed from the " +
                                                      key + " value '" + value + "'.");
                }

                if (key.startsWith(TILE_PROPERTY_MAPPING_PREFIX)) {
                    tileName = key.substring(TILE_PROPERTY_MAPPING_PREFIX.length());
                    tileMap.put(tileName, value);
                } else {
                    dataSetMap.put(key, value);
                }
            }
        }

        this.dataSetToObjectiveMap = dataSetMap;
        this.tileToObjectiveMap = tileMap;
    }

    @Override
    public PluginDataRow updateRow(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        row.applyPluginDataValue(objectiveColumnName,
                                 getObjectiveValue(row));
        return row;
    }

    @Override
    public void startSessionValidation(String sessionName,
                                       List<DataRow> allRows)
            throws ExternalSystemException {
        // nothing to do
    }

    @Override
    public void validate(String sessionName,
                         PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        final String dataSet = row.getCoreValue(dataSetColumnName);
        final String tile = row.getCoreValue(tileColumnName);
        final String recordedValue = row.getCoreValue(objectiveColumnName);

        if (! StringUtil.isDefined(dataSet)) {
            throw new ExternalDataException("To validate the " + objectiveColumnName + " value, a " +
                                            dataSetColumnName + " value must be specified.");
        }

        if (! StringUtil.isDefined(tile)) {
            throw new ExternalDataException("To validate the " + objectiveColumnName + " value, a " +
                                            tileColumnName + " value must be specified.");
        }

        if (! StringUtil.isDefined(recordedValue)) {
            throw new ExternalDataException("The " + objectiveColumnName + " value is missing.  " +
                                            "Please click the Load Mapped Data button to load the " +
                                            objectiveColumnName + " value.");
        }

        final String recordedCoreObjective =
                getCoreObjective("lsm recorded value", recordedValue, recordedValuePattern);

        if (! StringUtil.isDefined(recordedCoreObjective)) {
            throw new ExternalDataException("A core value (e.g. 20x or 63x) cannot be parsed from the " +
                                            objectiveColumnName + " value '" + recordedValue +
                                            "'.  The Objective Correction plug-in needs to be modified to handle " +
                                            "this new type of name.");
        }

        final String fixedValue = getFixedValue(dataSet, tile, recordedCoreObjective);
        final String fixedCoreObjective =
                getCoreObjective("fixed objective value", fixedValue, recordedValuePattern);

        if (! recordedCoreObjective.equalsIgnoreCase(fixedCoreObjective)) {
            final String fileName = row.getDataRow().getTarget().getName();
            final String msg = "The " + fileName + " file has a " + recordedCoreObjective + " " +
                               objectiveColumnName + " '" + recordedValue + "' but a " +
                               fixedCoreObjective + " value is expected for '" + tile +
                               "' tiles in the '" + dataSet + "' data set.\n\n" +
                               "Please confirm both your " + dataSetColumnName +" and " + tileColumnName +
                               " field entries are correct, " +
                               "then click the Load Mapped Data button to fix the value.";
            throw new ExternalDataException(msg);
        }
    }

    @Override
    public void stopSessionValidation(String sessionName) {
        // nothing to do
    }

    private String getObjectiveValue(PluginDataRow row) {

        final File scopeFile = row.getTargetFile();
        final String fileName = scopeFile.getName();
        final String recordedValue = row.getCoreValue(objectiveColumnName);

        String objectiveValue = recordedValue;

        final String fileNameCoreObjective =
                getCoreObjective("scope file name", fileName, fileNamePattern);
        final String recordedCoreObjective =
                getCoreObjective("lsm recorded value", recordedValue, recordedValuePattern);

        if ((fileNameCoreObjective != null) && (recordedCoreObjective != null)) {
            if (fileNameCoreObjective.equalsIgnoreCase(recordedCoreObjective)) {
                LOG.info("getObjectiveValue: core objective values match for " + fileName);
            } else {
                final String dataSet = row.getCoreValue(dataSetColumnName);
                final String tile = row.getCoreValue(tileColumnName);
                objectiveValue = fixInconsistentObjective(recordedValue,
                                                          recordedCoreObjective,
                                                          dataSet,
                                                          tile,
                                                          fileName);
            }
        }

        return objectiveValue;
    }

    private String fixInconsistentObjective(String recordedValue,
                                            String recordedCoreObjective,
                                            String dataSet,
                                            String tile,
                                            String fileName) {
        String fixedValue;
        if (StringUtil.isDefined(dataSet) && StringUtil.isDefined(tile)) {
            fixedValue = getFixedValue(dataSet, tile, recordedCoreObjective);
            final String fixedCoreObjective =
                    getCoreObjective("fixed objective value", fixedValue, recordedValuePattern);

            if (fixedCoreObjective.equalsIgnoreCase(recordedCoreObjective)) {

                LOG.info("fixInconsistentObjective: recorded core matches fixed core '" +
                         fixedCoreObjective + "', keeping recorded value '" + recordedValue + "' for " + fileName);
                fixedValue = recordedValue;

            } else {

                if (showFixConfirmationDialog) {

                    final String msg = "The " + fileName + " file has a " + recordedCoreObjective +
                                       " recorded objective value '" + recordedValue + "' but a " +
                                       fixedCoreObjective + " value is expected for '" + tile +
                                       "' tiles in the '" + dataSet + "' data set.\n\n" +
                                       "Do you want the value to be corrected?\n\n" +
                                       "NOTE: If you do not correct the " + objectiveColumnName +
                                       " value, you will need to change the " + dataSetColumnName +
                                       " and/or " + tileColumnName + " values for the file.";
                    final int response =
                            NarrowOptionPane.showConfirmDialog(null,
                                                               msg,
                                                               "Correct Invalid Objective?",
                                                               JOptionPane.YES_NO_OPTION);
                    if (response == JOptionPane.NO_OPTION) {
                        fixedValue = recordedValue;
                    }

                }

                if (fixedValue.equals(recordedValue)) {
                    LOG.info("fixInconsistentObjective: user decided to keep recorded value '" + recordedValue +
                             "' for " + fileName);
                } else {
                    LOG.info("fixInconsistentObjective: changing objective from '" +
                             recordedValue + "' to '" + fixedValue + "' for " + fileName);
                }

            }
        } else {
            LOG.info("fixInconsistentObjective: skipping fix, data set and/or tile not defined for " + fileName);
            fixedValue = recordedValue;
        }

        return fixedValue;
    }

    private String getFixedValue(String dataSet,
                                 String tile,
                                 String recordedCoreObjective) {
        String fixedValue = dataSetToObjectiveMap.get(dataSet);
        if (fixedValue == null) {
            fixedValue = tileToObjectiveMap.get(tile);
            if (fixedValue == null) {
                // if a fixed value was not explicitly mapped by data set or tile,
                // assume the fixed value should be ...
                if (recordedCoreObjective.equalsIgnoreCase("40x")) {
                    // 40x if the recorded core objective is 40x
                    fixedValue = FIX_40X_OBJECTIVE_NAME;
                } else {
                    // 63x in all other cases
                    fixedValue = FIX_63X_OBJECTIVE_NAME;
                }
            }
        }
        return fixedValue;
    }

    private String getCoreObjective(String context,
                                    String fullValue,
                                    Pattern corePattern) {
        String coreObjective = null;
        final Matcher m = corePattern.matcher(fullValue);
        if (m.matches() && (m.groupCount() == 1)) {
            coreObjective = m.group(1);
        } else {
            LOG.warn("getCoreObjective: failed to parse core from " + context + " '" + fullValue + "'");
        }
        return coreObjective;
    }

}