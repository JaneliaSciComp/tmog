/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.dataFile;

import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.filefilter.NumberComparator;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowUpdater;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Derives data set values using annotator, slide location and tile.
 * Values are derived based upon the number of images for each slide,
 * meaning that the session must contain all images for a particular slide.
 *
 * @author Eric Trautman
 */
public class TileToDataSetPlugin
        implements RowUpdater {

    public static final String ANNOTATOR_FIELD_NAME = "annotatorFieldName";
    public static final String DATA_SET_FIELD_NAME = "dataSetFieldName";
    public static final String SLIDE_CODE_FIELD_NAME = "slideCodeFieldName";
    public static final String TILE_FIELD_NAME = "tileFieldName";

    public static final String SORT_PATTERN = "sortPattern";

    public static final String DEFAULT_ANNOTATOR_COLUMN_NAME = "Annotator";
    public static final String DEFAULT_DATA_SET_COLUMN_NAME = "Data Set";
    public static final String DEFAULT_SLIDE_CODE_COLUMN_NAME = "Slide Code";
    public static final String DEFAULT_TILE_COLUMN_NAME = "Tile";

    private String annotatorColumnName;
    private String dataSetColumnName;
    private String slideCodeColumnName;
    private String tileColumnName;
    private Map<String, Map<String, List<String>>>
            annotatorTileNamesToDataSetListMap;

    private int annotatorColumn;
    private int slideCodeColumn;
    private int tileColumn;
    private boolean isColumnMappingComplete;

    private NumberComparator targetComparator;

    private Map<File, String> targetFileToDataSetMap;

    @Override
    public void init(PluginConfiguration config)
            throws ExternalSystemException {
        this.annotatorColumnName = DEFAULT_ANNOTATOR_COLUMN_NAME;
        this.dataSetColumnName = DEFAULT_DATA_SET_COLUMN_NAME;
        this.slideCodeColumnName = DEFAULT_SLIDE_CODE_COLUMN_NAME;
        this.tileColumnName = DEFAULT_TILE_COLUMN_NAME;

        this.annotatorColumn = -1;
        this.slideCodeColumn = -1;
        this.tileColumn = -1;
        this.isColumnMappingComplete = false;

        this.annotatorTileNamesToDataSetListMap =
                new HashMap<String, Map<String, List<String>>>();

        String sortPattern = null;
        String value;
        final Map<String, String> props = config.getProperties();
        for (String key : props.keySet()) {
            value = props.get(key);
            if (SORT_PATTERN.equals(key)) {
                sortPattern = value;
            } else if (ANNOTATOR_FIELD_NAME.equals(key)) {
                this.annotatorColumnName = value;
            } else if (DATA_SET_FIELD_NAME.equals(key)) {
                this.dataSetColumnName = value;
            } else if (SLIDE_CODE_FIELD_NAME.equals(key)) {
                this.slideCodeColumnName = value;
            } else if (TILE_FIELD_NAME.equals(key)) {
                this.tileColumnName = value;
            } else {
                addTileNameMapping(value);
            }
        }

        // sortPattern notes:
        //   - first capturing group must be equal for second capturing group
        //     to be used in numeric sort
        //   - third capturing group only used when first and second are equal
        //   - '(?: )' identifies a non-capturing group
        if ((sortPattern == null) || (sortPattern.length() == 0)) {
            // use L number if slide code matches
            sortPattern = "([A-Z]\\d++).*_R1_L(\\d++)[_\\.](.*)";
        }
        this.targetComparator = new NumberComparator(sortPattern);

        this.targetFileToDataSetMap = new HashMap<File, String>();
    }

    @Override
    public PluginDataRow updateRow(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        row.applyPluginDataValue(dataSetColumnName,
                                 getDataSetValue(row));
        return row;
    }

    private void addTileNameMapping(String mapping)
            throws ExternalSystemException {

        final String[] mappingTriplet = MAPPING_PATTERN.split(mapping);

        if (mappingTriplet.length != 3) {
            throwMappingConfigurationException(
                    mapping,
                    "Mapping triplet length is " + mappingTriplet.length + ".");
        }

        final String annotator = mappingTriplet[0];
        Map<String, List<String>> tileNamesToDataSetListMap =
                annotatorTileNamesToDataSetListMap.get(annotator);
        if (tileNamesToDataSetListMap == null) {
            tileNamesToDataSetListMap = new HashMap<String, List<String>>();
            annotatorTileNamesToDataSetListMap.put(annotator,
                                                   tileNamesToDataSetListMap);
        }

        final String[] tileNames = CSV_PATTERN.split(mappingTriplet[1]);
        final String[] dataSetNames = CSV_PATTERN.split(mappingTriplet[2]);
        final int mappingCount = tileNames.length;

        if (mappingCount > 0) {
            if (mappingCount != dataSetNames.length) {
                throwMappingConfigurationException(
                        mapping,
                        "Tile count (" + mappingCount +
                        ") differs from data set count (" +
                        dataSetNames.length + ").");
            }

            StringBuilder expandedTileNames = new StringBuilder(128);
            List<String> expandedDataSetNames =
                    new ArrayList<String>(mappingCount);
            String tileName = null;
            String dataSetName = null;
            for (int i = 0; i < mappingCount; i++) {
                tileName = getMappingComponentValue(mapping,
                                                    tileName,
                                                    tileNames[i]);
                dataSetName = getMappingComponentValue(mapping,
                                                       dataSetName,
                                                       dataSetNames[i]);
                if (i > 0) {
                    expandedTileNames.append(',');
                }
                expandedTileNames.append(tileName);
                expandedDataSetNames.add(dataSetName);
            }

            tileNamesToDataSetListMap.put(expandedTileNames.toString(),
                                          expandedDataSetNames);
        }
    }

    private String getMappingComponentValue(String mapping,
                                            String previousValue,
                                            String currentValue)
            throws ExternalSystemException {

        String value = currentValue.trim();
        if (".".equals(value)) {
            if (previousValue == null) {
                throwMappingConfigurationException(
                        mapping,
                        "Repeat '.' specified when no previous value exists.");
            } else {
                value = previousValue;
            }
        } else if (value.length() == 0) {
            throwMappingConfigurationException(
                    mapping,
                    "Empty zero length value specified.");
        }
        return value;
    }

    private String throwMappingConfigurationException(String mapping,
                                                      String context)
            throws ExternalSystemException {
        throw new ExternalSystemException(
                INIT_FAILURE_MSG + context + "  Configured mapping '" +
                mapping + "' must follow pattern " +
                "<annotator>:<tile 1>,...,<tile n>:<data set 1>,...,<data set n>");
    }

    /**
     * Derives the data set value for the specified row.
     * This method is synchronized to ensure that concurrent sessions
     * do not step on each other since the plug-in instance is shared
     * between all sessions for the same project,
     *
     * @param  pluginDataRow  current row being processed.
     *
     * @return the derived data set value for the specified row.
     *
     * @throws ExternalDataException
     *   if the data set derivation process fails.
     */
    private synchronized String getDataSetValue(PluginDataRow pluginDataRow)
            throws ExternalDataException {
        final File targetFile = pluginDataRow.getTargetFile();
        if (! targetFileToDataSetMap.containsKey(targetFile)) {
            deriveDataSetValues(pluginDataRow);
        }
        return targetFileToDataSetMap.remove(targetFile);
    }

    private String getRequiredValue(DataTableModel model,
                                    int rowIndex,
                                    int column,
                                    String context)
            throws ExternalDataException {
        final String value = getValue(model, rowIndex, column);
        if ((value == null) || (value.length() == 0)) {
            throw new ExternalDataException(
                    "To derive data set values, a " + context +
                    " must be specified for " +
                    model.getTargetForRow(rowIndex).getName() + ".");
        }
        return value;
    }

    private String getValue(DataTableModel model,
                            int rowIndex,
                            int column)
            throws ExternalDataException {
        final DataField f = (DataField) model.getValueAt(rowIndex, column);
        return f.getCoreValue();
    }


    /**
     * Derives data set for all rows and caches the results so
     * that subsequent row updates don't need to do any real work.
     *
     * @param  pluginDataRow  the current row being processed.
     *
     * @throws ExternalDataException
     *   if the tile values cannot be derived for any reason.
     */
    private void deriveDataSetValues(PluginDataRow pluginDataRow)
            throws ExternalDataException {

        final DataRow dataRow = pluginDataRow.getDataRow();
        DataTableModel model = dataRow.getDataTableModel();
        final int rowCount = model.getRowCount();

        if (! isColumnMappingComplete) {
            setColumnsOfInterest(model);
        }

        // map slideCode to list of targets
        Map<String, List<FileTarget>> locToTargetListMap =
                new HashMap<String, List<FileTarget>>(rowCount);

        Map<FileTarget, String> targetToTileMap =
                new HashMap<FileTarget, String>(rowCount);

        final String firstRowAnnotator = getRequiredValue(model,
                                                          0,
                                                          annotatorColumn,
                                                          annotatorColumnName);
        Map<String, List<String>> tileNamesToDataSetListMap =
                annotatorTileNamesToDataSetListMap.get(firstRowAnnotator);
        if (tileNamesToDataSetListMap == null) {
            tileNamesToDataSetListMap =
                    new HashMap<String, List<String>>();
        }

        FileTarget target;
        String tile;
        String slideCode;
        String annotator;
        List<FileTarget> targetsForCodeList;
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {

            annotator = getRequiredValue(model,
                                         rowIndex,
                                         annotatorColumn,
                                         annotatorColumnName);

            if (! firstRowAnnotator.equals(annotator)) {
                clearDerivedDataSetsAndThrowException(
                        "To derive data set values, all images must be " +
                        "associated with the same annotator.  " +
                        "The current session contains annotators '" +
                        firstRowAnnotator + "' and '" + annotator + "'.");
            }


            target = (FileTarget) model.getTargetForRow(rowIndex);

            tile = getRequiredValue(model,
                                    rowIndex,
                                    tileColumn,
                                    tileColumnName);

            targetToTileMap.put(target, tile);

            slideCode = getRequiredValue(model,
                                         rowIndex,
                                         slideCodeColumn,
                                         slideCodeColumnName);

            targetsForCodeList = locToTargetListMap.get(slideCode);
            if (targetsForCodeList == null) {
                targetsForCodeList = new ArrayList<FileTarget>();
                locToTargetListMap.put(slideCode, targetsForCodeList);
            }

            targetsForCodeList.add(target);
        }

        List<FileTarget> targetList;
        StringBuilder tilesForSlide = new StringBuilder(128);
        List<String> dataSetList;
        int imageCountForSlide;
        for (String loc : locToTargetListMap.keySet()) {

            targetList = locToTargetListMap.get(loc);

            for (FileTarget ft : targetList) {
                if (! targetComparator.isNumberInTargetName(ft)) {
                    clearDerivedDataSetsAndThrowException(
                            "The file '" + ft.getName() +
                            "' does not contain a Zeiss L-Number which " +
                            "is required to derive data set values.");
                }
            }

            // sort by L-Number to line up with tile mapping
            Collections.sort(targetList, targetComparator);

            tilesForSlide.setLength(0);
            for (FileTarget ft : targetList) {
                if (tilesForSlide.length() > 0) {
                    tilesForSlide.append(',');
                }
                tilesForSlide.append(targetToTileMap.get(ft));
            }

            dataSetList =
                    tileNamesToDataSetListMap.get(tilesForSlide.toString());

            imageCountForSlide = targetList.size();

            File file;
            if (dataSetList == null) {

                // add empty data set mappings so that we don't
                // retry derivation for each row
                for (int i = 0; i < imageCountForSlide; i++) {
                    file = targetList.get(i).getFile();
                    targetFileToDataSetMap.put(file, "");
                }

            } else {
                if (dataSetList.size() != imageCountForSlide) {
                    clearDerivedDataSetsAndThrowException(
                            "The image count for slide " + loc + " is " +
                            imageCountForSlide +
                            " but the matching data set list size is " +
                            dataSetList.size() +
                            ".  The plug-in must have a coding error.");
                }

                for (int i = 0; i < imageCountForSlide; i++) {
                    file = targetList.get(i).getFile();
                    if (targetFileToDataSetMap.containsKey(file)) {
                        clearDerivedDataSetsAndThrowException(
                                "The file '" + file.getName() +
                                "' has multiple derived data set values. " +
                                "Is there another session running with " +
                                "the same file?");
                    }
                    targetFileToDataSetMap.put(file, dataSetList.get(i));
                }

            }
        }

    }

    /**
     * Clears all derived data because it may have been generated based
     * upon non-conforming data.  This could remove data for a concurrent
     * session, but that's okay since the other session's data will get
     * regenerated when the next row is processed.
     *
     * @param  message  describes the error that forced this clear.
     *
     * @throws ExternalDataException
     *   always.
     */
    private void clearDerivedDataSetsAndThrowException(String message)
            throws ExternalDataException {
        targetFileToDataSetMap.clear();
        throw new ExternalDataException(message);
    }

    /**
     * Saves locations of data set and slide code columns in the model.
     *
     * @param  model  data model for current session.
     */
    private void setColumnsOfInterest(DataTableModel model)
            throws ExternalDataException {
        final int count = model.getColumnCount();
        String name;
        for (int i = 0; i < count; i++) {
            name = model.getColumnName(i);
            if (name != null) {
                if (name.equals(annotatorColumnName)) {
                    annotatorColumn = i;
                } else if (name.equals(slideCodeColumnName)) {
                    slideCodeColumn = i;
                } else if (name.equals(tileColumnName)) {
                    tileColumn = i;
                }
            }
        }

        verifyColumnFound(annotatorColumn, annotatorColumnName);
        verifyColumnFound(slideCodeColumn, slideCodeColumnName);
        verifyColumnFound(tileColumn, tileColumnName);

        isColumnMappingComplete = true;
    }

    private void verifyColumnFound(int columnIndex,
                                   String columnName)
            throws ExternalDataException {
        if (columnIndex == -1) {
            clearDerivedDataSetsAndThrowException(
                    "The data field '" + columnName +
                    "' is required to derive data set values " +
                    "but is not configured for the current session.");
        }
    }

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize TileToDataSet plug-in.  ";

    private static final Pattern MAPPING_PATTERN = Pattern.compile(":");
    private static final Pattern CSV_PATTERN = Pattern.compile(",");
}
