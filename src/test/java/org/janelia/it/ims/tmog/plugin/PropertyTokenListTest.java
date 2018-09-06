/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.DataFieldGroupModel;
import org.janelia.it.ims.tmog.field.StaticDataModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests the PropertyTokenList class.
 *
 * @author Eric Trautman
 */
public class PropertyTokenListTest
        extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public PropertyTokenListTest(String name) {
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
        return new TestSuite(PropertyTokenListTest.class);
    }

    /**
     * Tests the parseTokens method with valid tokens.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testValidTokens() throws Exception {
        HashMap<String, String> props = new HashMap<String, String>();
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

        Map<String, DataField> nameToFieldMap =
                new HashMap<String, DataField>();

        nameToFieldMap.put(field1.getDisplayName(), field1);
        nameToFieldMap.put(field2.getDisplayName(), field2);
        nameToFieldMap.put(fieldWithSpaces.getDisplayName(), fieldWithSpaces);
        nameToFieldMap.put(nullField.getDisplayName(), nullField);

        String tokenString;
        String expectedValue;
        PropertyTokenList tokenList;
        List<String> values;
        for (String[] testCaseData : testData) {
            tokenString = testCaseData[0];
            expectedValue = testCaseData[1];
            tokenList = new PropertyTokenList(tokenString, props);
            values = tokenList.deriveValues(nameToFieldMap, false);
            assertEquals("incorrect number of values returned for '" +
                         tokenString + "'",
                         1, values.size());
            assertEquals("incorrect value returned for '" +
                         tokenString + "'",
                         expectedValue, values.get(0));
        }

    }

    /**
     * Tests the parseTokens method with invalid tokens.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testInvalidTokens() throws Exception {
        HashMap<String, String> props = new HashMap<String, String>();
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
                new PropertyTokenList(invalidValue, props);
                fail("invalid value '" + invalidValue +
                     "' did not cause exception");
            } catch (IllegalArgumentException e) {
                assertTrue(true); // test passed
            }
        }
    }

    /**
     * Tests the deriveValues method with a concatenated field group.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testDeriveValuesWithConcatenatedFieldGroup() throws Exception {
        DataFieldGroupModel fieldGroup = createFieldGroup();
        Map<String, DataField> nameToFieldMap = createFieldMap(fieldGroup);
        Map<String, String> props = new HashMap<String, String>();
        props.put("GROUP-1:field:rowFormat", "@${column-0}@${column-1}@");
        props.put("GROUP-1:field:concatenate", "_");

        // ------------------------------------
        // test 1 row

        String tokenString = "a${GROUP-1:field}b${other-field}c";
        String expectedValue = "a@v-0-99@v-1-99@b-other-v-c";
        PropertyTokenList tokenList = new PropertyTokenList(tokenString, props);
        List<String> values = tokenList.deriveValues(nameToFieldMap, false);
        assertEquals("incorrect number of values returned for '" +
                     tokenString + "' with 1 row",
                     1, values.size());
        assertEquals("incorrect value returned for '" +
                     tokenString + "' with 1 row",
                     expectedValue, values.get(0));

        // ------------------------------------
        // test 2 rows

        fieldGroup.addRow(1);
        List<List<DataField>> fieldRows = fieldGroup.getFieldRows();
        int column = 0;
        for (DataField f : fieldRows.get(1)) {
            StaticDataModel nestedField = (StaticDataModel) f;
            nestedField.setValue("v-" + column + "-88");
            column++;
        }

        expectedValue = "a@v-0-99@v-1-99@_@v-0-88@v-1-88@b-other-v-c";
        values = tokenList.deriveValues(nameToFieldMap, false);
        assertEquals("incorrect number of values returned for '" +
                     tokenString + "' with 2 rows",
                     1, values.size());
        assertEquals("incorrect value returned for '" +
                     tokenString + "' with 2 rows",
                     expectedValue, values.get(0));

        // ------------------------------------
        // test 2 rows - sorted

        props.put("GROUP-1:field:sort", "true");
        tokenList = new PropertyTokenList(tokenString, props);
        expectedValue = "a@v-0-88@v-1-88@_@v-0-99@v-1-99@b-other-v-c";
        values = tokenList.deriveValues(nameToFieldMap, false);
        assertEquals("incorrect number of values returned for '" +
                     tokenString + "' with 2 sorted rows",
                     1, values.size());
        assertEquals("incorrect value returned for '" +
                     tokenString + "' with 2 sorted rows",
                     expectedValue, values.get(0));
    }

    /**
     * Tests the deriveValues method with a iterated field group.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testDeriveValuesWithIteratedFieldGroup() throws Exception {
        DataFieldGroupModel fieldGroup = createFieldGroup();
        Map<String, DataField> nameToFieldMap = createFieldMap(fieldGroup);
        Map<String, String> props = new HashMap<String, String>();
        props.put("GROUP-2:field:rowFormat", "@${column-0}@${column-1}@");

        // ------------------------------------
        // test 1 row

        String tokenString = "a${GROUP-2:field}b${other-field}c";
        String expectedValue = "a@v-0-99@v-1-99@b-other-v-c";
        PropertyTokenList tokenList = new PropertyTokenList(tokenString, props);
        List<String> values = tokenList.deriveValues(nameToFieldMap, false);
        assertEquals("incorrect number of values returned for '" +
                     tokenString + "' with 1 row",
                     1, values.size());
        assertEquals("incorrect value returned for '" +
                     tokenString + "' with 1 row",
                     expectedValue, values.get(0));

        // ------------------------------------
        // test 2 rows

        fieldGroup.addRow(1);
        List<List<DataField>> fieldRows = fieldGroup.getFieldRows();
        int column = 0;
        for (DataField f : fieldRows.get(1)) {
            StaticDataModel nestedField = (StaticDataModel) f;
            nestedField.setValue("v-" + column + "-88");
            column++;
        }

        expectedValue = "a@v-0-99@v-1-99@b-other-v-c";
        values = tokenList.deriveValues(nameToFieldMap, false);
        assertEquals("incorrect number of values returned for '" +
                     tokenString + "' with 2 rows",
                     2, values.size());
        assertEquals("incorrect value returned for first row of '" +
                     tokenString + "'",
                     expectedValue, values.get(0));

        expectedValue = "a@v-0-88@v-1-88@b-other-v-c";
        assertEquals("incorrect value returned for second row of '" +
                     tokenString + "'",
                     expectedValue, values.get(1));
    }

    private DataFieldGroupModel createFieldGroup() {
        DataFieldGroupModel fieldGroup = new DataFieldGroupModel();
        fieldGroup.setDisplayName("field");

        StaticDataModel field;
        for (int column = 0; column < 2; column++) {
            field = new StaticDataModel();
            field.setName("column-" + column);
            field.setValue("v-" + column + "-99");
            fieldGroup.add(field);
        }

        return fieldGroup;
    }

    private Map<String, DataField> createFieldMap(DataFieldGroupModel fieldGroup) {
        StaticDataModel otherField = new StaticDataModel();
        otherField.setName("other-field");
        otherField.setValue("-other-v-");

        Map<String, DataField> nameToFieldMap =
                new HashMap<String, DataField>();

        nameToFieldMap.put(fieldGroup.getDisplayName(), fieldGroup);
        nameToFieldMap.put(otherField.getDisplayName(), otherField);
        
        return nameToFieldMap;
    }

}