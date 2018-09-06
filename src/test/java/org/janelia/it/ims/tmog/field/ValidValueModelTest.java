/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the ValidValueModel class.
 *
 * @author Eric Trautman
 */
public class ValidValueModelTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public ValidValueModelTest(String name) {
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
        return new TestSuite(ValidValueModelTest.class);
    }

    /**
     * Tests the getFileNameValue method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetFileNameValue() throws Exception {
        ValidValueModel model = new ValidValueModel();
        ValidValue valueOne = new ValidValue("nameOne", "valueOne");
        ValidValue valueTwo = new ValidValue("nameTwo", "valueTwo");
        model.addValidValue(valueOne);
        model.addValidValue(valueTwo);

        model.setSelectedValue(valueOne);
        String fileNameValue = model.getFileNameValue();
        assertEquals("invalid value returned for basic value",
                     valueOne.getValue(), fileNameValue);

        String prefix = "pre";
        model.setPrefix(prefix);
        fileNameValue = model.getFileNameValue();
        String expectedValue = prefix + valueOne.getValue();
        assertEquals("invalid value returned for value with prefix",
                     expectedValue, fileNameValue);

        String suffix = "post";
        model.setPrefix(null);
        model.setSuffix(suffix);
        fileNameValue = model.getFileNameValue();
        expectedValue = valueOne.getValue() + suffix;
        assertEquals("invalid value returned for value with suffix",
                     expectedValue, fileNameValue);

        model.setPrefix(prefix);
        fileNameValue = model.getFileNameValue();
        expectedValue = prefix + valueOne.getValue() + suffix;
        assertEquals("invalid value returned for value with prefix and suffix",
                     expectedValue, fileNameValue);

        model.setSelectedValue(null);
        fileNameValue = model.getFileNameValue();
        expectedValue = "";
        assertEquals("invalid value returned for null value with prefix and suffix",
                     expectedValue, fileNameValue);
    }

    /**
     * Tests the getNewInstance method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetNewInstance() throws Exception {
        ValidValueModel model = new ValidValueModel();
        ValidValue valueOne = new ValidValue("nameOne", "valueOne");
        ValidValue valueTwo = new ValidValue("nameTwo", "valueTwo");
        model.addValidValue(valueOne);
        model.addValidValue(valueTwo);
        String prefix = "pre";
        model.setPrefix(prefix);
        String suffix = "post";
        model.setSuffix(suffix);

        model.setSelectedValue(valueOne);

        ValidValueModel newInstance = model.getNewInstance(false);

        Object[][] attributes = {
                {"DisplayName", model.getDisplayName(), newInstance.getDisplayName() },
                {"Required", model.isRequired(), newInstance.isRequired() },
                {"Prefix", model.getPrefix(), newInstance.getPrefix() },
                {"Suffix", model.getSuffix(), newInstance.getSuffix() },
                {"SelectedValue", model.getSelectedValue(), newInstance.getSelectedValue() },
                {"Size", model.getSize(), newInstance.getSize() }
        };
        for (Object[] attribute : attributes) {
            assertEquals((String)attribute[0], attribute[1], attribute[2]);
        }

        model.setSharedForAllSessionFiles(true);
        newInstance = model.getNewInstance(false);
        assertTrue("new instance for shared model is not the same",
                   (newInstance == model));

        newInstance = model.getNewInstance(true);
        assertTrue("new instance not returned when clone is required", 
                   (newInstance != model));
    }
}