/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.dataFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.config.DataFields;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.config.ProjectConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.VerifiedTextModel;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.junit.Assert;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the {@link SlideCodeConsensusPlugin} class.
 *
 * @author Eric Trautman
 */
public class SlideConsensusPluginTest
        extends TestCase {

    private SlideCodeConsensusPlugin plugin;
    private DataTableModel model;
    private List<DataRow> dataRowList;
    private int dataSetIndex;
    private int slideCodeIndex;
    private int ageIndex;

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public SlideConsensusPluginTest(String name) {
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
        return new TestSuite(SlideConsensusPluginTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        PluginConfiguration pluginConfig = new PluginConfiguration();
        pluginConfig.setProperty(SlideCodeConsensusPlugin.SERVICE_URL_PROPERTY_NAME,
                                 "https://sage-responder.int.janelia.org/slide_code_consensus/${Data Set}/${Slide Code}");
        pluginConfig.setProperty(SlideCodeConsensusPlugin.TEST_URL_PROPERTY_NAME,
                                 "https://sage-responder.int.janelia.org/slide_code_consensus/sterneg_ti_mcfo_case_1/20161201_19_A1");
        pluginConfig.setProperty(SlideCodeConsensusPlugin.ROOT_XPATH_PROPERTY_NAME, "slideCodeConsensusData");

        final String AGE_PROPERTY = "Age";
        pluginConfig.setProperty(AGE_PROPERTY, "age");

//                <property name="Annotator" value="annotatedBy" />
//                <property name="Cross Serial Number" value="crossBarcode" />
//                <property name="Full Age" value="fullAge" />
//                <property name="Heat Shock Minutes" value="heatShockMinutes" />
//                <property name="Reporter" value="effector" />
//                <property name="Gender" value="gender" />
//                <property name="GAL4 Line" value="line" />
//                <property name="Tissue Mounting Protocol" value="mountingProtocol" />
//                <property name="First Slice of Z" value="tissueOrientation" />

        plugin = new SlideCodeConsensusPlugin();
        plugin.init(pluginConfig);

        DataFields fields = new DataFields();
        fields.add(buildField(TileToDataSetPlugin.DEFAULT_DATA_SET_COLUMN_NAME));
        fields.add(buildField(TileToDataSetPlugin.DEFAULT_SLIDE_CODE_COLUMN_NAME));
        fields.add(buildField(AGE_PROPERTY));


        ProjectConfiguration projectConfig = new ProjectConfiguration();
        projectConfig.setDataFields(fields);

        List<FileTarget> fileTargetList = new ArrayList<FileTarget>();
        fileTargetList.add(new FileTarget(new File("L3_ZB113_T1_BRN_20230406_32_20X_R1_L13.lsm")));
        fileTargetList.add(new FileTarget(new File("L3_ZB113_T1_BRN_20230406_32_20X_R1_L14.lsm")));

        model = new DataTableModel("File Name", fileTargetList, projectConfig);
        dataRowList = model.getRows();

        dataSetIndex = model.getTargetColumnIndex() + 1;
        slideCodeIndex = dataSetIndex + 1;
        ageIndex = slideCodeIndex + 1;

        DataField dataSetField;
        DataField ageField;
        DataField slideCodeField;
        for (int i = 0; i < 2; i++) {
            dataSetField = (DataField) model.getValueAt(i, dataSetIndex);
            dataSetField.applyValue("ditp_polarity_case_4");
            slideCodeField = (DataField) model.getValueAt(i, slideCodeIndex);
            slideCodeField.applyValue("20230406_32_L4");
            ageField = (DataField) model.getValueAt(i, ageIndex);
            ageField.applyValue("A1");
        }
    }

    public void testValidate() {
        DataRow dataRow;
        PluginDataRow pluginDataRow;
        for (int i = 0; i < model.getRowCount(); i++) {
            dataRow = dataRowList.get(i);
            pluginDataRow = new PluginDataRow(dataRow);

            try {
                plugin.validate("test session", pluginDataRow);
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail("caught exception validating row " + i + ":\n" + e);
            }
        }
    }

    private DataField buildField(String displayName) {
        VerifiedTextModel field = new VerifiedTextModel();
        field.setDisplayName(displayName);
        return field;
    }

}