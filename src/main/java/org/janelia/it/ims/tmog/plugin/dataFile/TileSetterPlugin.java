/*
 * Copyright (c) 2016 Howard Hughes Medical Institute.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Derives tile values using slide location and data set.
 * Requires slide location and data set to be defined for all
 * session images.  Also requires that all session images have
 * the same data set.  Tile values are derived based upon the number
 * of images for each slide, meaning that the session must contain
 * all images for a particular slide.  Pre-screen images are organized
 * within slide directories, so this assumption should hold.
 *
 * @author Eric Trautman
 */
public class TileSetterPlugin implements RowUpdater {

    public static final String DEFAULT_DATA_SET_COLUMN_NAME = "Data Set";
    public static final String DEFAULT_SLIDE_CODE_COLUMN_NAME = "Slide Code";
    public static final String DEFAULT_TILE_COLUMN_NAME = "Tile";

    public static final String DEFAULT_20X_DATA_SET = "DEFAULT_20X_DATA_SET";

    private String dataSetColumnName;
    private String slideCodeColumnName;
    private String tileColumnName;

    private int dataSetColumn;
    private int slideCodeColumn;
    private boolean isColumnMappingComplete;
    private NumberComparator targetComparator;
    private Map<String, DataSetTiles> dataSetToTilesMap;

    private Map<File, String> targetFileToTileMap;

    @Override
    public PluginDataRow updateRow(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        row.applyPluginDataValue(tileColumnName,
                                 getTileValue(row));
        return row;
    }

    @Override
    public void init(PluginConfiguration config)
            throws ExternalSystemException {
        this.dataSetColumnName = DEFAULT_DATA_SET_COLUMN_NAME;
        this.slideCodeColumnName = DEFAULT_SLIDE_CODE_COLUMN_NAME;
        this.tileColumnName = DEFAULT_TILE_COLUMN_NAME;
        this.isColumnMappingComplete = false;

        String sortPattern = config.getProperty("sortPattern");
        if ((sortPattern == null) || (sortPattern.length() == 0)) {
            // use L number if slide code matches
            sortPattern = ".*_(\\d{8}_\\d{2}_[A-Z]\\d).*_R._L(\\d++)_(.*)";
        }
        this.targetComparator = new NumberComparator(sortPattern);

        this.targetFileToTileMap = new HashMap<>();
        buildDataSetToTilesMap();
    }

    /**
     * Builds the maps used to derive tile values.
     * The maps are coded here because they are not expected to change often
     * and they would be difficult to configure using the current
     * infrastructure.
     */
    private void buildDataSetToTilesMap() {

        dataSetToTilesMap = new HashMap<>();

        // logic/mapping pulled from Rebecca's spreadsheet
        // data set names pulled from "http://jacs-data.int.janelia.org:8180/rest-v1/data/dataSet/sage?owners=user:asoy&amp;sageSync=true"
        // tile names pulled from "http://sage.int.janelia.org/sage-ws/cvs/fly_light_adult_tiles"

//        final String a = "abdominal";
        final String b = "brain";
        final String c = "central";
//        final String lc = "left_central";
        final String dm = "dorsal_medial";
        final String ld = "left_dorsal";
        final String l = "left_optic_lobe";
        final String meso = "mesothorasic";
        final String meta = "metathorasic";
        final String pro = "prothorasic";
//        final String rc = "right_central";
        final String rd = "right_dorsal";
        final String r = "right_optic_lobe";
        final String v = "ventral";
        final String vnc = "ventral_nerve_cord";

        DataSetTiles dataSetTiles = new DataSetTiles(new String[][] {
                {b},
                {b, vnc}
        });

        dataSetToTilesMap.put(DEFAULT_20X_DATA_SET,
                              dataSetTiles);

        dataSetTiles = new DataSetTiles(new String[][] {
                {ld, ld, rd, rd},                                                     // (4)  LDRD
                {dm, dm},                                                             // (2)  C
                {v, v, ld, ld, rd, rd},                                               // (6)  VLDRD
                {v, v, ld, ld, rd, rd, pro, pro, meso, meso, meta, meta},             // (12) VLDRD T1-T3
                {l, l, v, v, ld, ld, rd, rd, r, r},                                   // (10) Whole Brain
                {l, l, v, v, ld, ld, rd, rd, r, r, pro, pro, meso, meso, meta, meta}  // (16) Whole CNS
        });

        dataSetToTilesMap.put("asoy_mb_polarity_case_1", dataSetTiles);
        dataSetToTilesMap.put("asoy_mb_polarity_case_2", dataSetTiles);
        dataSetToTilesMap.put("asoy_mb_split_mcfo_case_1", dataSetTiles);

        dataSetTiles = new DataSetTiles(new String[][] {
                {ld, rd},                                                             // (2)  LDRD
                {dm},                                                                 // (1)  C
                {v, ld, rd},                                                          // (3)  VLDRD
                {v, ld, rd, pro, meso, meta},                                         // (6)  VLDRD T1-T3
                {l, v, ld, rd, r},                                                    // (5)  Whole Brain
                {l, v, ld, rd, r, pro, meso, meta}                                    // (8)  Whole CNS
        });

        dataSetToTilesMap.put("asoy_mb_polarity_case_3", dataSetTiles);
        dataSetToTilesMap.put("asoy_mb_polarity_case_4", dataSetTiles);
        dataSetToTilesMap.put("jenetta_stabilized_split_case_3", dataSetTiles);

        dataSetTiles = new DataSetTiles(new String[][] {
                {c},
                {ld, rd}
        });

        dataSetToTilesMap.put("asoy_cell_count", dataSetTiles);
    }

    /**
     * Derives the tile value for the specified row.
     * This method is synchronized to ensure that concurrent sessions
     * do not step on each other since the plug-in instance is shared
     * between all sessions for the same project,
     *
     * @param  pluginDataRow  current row being processed.
     *
     * @return the derived tile value for the specified row.
     *
     * @throws ExternalDataException
     *   if the tile cannot be derived.
     */
    private synchronized String getTileValue(PluginDataRow pluginDataRow)
            throws ExternalDataException {
        final File targetFile = pluginDataRow.getTargetFile();
        if (! targetFileToTileMap.containsKey(targetFile)) {
            deriveTileValues(pluginDataRow);
        }
        return targetFileToTileMap.remove(targetFile);
    }

    /**
     * Derives tile values for all rows and caches the results so
     * that subsequent row updates don't need to do any real work.
     *
     * @param  pluginDataRow  the current row being processed.
     *
     * @throws ExternalDataException
     *   if the tile values cannot be derived for any reason.
     */
    private void deriveTileValues(PluginDataRow pluginDataRow)
            throws ExternalDataException {

        final DataRow dataRow = pluginDataRow.getDataRow();
        DataTableModel model = dataRow.getDataTableModel();
        if (! isColumnMappingComplete) {
            setColumnsOfInterest(model);
        }

        // map location (slideCode + dataSet) to list of targets
        Map<SlideAndDataSet, List<FileTarget>> locToTargetListMap =
                new HashMap<>();

        String dataSet = null;
        SlideAndDataSet slideAndDataSet;
        List<FileTarget> targetsForCodeList;
        final int rowCount = model.getRowCount();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {

            slideAndDataSet = new SlideAndDataSet(model, rowIndex);

            if (dataSet == null) {
                dataSet = slideAndDataSet.getDataSet();
            } else if (! dataSet.equals(slideAndDataSet.getDataSet())) {
                throw new ExternalDataException(
                        "Multiple data sets ('" + dataSet + "' and '" +
                        slideAndDataSet.getDataSet() + "') have been defined " +
                        "for this session. All images must have the same " +
                        "data set to reliably derive tile values.");
            }

            targetsForCodeList = locToTargetListMap.get(slideAndDataSet);
            if (targetsForCodeList == null) {
                targetsForCodeList = new ArrayList<>();
                locToTargetListMap.put(slideAndDataSet, targetsForCodeList);
            }

            targetsForCodeList.add(
                    (FileTarget) model.getTargetForRow(rowIndex));
        }

        final DataSetTiles dataSetTiles = dataSetToTilesMap.get(dataSet);

        if (dataSetTiles != null) {

            List<FileTarget> targetList;
            int totalImageCount;
            String tileValue;
            for (SlideAndDataSet loc : locToTargetListMap.keySet()) {

                targetList = locToTargetListMap.get(loc);

                for (FileTarget ft : targetList) {
                    if (! targetComparator.isNumberInTargetName(ft)) {
                        clearDerivedTilesAndThrowException(
                                "The file '" + ft.getName() +
                                "' does not contain a Zeiss L-Number which " +
                                "is required to derive tile values.");
                    }
                }

                // sort by L-Number to line up with tile mapping
                Collections.sort(targetList, targetComparator);

                File file;
                totalImageCount = targetList.size();
                for (int i = 0; i < totalImageCount; i++) {
                    tileValue = dataSetTiles.getTileValue(i, totalImageCount);
                    file = targetList.get(i).getFile();
                    if (targetFileToTileMap.containsKey(file)) {
                        clearDerivedTilesAndThrowException(
                                "The file '" + file.getName() +
                                "' has multiple derived tile values. " +
                                "Is there another session running with " +
                                "the same file?");
                    }
                    targetFileToTileMap.put(file,
                                            tileValue);
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
    private void clearDerivedTilesAndThrowException(String message)
            throws ExternalDataException {
        targetFileToTileMap.clear();
        throw new ExternalDataException(message);
    }

    /**
     * Saves locations of data set and slide code columns in the model.
     *
     * @param  model  data model for current session.
     */
    private void setColumnsOfInterest(DataTableModel model) {
        final int count = model.getColumnCount();
        String name;
        for (int i = 0; i < count; i++) {
            name = model.getColumnName(i);
            if (name != null) {
                if (name.equals(dataSetColumnName)) {
                    dataSetColumn = i;
                } else if (name.equals(slideCodeColumnName)) {
                    slideCodeColumn = i;
                }
            }
        }
        isColumnMappingComplete = true;
    }

    /**
     * Helper class to facilitate sorting of target images by data set
     * and slide code.
     */
    public class SlideAndDataSet {

        private String slideCode;
        private String dataSet;

        public SlideAndDataSet(DataTableModel model,
                               int rowIndex)
                throws ExternalDataException {
            slideCode = getRequiredValue(model,
                                         rowIndex,
                                         slideCodeColumn,
                                         slideCodeColumnName);
            dataSet = getValue(model,
                               rowIndex,
                               dataSetColumn);

            // hack to use default 20x data set when one has not been provided
            if ((dataSet == null) || (dataSet.length() == 0)) {
                dataSet = DEFAULT_20X_DATA_SET;
            }
        }

        public String getDataSet() {
            return dataSet;
        }

        @Override
        public boolean equals(Object o) {
            boolean isEqual = false;
            if (this == o) {
                isEqual = true;
            } else if (o instanceof SlideAndDataSet) {
                final SlideAndDataSet that = (SlideAndDataSet) o;
                isEqual = slideCode.equals(that.slideCode) &&
                          dataSet.equals(that.dataSet);
            }
            return isEqual;
        }

        @Override
        public int hashCode() {
            return slideCode.hashCode();
        }

        private String getRequiredValue(DataTableModel model,
                                        int rowIndex,
                                        int column,
                                        String context)
                throws ExternalDataException {
            final String value = getValue(model, rowIndex, column);
            if ((value == null) || (value.length() == 0)) {
                throw new ExternalDataException(
                        "To derive tile values, a " + context +
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
    }

    /**
     * The number of images for each slide location (slide code) determines
     * the ordered list of tile values for the images.  This class
     * captures the tile value lists for a specific data set.
     */
    public class DataSetTiles {
        private Map<Integer, List<String>> countToTileNameListMap;

        public DataSetTiles(String[][] tileNameLists) {
            countToTileNameListMap = new HashMap<>();
            for (String[] tileNameList : tileNameLists) {
                countToTileNameListMap.put(tileNameList.length,
                                           Arrays.asList(tileNameList));
            }
        }

        public String getTileValue(int imageIndex, int totalImageCount) {
            String tileValue = null;
            List<String> tileNameList =
                    countToTileNameListMap.get(totalImageCount);
            if (tileNameList != null) {
                tileValue = tileNameList.get(imageIndex);
            }
            return tileValue;
        }
    }
}
