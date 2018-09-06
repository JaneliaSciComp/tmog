/*
 * Copyright (c) 2017 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.dataFile;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.VerifiedTextModel;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;

/**
 * Tests the {@link ObjectiveCorrectionPlugin} class.
 *
 * @author Eric Trautman
 */
public class ObjectiveCorrectionPluginTest
        extends TestCase {

    private static String TEST_DATA_SET_NAME = "test_data_set";
    private static String TEST_DATA_SET_FIX_VALUE = "data set 99x fix ";

    private static String TEST_TILE_NAME = "test_tile";
    private static String TEST_TILE_FIX_VALUE = "tile 99x fix ";

    private ObjectiveCorrectionPlugin plugin;

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public ObjectiveCorrectionPluginTest(String name) {
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
        return new TestSuite(ObjectiveCorrectionPluginTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        plugin = new ObjectiveCorrectionPlugin();

        PluginConfiguration pluginConfig = new PluginConfiguration();
        pluginConfig.setProperty(TEST_DATA_SET_NAME,
                                 TEST_DATA_SET_FIX_VALUE);
        pluginConfig.setProperty(ObjectiveCorrectionPlugin.TILE_PROPERTY_MAPPING_PREFIX + TEST_TILE_NAME,
                                 TEST_TILE_FIX_VALUE);
        plugin.init(pluginConfig);
    }

    public void testBadConfigurations() throws Exception {
        PluginConfiguration pluginConfig = new PluginConfiguration();
        pluginConfig.setProperty(TEST_DATA_SET_NAME,
                                 "can't parse configured objective");
        try {
            plugin.init(pluginConfig);
            fail("bad data set objective should have caused exception");
        } catch (ExternalSystemException e) {
            // test passed
        }


        pluginConfig = new PluginConfiguration();
        pluginConfig.setProperty(ObjectiveCorrectionPlugin.TILE_PROPERTY_MAPPING_PREFIX + TEST_TILE_NAME,
                                 "can't parse this either");
        try {
            plugin.init(pluginConfig);
            fail("bad data set objective should have caused exception");
        } catch (ExternalSystemException e) {
            // test passed
        }
    }

    public void testUpdateRow() throws Exception {

        final String[][] testData = {

                // test,
                // data set,                     tile,      recorded objective,                scope file name,               expected fix

                {"can't parse recorded objective",
                 "wolfft_central_tile",          "central", "original-recorded-objective",     "file-name_20X_objective.lsm", "original-recorded-objective"},

                {"can't parse file name objective",
                 "wolfft_central_tile",          "central", "original-recorded-20x-objective", "file-name-objective.lsm",     "original-recorded-20x-objective"},

                {"recorded and file name cores match",
                 "wolfft_central_tile",          "central", "recorded-20x-objective",          "file-name_20X_objective.lsm", "recorded-20x-objective"},

                {"missing data set value",
                 "",                             "central", "recorded-20x-objective",          "file-name_63X_objective.lsm", "recorded-20x-objective"},

                {"missing tile value",
                 "wolfft_central_tile",          "",        "recorded-20x-objective",          "file-name_63X_objective.lsm", "recorded-20x-objective"},

                {"recorded and fix cores match",
                 "wolfft_central_tile",          "central", "recorded-63x-objective",          "file-name_20X_objective.lsm", "recorded-63x-objective"},

                {"apply coded data set based fix objective",
                 "asoy_mb_lexa_gal4_40X_512px",  "central", "Plan-Apochromat 20x/0.8",         "file-name_99X_objective.lsm", ObjectiveCorrectionPlugin.FIX_40X_OBJECTIVE_NAME},

                {"apply configured data set based fix objective",
                 TEST_DATA_SET_NAME,             "central", "Plan-Apochromat 20x/0.8",         "file-name_63X_objective.lsm", TEST_DATA_SET_FIX_VALUE},

                {"apply coded tile based fix objective",
                 "umapped_data_set",             "brain",   "Plan-Apochromat 63x/1.4 Oil",     "file-name_99X_objective.lsm", ObjectiveCorrectionPlugin.FIX_20X_OBJECTIVE_NAME},

                {"apply configured tile based fix objective",
                 "umapped_data_set",      TEST_TILE_NAME,   "Plan-Apochromat 63x/1.4 Oil",     "file-name_99X_objective.lsm", TEST_TILE_FIX_VALUE},

                {"apply default fix objective",
                 "umapped_data_set",             "central", "Plan-Apochromat 20x/0.8",         "file-name_99X_objective.lsm", ObjectiveCorrectionPlugin.FIX_63X_OBJECTIVE_NAME}
        };

        String testDescription;
        String expectedResult;
        PluginDataRow pluginDataRow;
        String actualResult;
        for (String[] testValues : testData) {
            testDescription = testValues[0];
            expectedResult = testValues[5];
            pluginDataRow = buildPluginDataRow(testValues);
            pluginDataRow = plugin.updateRow(pluginDataRow);
            actualResult = pluginDataRow.getCoreValue(ObjectiveCorrectionPlugin.DEFAULT_OBJECTIVE_COLUMN_NAME);
            assertEquals(testDescription + " test failed", expectedResult, actualResult);
        }
    }

    public void testValidate() throws Exception {

        final String[][] failureTestData = {

                // test,
                // data set,  tile,    recorded objective,       file name, exception message

                {"missing data set value",
                 "",          "tile",  "recorded-20x-objective", "foo.lsm",  ObjectiveCorrectionPlugin.DEFAULT_DATA_SET_COLUMN_NAME + " value must be specified"},

                {"missing tile value",
                 "data_set",  "",      "recorded-20x-objective", "foo.lsm",  ObjectiveCorrectionPlugin.DEFAULT_TILE_COLUMN_NAME + " value must be specified"},

                {"missing objective value",
                 "data_set",  "tile",  "",                       "foo.lsm",  ObjectiveCorrectionPlugin.DEFAULT_OBJECTIVE_COLUMN_NAME + " value is missing"},

                {"can't parse recorded objective",
                 "data_set",  "tile",  "can't-parse-this",       "foo.lsm",  "A core value (e.g. 20x or 63x) cannot be parsed"},

                {"recorded core does not match fixed core",
                 "data_set",  "tile",  "recorded-20x-objective", "foo.lsm",  "a 63x value is expected"}
        };

        String testDescription;
        String exceptionMessage;
        PluginDataRow pluginDataRow;
        String actualMessage;
        for (String[] testValues : failureTestData) {
            testDescription = testValues[0];
            exceptionMessage = testValues[5];
            pluginDataRow = buildPluginDataRow(testValues);
            try {
                plugin.validate("test-session", pluginDataRow);
                fail(testDescription + " should have caused exception");
            } catch (ExternalDataException e) {
                actualMessage = e.getMessage();
                assertTrue("The '" + testDescription + "' test returned an invalid exception message.\n" +
                           "The exception message should contain '" + exceptionMessage + "'\n" +
                           "but the actual message is:\n" + actualMessage + "\n",
                           actualMessage.contains(exceptionMessage));
            }
        }

        final String[][] validTestValues = {
                { "valid 63x test", "data_set", "tile", "recorded-63x-objective", "foo.lsm" },
                { "valid 40x test", "data_set", "tile", "Plan-Apochromat 40x/1.3 Oil DIC", "foo.lsm" }
        };
        for (final String[] testValues : validTestValues) {
            pluginDataRow = buildPluginDataRow(testValues);
            try {
                plugin.validate("test-session", pluginDataRow);
                // test passed
            } catch (ExternalDataException e) {
                fail("valid test values caused exception: " + e.getMessage());
            }
        }

    }

    private PluginDataRow buildPluginDataRow(String[] testValues) {
        return new PluginDataRow(buildDataRow(testValues[4], testValues[1], testValues[2], testValues[3]));
    }

    private DataRow buildDataRow(String fileName,
                                 String dataSet,
                                 String tile,
                                 String objective) {
        DataRow dataRow = new DataRow(new FileTarget(new File(fileName)));
        dataRow.addField(buildField(ObjectiveCorrectionPlugin.DEFAULT_DATA_SET_COLUMN_NAME, dataSet));
        dataRow.addField(buildField(ObjectiveCorrectionPlugin.DEFAULT_TILE_COLUMN_NAME, tile));
        dataRow.addField(buildField(ObjectiveCorrectionPlugin.DEFAULT_OBJECTIVE_COLUMN_NAME, objective));
        return dataRow;
    }

    private DataField buildField(String displayName,
                                 String value) {
        VerifiedTextModel field = new VerifiedTextModel();
        field.setDisplayName(displayName);
        field.setText(value);
        return field;
    }

}