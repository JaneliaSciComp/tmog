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
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowUpdater;
import org.janelia.it.ims.tmog.target.FileTarget;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Derives slide code values using slide id and current model ordering.
 * Requires slide id to be defined for all session images.
 *
 * @author Eric Trautman
 */
public class SlideCodeSetterPlugin
        implements RowUpdater {

    public static final String DEFAULT_SLIDE_ID_COLUMN_NAME = "Slide ID";
    public static final String DEFAULT_SLIDE_ROW_COLUMN_NAME = "Slide Row";
    public static final String DEFAULT_SLIDE_COLUMN_COLUMN_NAME = "Slide Column";

    private String slideIdColumnName;
    private String slideRowColumnName;
    private String slideColumnColumnName;

    private int slideIdColumn;
    private boolean isColumnMappingComplete;

    private Map<File, SlideData> targetFileToSlideDataMap;

    @Override
    public PluginDataRow updateRow(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        final SlideData slideData = getSlideData(row);

        if (slideData != null) {
            row.applyPluginDataValue(slideRowColumnName, String.valueOf(slideData.row));
            row.applyPluginDataValue(slideColumnColumnName, String.valueOf(slideData.column));
        }

        return row;
    }

    @Override
    public void init(PluginConfiguration config)
            throws ExternalSystemException {
        this.slideIdColumnName = DEFAULT_SLIDE_ID_COLUMN_NAME;
        this.slideRowColumnName = DEFAULT_SLIDE_ROW_COLUMN_NAME;
        this.slideColumnColumnName = DEFAULT_SLIDE_COLUMN_COLUMN_NAME;
        this.isColumnMappingComplete = false;

        this.targetFileToSlideDataMap = new HashMap<File, SlideData>();
    }

    /**
     * Derives the slide data for the specified row.
     * This method is synchronized to ensure that concurrent sessions
     * do not step on each other since the plug-in instance is shared
     * between all sessions for the same project,
     *
     * @param  pluginDataRow  current row being processed.
     *
     * @return the derived slide data for the specified row.
     *
     * @throws ExternalDataException
     *   if the slide data cannot be derived.
     */
    private synchronized SlideData getSlideData(PluginDataRow pluginDataRow)
            throws ExternalDataException {
        final File targetFile = pluginDataRow.getTargetFile();
        if (! targetFileToSlideDataMap.containsKey(targetFile)) {
            deriveSlideData(pluginDataRow);
        }
        return targetFileToSlideDataMap.remove(targetFile);
    }

    /**
     * Derives slide data for all rows and caches the results so
     * that subsequent row updates don't need to do any real work.
     *
     * @param  pluginDataRow  the current row being processed.
     *
     * @throws ExternalDataException
     *   if the slide data cannot be derived for any reason.
     */
    private void deriveSlideData(PluginDataRow pluginDataRow)
            throws ExternalDataException {

        final DataRow dataRow = pluginDataRow.getDataRow();
        DataTableModel model = dataRow.getDataTableModel();
        if (! isColumnMappingComplete) {
            setColumnsOfInterest(model);
        }

        final Integer defaultOption = 2;
        final Integer[] options = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
        Integer chosenGroupCount = (Integer)
                JOptionPane.showInputDialog(null,
                                            "How many images should be grouped per slide code?",
                                            "Slide Code Group Count",
                                            JOptionPane.QUESTION_MESSAGE,
                                            null,
                                            options,
                                            defaultOption);

        if (chosenGroupCount == null) {
            skipSlideDataDerivation(model);
        } else {
            deriveSlideData(model, chosenGroupCount);
        }

    }

    /**
     * Save null slide data for all target files so that nothing is changed.
     *
     * @param  model  current data model.
     */
    private void skipSlideDataDerivation(DataTableModel model) {

        FileTarget fileTarget;
        final int rowCount = model.getRowCount();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            fileTarget = (FileTarget) model.getTargetForRow(rowIndex);
            targetFileToSlideDataMap.put(fileTarget.getFile(), null);
        }
    }

    /**
     * Derives slide data for all rows and caches the results so
     * that subsequent row updates don't need to do any real work.
     *
     * @param  model       current data model.
     * @param  groupCount  number of images for each slide code.
     *
     * @throws ExternalDataException
     *   if the slide data cannot be derived for any reason.
     */
    private void deriveSlideData(DataTableModel model,
                                 int groupCount)
            throws ExternalDataException {

        Map<String, List<FileTarget>> slideIdToTargetListMap =
                new HashMap<String, List<FileTarget>>(model.getRowCount());

        String slideId;
        List<FileTarget> targetsForSlideIdList;
        final int rowCount = model.getRowCount();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {

            slideId = getRequiredRowValue(model, rowIndex, slideIdColumn, slideIdColumnName);
            targetsForSlideIdList = slideIdToTargetListMap.get(slideId);
            if (targetsForSlideIdList == null) {
                targetsForSlideIdList = new ArrayList<FileTarget>();
                slideIdToTargetListMap.put(slideId, targetsForSlideIdList);
            }
            targetsForSlideIdList.add((FileTarget) model.getTargetForRow(rowIndex));
        }

        List<FileTarget> targetList;
        int totalImageCount;
        for (String key : slideIdToTargetListMap.keySet()) {

            targetList = slideIdToTargetListMap.get(key);

            char currentRowChar = 'A';
            int currentColumn = 0;

            File file;
            totalImageCount = targetList.size();
            for (int i = 0; i < totalImageCount; i++) {
                if (i % groupCount == 0) {
                    currentColumn++;
                    if (currentColumn % 7 == 0) {
                        currentRowChar++;
                        currentColumn = 1;
                    }
                }
                file = targetList.get(i).getFile();
                if (targetFileToSlideDataMap.containsKey(file)) {
                    clearDerivedTilesAndThrowException(
                            "The file '" + file.getName() +
                            "' has multiple derived tile values. " +
                            "Is there another session running with " +
                            "the same file?");
                }
                targetFileToSlideDataMap.put(file,
                                             new SlideData(currentRowChar, currentColumn));
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
     * @throws org.janelia.it.ims.tmog.plugin.ExternalDataException
     *   always.
     */
    private void clearDerivedTilesAndThrowException(String message)
            throws ExternalDataException {
        targetFileToSlideDataMap.clear();
        throw new ExternalDataException(message);
    }

    /**
     * Saves locations of slide data columns from the model.
     *
     * @param  model  data model for current session.
     */
    private void setColumnsOfInterest(DataTableModel model) {
        final int count = model.getColumnCount();
        String name;
        for (int i = 0; i < count; i++) {
            name = model.getColumnName(i);
            if (name != null) {
                if (name.equals(slideIdColumnName)) {
                    slideIdColumn = i;
                }
            }
        }
        isColumnMappingComplete = true;
    }

    private String getRequiredRowValue(DataTableModel model,
                                       int rowIndex,
                                       int column,
                                       String context)
            throws ExternalDataException {
        final String value = getRowValue(model, rowIndex, column);
        if ((value == null) || (value.length() == 0)) {
            throw new ExternalDataException(
                    "To derive slide code values, a " + context +
                    " must be specified for " +
                    model.getTargetForRow(rowIndex).getName() + ".");
        }
        return value;
    }

    private String getRowValue(DataTableModel model,
                               int rowIndex,
                               int column)
            throws ExternalDataException {
        final DataField f = (DataField) model.getValueAt(rowIndex, column);
        return f.getCoreValue();
    }

    public class SlideData {
        public char row;
        public int column;

        public SlideData(char row,
                         int column) {
            this.row = row;
            this.column = column;
        }
    }
}
