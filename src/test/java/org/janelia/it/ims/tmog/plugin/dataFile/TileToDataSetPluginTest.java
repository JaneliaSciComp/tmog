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
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.junit.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the {@link TileToDataSetPlugin} class.
 *
 * @author Eric Trautman
 */
public class TileToDataSetPluginTest
        extends TestCase {

    private TileToDataSetPlugin plugin;
    private DataTableModel model;
    private List<DataRow> dataRowList;
    private int annotatorIndex;
    private int dataSetIndex;
    private int slideCodeIndex;
    private int tileIndex;

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public TileToDataSetPluginTest(String name) {
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
        return new TestSuite(TileToDataSetPluginTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        PluginConfiguration pluginConfig = new PluginConfiguration();
        pluginConfig.setProperty("m0",
                                 "Aljoscha Nern:Left Optic Lobe,.:nerna_optic_lobe_left,.");
        plugin = new TileToDataSetPlugin();
        plugin.init(pluginConfig);

        DataFields fields = new DataFields();
        fields.add(buildField(TileToDataSetPlugin.DEFAULT_ANNOTATOR_COLUMN_NAME));
        fields.add(buildField(TileToDataSetPlugin.DEFAULT_DATA_SET_COLUMN_NAME));
        fields.add(buildField(TileToDataSetPlugin.DEFAULT_SLIDE_CODE_COLUMN_NAME));
        fields.add(buildField(TileToDataSetPlugin.DEFAULT_TILE_COLUMN_NAME));

        ProjectConfiguration projectConfig = new ProjectConfiguration();
        projectConfig.setDataFields(fields);

        File file;
        List<FileTarget> fileTargetList = new ArrayList<FileTarget>();
        for (int i = 0; i < 10; i++) {
            file = new File("B" + (i/2) + "_T1_20120315_1_R1_L0"+ i + ".lsm");
//            System.out.println(file.getName());
            fileTargetList.add(new FileTarget(file));
        }

        model = new DataTableModel("File Name", fileTargetList, projectConfig);
        dataRowList = model.getRows();

        annotatorIndex = model.getTargetColumnIndex() + 1;
        dataSetIndex = annotatorIndex + 1;
        slideCodeIndex = dataSetIndex + 1;
        tileIndex = slideCodeIndex + 1;

        int slideLocation;
        DataField annotatorField;
        DataField tileField;
        DataField slideCodeField;
        for (int i = 0; i < model.getRowCount(); i++) {
            annotatorField = (DataField) model.getValueAt(i, annotatorIndex);
            annotatorField.applyValue("Aljoscha Nern");
            tileField = (DataField) model.getValueAt(i, tileIndex);
            tileField.applyValue("Left Optic Lobe");
            slideCodeField = (DataField) model.getValueAt(i, slideCodeIndex);
            slideLocation = i / 2;
            slideCodeField.applyValue("19991111_22_A" + slideLocation);
        }
    }

    public void testValidData() throws Exception {
        DataRow dataRow;
        DataField dataSetField;
        PluginDataRow pluginDataRow;
        for (int i = 0; i < model.getRowCount(); i++) {
            dataRow = dataRowList.get(i);
            dataSetField = (DataField) model.getValueAt(i, dataSetIndex);

            Assert.assertEquals(
                    "data set should be empty before updating row " + i,
                    "", dataSetField.getCoreValue());

            pluginDataRow = new PluginDataRow(dataRow);

            plugin.updateRow(pluginDataRow);

            Assert.assertEquals("invalid data set value for row " + i,
                                "nerna_optic_lobe_left",
                                dataSetField.getCoreValue());
        }
    }

    public void testMultipleAnnotators() throws Exception {
        verifyExternalDataException("multiple annotators",
                                    model.getRowCount() / 2,
                                    "Tanya Wolff",
                                    annotatorIndex);
    }

    public void testMissingSlideCodeValue() throws Exception {
        verifyExternalDataException("empty slide code",
                                    model.getRowCount() / 2,
                                    "",
                                    slideCodeIndex);
    }

    public void testMissingTileColumn() throws Exception {
        PluginConfiguration pluginConfig = new PluginConfiguration();
        pluginConfig.setProperty(TileToDataSetPlugin.TILE_FIELD_NAME,
                                 "MyTileColumn");
        plugin.init(pluginConfig);

        verifyExternalDataException("missing Tile column",
                                    0,
                                    "foo",
                                    tileIndex);
    }

    public void testDotBeforeDefinition() throws Exception {
        PluginConfiguration pluginConfig = new PluginConfiguration();
        pluginConfig.setProperty("m0", "a:t1,t2:.,d2");
        try {
            plugin.init(pluginConfig);
            Assert.fail("invalid mapping should have caused exception");
        } catch (ExternalSystemException e) {
            // test passed
            System.out.println(e.getMessage());
        }
    }

    public void testTileAndDataSetCountMismatch() throws Exception {
        PluginConfiguration pluginConfig = new PluginConfiguration();
        pluginConfig.setProperty("m0", "a:t1,t2,t3:d1,d2");
        try {
            plugin.init(pluginConfig);
            Assert.fail("invalid mapping should have caused exception");
        } catch (ExternalSystemException e) {
            // test passed
            System.out.println(e.getMessage());
        }
    }

    public void testMissingMappingComponent() throws Exception {
        PluginConfiguration pluginConfig = new PluginConfiguration();
        pluginConfig.setProperty("m0", "a:d1,d2");
        try {
            plugin.init(pluginConfig);
            Assert.fail("invalid mapping should have caused exception");
        } catch (ExternalSystemException e) {
            // test passed
            System.out.println(e.getMessage());
        }
    }

    private DataField buildField(String displayName) {
        VerifiedTextModel field = new VerifiedTextModel();
        field.setDisplayName(displayName);
        return field;
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