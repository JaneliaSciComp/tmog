/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.dataFile;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.config.DataFields;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.config.ProjectConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.VerifiedTextModel;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.junit.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tests the {@link TileSetterPlugin} class.
 *
 * @author Eric Trautman
 */
public class TileSetterPluginTest
        extends TestCase {

    private TileSetterPlugin plugin;
    private DataTableModel model;
    private List<DataRow> dataRowList;
    private int dataSetIndex;
    private int slideCodeIndex;
    private int tileIndex;

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public TileSetterPluginTest(String name) {
        super(name);
    }

    /**
     * Static method to return a suite of all tests.
     * <p/>
     * The JUnit framework uses Java reflection to build a suite of all public
     * methods that have names like "testXXXX()".
     *
     * @return suite of all tests defined in this class.
     */
    public static Test suite() {
        return new TestSuite(TileSetterPluginTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        PluginConfiguration pluginConfig = new PluginConfiguration();
        plugin = new TileSetterPlugin();
        plugin.init(pluginConfig);

        DataFields fields = new DataFields();
        fields.add(buildField(TileSetterPlugin.DEFAULT_DATA_SET_COLUMN_NAME));
        fields.add(buildField(TileSetterPlugin.DEFAULT_SLIDE_CODE_COLUMN_NAME));
        fields.add(buildField(TileSetterPlugin.DEFAULT_TILE_COLUMN_NAME));

        ProjectConfiguration projectConfig = new ProjectConfiguration();
        projectConfig.setDataFields(fields);

        Random random = new Random();
        File file;
        List<FileTarget> fileTargetList = new ArrayList<FileTarget>();
        int channelName;
        for (int i = 0; i < 10; i++) {
            channelName = random.nextInt(500);
            file = new File("test_20130102_11_H2_" + channelName +
                            "-71-440__R1_L0" + i + "_9999888.lsm");
            System.out.println(file.getName());
            fileTargetList.add(new FileTarget(file));
        }

        model = new DataTableModel("File Name", fileTargetList, projectConfig);
        dataRowList = model.getRows();

        dataSetIndex = model.getTargetColumnIndex() + 1;
        slideCodeIndex = dataSetIndex + 1;
        tileIndex = slideCodeIndex + 1;

        int slideLocation = 1;
        DataField dataSetField;
        DataField slideCodeField;
        for (int i = 0; i < model.getRowCount(); i++) {
            dataSetField = (DataField) model.getValueAt(i, dataSetIndex);
            if (i % 2 == 0) {
                dataSetField.applyValue(
                        TileSetterPlugin.DEFAULT_20X_DATA_SET);
            }
            slideCodeField = (DataField) model.getValueAt(i, slideCodeIndex);

            if (! isSameSlideLocation(i)) {
                slideLocation++;
            }
            slideCodeField.applyValue("19991111_22_A" + slideLocation);
        }
    }

    public void testValidData() throws Exception {
        DataRow dataRow;
        DataField tileField;
        PluginDataRow pluginDataRow;
        String expectedValue;
        for (int i = 0; i < model.getRowCount(); i++) {
            dataRow = dataRowList.get(i);
            tileField = (DataField) model.getValueAt(i, tileIndex);

            Assert.assertEquals("tile should be empty before updating row " + i,
                                "", tileField.getCoreValue());

            pluginDataRow = new PluginDataRow(dataRow);

            plugin.updateRow(pluginDataRow);

            if (isSameSlideLocation(i)) {
                expectedValue = "ventral_nerve_cord";
            } else {
                expectedValue = "brain";
            }

            Assert.assertEquals("invalid tile value for row " + i,
                                expectedValue, tileField.getCoreValue());
        }
    }

    public void testMultipleDataSets() throws Exception {
        verifyExternalDataException("multiple data sets",
                                    model.getRowCount() / 2,
                                    "another-data-set",
                                    dataSetIndex);
    }

    public void testMissingSlideCode() throws Exception {
        verifyExternalDataException("empty slide code",
                                    (model.getRowCount() / 2) + 1,
                                    "",
                                    slideCodeIndex);
    }

    private DataField buildField(String displayName) {
        VerifiedTextModel field = new VerifiedTextModel();
        field.setDisplayName(displayName);
        return field;
    }

    // create alternating #images per location: 1, 2, 1, 2, ...
    private boolean isSameSlideLocation(int index) {
        return (((index - 2) % 3) == 0);
    }

    private void verifyExternalDataException(String failureMessage,
                                            int rowIndex,
                                            String fieldValue,
                                            int fieldIndex)
            throws Exception {

        DataField field = (DataField) model.getValueAt(rowIndex, fieldIndex);
        field.applyValue(fieldValue);

        DataRow dataRow = dataRowList.get(rowIndex);
        PluginDataRow pluginDataRow = new PluginDataRow(dataRow);

        try {
            plugin.updateRow(pluginDataRow);
            Assert.fail(failureMessage + " should have caused exception");
        } catch (ExternalDataException e) {
            // test passed
            System.out.println(e.getMessage());
        }
    }

}