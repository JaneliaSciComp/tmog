/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.field.StaticDataModel;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.target.FileTarget;

/**
 * Tests the {@link SimpleSetter} class.
 *
 * @author Eric Trautman
 */
public class SimpleSetterTest
        extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public SimpleSetterTest(String name) {
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
        return new TestSuite(SimpleSetterTest.class);
    }

    /**
     * Tests the {@link SimpleSetter#deriveValue} method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testDeriveValueWithoutMapDefault() throws Exception {

        final String fieldName = "tile";

        final String explicitlyMappedValue = "vnc_verify";
        final String explicitlyMappedResult = "ventral_nerve_cord";
        final String unmappedValue = "left_optic_lobe";

        final String configuredPropertyValue = fieldName + SimpleSetter.FIELD_MAP_IDENTIFIER +
                                               explicitlyMappedValue + '=' + explicitlyMappedResult;

        final SimpleSetter setterWithMap = new SimpleSetter("propertyName", configuredPropertyValue);


        validateMappedValue(setterWithMap, fieldName, explicitlyMappedValue, explicitlyMappedResult);
        validateMappedValue(setterWithMap, fieldName, unmappedValue, unmappedValue);
    }

    /**
     * Tests the {@link SimpleSetter#deriveValue} method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testDeriveValueWithMapDefault() throws Exception {

        final String fieldName = "tile";

        final String explicitlyMappedValue = "ventral_nerve_cord";
        final String explicitlyMappedResult = "VNC";
        final String defaultMappedResult = "Brain";

        final String configuredPropertyValue = fieldName + SimpleSetter.FIELD_MAP_IDENTIFIER +
                                               SimpleSetter.MAP_DEFAULT_KEY + '=' + defaultMappedResult + '|' +
                                               explicitlyMappedValue + '=' + explicitlyMappedResult;

        final SimpleSetter setterWithMap = new SimpleSetter("propertyName", configuredPropertyValue);


        validateMappedValue(setterWithMap, fieldName, explicitlyMappedValue, explicitlyMappedResult);
        validateMappedValue(setterWithMap, fieldName, "left_optic_lobe", defaultMappedResult);
    }

    private void validateMappedValue(SimpleSetter setter,
                                     String fieldName,
                                     String fieldValue,
                                     String expectedMappedResult) {
        StaticDataModel field = new StaticDataModel();
        field.setName(fieldName);
        field.setValue(fieldValue);

        final FileTarget testTarget = new FileTarget(null);
        DataRow testDataRow = new DataRow(testTarget);
        testDataRow.addField(field);

        final PluginDataRow testRow = new PluginDataRow(testDataRow);

        final String result = setter.deriveValue(testRow);
        assertEquals("incorrect mapped result returned for value '" + fieldValue + "'",
                     expectedMappedResult, result);
    }
}