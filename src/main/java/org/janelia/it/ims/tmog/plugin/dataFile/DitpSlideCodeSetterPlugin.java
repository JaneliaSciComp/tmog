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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Derives slide code values using slide id, current model ordering, and area name.
 * Descending Interneuron images are grouped in ones (brain only) or twos (brain and VNC).
 * Requires slide id and area to be defined for all session images.
 *
 * @author Eric Trautman
 */
public class DitpSlideCodeSetterPlugin
        implements RowUpdater {

    public static final String DEFAULT_SLIDE_ID_COLUMN_NAME = "Slide ID";
    public static final String DEFAULT_SLIDE_ROW_COLUMN_NAME = "Slide Row";
    public static final String DEFAULT_SLIDE_COLUMN_COLUMN_NAME = "Slide Column";
    public static final String DEFAULT_AREA_COLUMN_NAME = "Area";
    public static final String DEFAULT_FIRST_AREA_NAME = "Brain";

    private String slideIdColumnName;
    private String slideRowColumnName;
    private String slideColumnColumnName;
    private String areaColumnName;
    private String firstAreaName;

    private int slideIdColumn;
    private int areaColumn;
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
        this.areaColumnName = DEFAULT_AREA_COLUMN_NAME;
        this.firstAreaName = DEFAULT_FIRST_AREA_NAME;
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
     * @throws org.janelia.it.ims.tmog.plugin.ExternalDataException
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
     * @throws org.janelia.it.ims.tmog.plugin.ExternalDataException
     *   if the slide data cannot be derived for any reason.
     */
    private void deriveSlideData(PluginDataRow pluginDataRow)
            throws ExternalDataException {

        final DataRow dataRow = pluginDataRow.getDataRow();
        DataTableModel model = dataRow.getDataTableModel();
        if (! isColumnMappingComplete) {
            setColumnsOfInterest(model);
        }

        Map<String, SlideData> slideIdToDataMap =
                new HashMap<String, SlideData>(model.getRowCount());

        String slideId;
        SlideData slideData;
        String area;
        FileTarget target;
        File file;
        final int rowCount = model.getRowCount();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {

            slideId = getRequiredRowValue(model, rowIndex, slideIdColumn, slideIdColumnName);
            area = getRequiredRowValue(model, rowIndex, areaColumn, areaColumnName);
            slideData = slideIdToDataMap.get(slideId);
            if (slideData == null) {
                slideData = new SlideData('A', 1);
            } else if (firstAreaName.equals(area)) {
                slideData = slideData.next();
            }
            slideIdToDataMap.put(slideId, slideData);

            target = (FileTarget) model.getTargetForRow(rowIndex);
            file = target.getFile();
            if (targetFileToSlideDataMap.containsKey(file)) {
                clearDerivedTilesAndThrowException(
                        "The file '" + file.getName() +
                        "' has multiple derived slide code values. " +
                        "Is there another session running with " +
                        "the same file?");
            }
            targetFileToSlideDataMap.put(file, slideData);
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
                } else if (name.equals(areaColumnName)) {
                    areaColumn = i;
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

        public SlideData next() {
            char nextRow = row;
            int nextColumn = column + 1;
            if (nextColumn % 10 == 0) {
                nextRow++;
                nextColumn = 1;
            }
            return new SlideData(nextRow, nextColumn);
        }
    }
}
