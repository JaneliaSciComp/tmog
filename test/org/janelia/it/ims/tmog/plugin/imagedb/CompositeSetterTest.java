/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
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
 * Tests the CompositeSetter class.
 *
 * @author Eric Trautman
 */
public class CompositeSetterTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public CompositeSetterTest(String name) {
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
        return new TestSuite(CompositeSetterTest.class);
    }

    /**
     * Tests the parseTokens method with invalid tokens.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testInvalidTokens() throws Exception {
        CompositeSetter setter = new CompositeSetter("propertyType", "test");
        String[] invalidValues = {
                "${",
                "a${foo",
                "${}",
                "a${}b",
                "${a${nested}b}",
                "a${'_missingQuote}b",
                "a${missingQuote_'}b",
                "a${'missingFieldName'}b",
                "a${'missingFieldName''missingFieldName'}b"
        };
        for (String invalidValue : invalidValues) {
            try {
                setter.parseTokens(invalidValue);
                fail("invalid value '" + invalidValue +
                     "' did not cause exception");
            } catch (IllegalArgumentException e) {
                assertTrue(true); // test passed
            }
        }
    }

    /**
     * Tests the getValue method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetValue() throws Exception {
        String[][] testData = {
                // compositeFieldString,           expectedValue
                { "${field1}",                     "value1"},
                { "${field1}${field2}",            "value1value2"},
                { "${field1}${field with spaces}", "value1value3"},
                { "a",                             "a"},
                { "a${field1}b",                   "avalue1b"},
                { "a${field1}b with spaces",       "avalue1b with spaces"},
                { "a_${field1}_b_${field2}_c",     "a_value1_b_value2_c"},
                { "a${'_'field1}b",                "a_value1b"},
                { "a${field1'_'}b",                "avalue1_b"},
                { "a${'_'field1'_'}b",             "a_value1_b"},
                { "a${''field1''}b",               "avalue1b"},
                { "${nullField}",                  ""},
                { "a${nullField}b",                "ab"},
                { "a${'_'nullField'_'}b",          "ab"},
        };

        StaticDataModel field1 = new StaticDataModel();
        field1.setName("field1");
        field1.setValue("value1");

        StaticDataModel field2 = new StaticDataModel();
        field2.setName("field2");
        field2.setValue("value2");

        StaticDataModel fieldWithSpaces = new StaticDataModel();
        fieldWithSpaces.setName("field with spaces");
        fieldWithSpaces.setValue("value3");

        StaticDataModel nullField = new StaticDataModel();
        nullField.setName("nullField");
        nullField.setValue(null);

        FileTarget testTarget = new FileTarget(null);
        DataRow testDataRow = new DataRow(testTarget);
        testDataRow.addField(field1);
        testDataRow.addField(field2);
        testDataRow.addField(fieldWithSpaces);
        testDataRow.addField(nullField);

        PluginDataRow testRow = new PluginDataRow(testDataRow);

        String compositeFieldString;
        String expectedValue;
        CompositeSetter setter;
        String value;
        for (String[] testCaseData : testData) {
            compositeFieldString = testCaseData[0];
            expectedValue = testCaseData[1];
            setter = new CompositeSetter("propertyType", compositeFieldString);
            value = setter.getValue(testRow);
            assertEquals("incorrect value returned for '" +
                         compositeFieldString + "'",
                         expectedValue, value);
        }

    }

}