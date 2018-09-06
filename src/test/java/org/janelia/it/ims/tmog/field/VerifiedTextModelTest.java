/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the VerifiedTextModel class.
 *
 * @author Eric Trautman
 */
public class VerifiedTextModelTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public VerifiedTextModelTest(String name) {
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
        return new TestSuite(VerifiedTextModelTest.class);
    }

    /**
     * Tests the getFileNameValue method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetFileNameValue() throws Exception {
        VerifiedTextModel model = new VerifiedTextModel();
        String basicText = "text";
        model.setText(basicText);
        String fileNameValue = model.getFileNameValue();
        assertEquals("invalid value returned for basic text",
                     basicText, fileNameValue);

        String prefix = "pre";
        model.setPrefix(prefix);
        fileNameValue = model.getFileNameValue();
        String expectedValue = prefix + basicText;
        assertEquals("invalid value returned for text with prefix",
                     expectedValue, fileNameValue);

        String suffix = "post";
        model.setPrefix(null);
        model.setSuffix(suffix);
        fileNameValue = model.getFileNameValue();
        expectedValue = basicText + suffix;
        assertEquals("invalid value returned for text with suffix",
                     expectedValue, fileNameValue);

        model.setPrefix(prefix);
        fileNameValue = model.getFileNameValue();
        expectedValue = prefix + basicText + suffix;
        assertEquals("invalid value returned for text with prefix and suffix",
                     expectedValue, fileNameValue);

        model.setText(null);
        fileNameValue = model.getFileNameValue();
        expectedValue = "";
        assertEquals("invalid value returned for null text with prefix and suffix",
                     expectedValue, fileNameValue);

        model.setText("");
        fileNameValue = model.getFileNameValue();
        expectedValue = "";
        assertEquals("invalid value returned for empty text with prefix and suffix",
                     expectedValue, fileNameValue);

    }

    /**
     * Tests the getNewInstance method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetNewInstance() throws Exception {
        VerifiedTextModel model = new VerifiedTextModel();
        model.setText("text");
        model.setPrefix("pre");
        model.setSuffix("post");
        model.setMinimumLength(2);
        model.setMaximumLength(3);
        model.setPattern("pattern");
        model.setPatternMatchFailureMessage("pattern does not match");
        model.setConvertToUpperCase(true);

        VerifiedTextModel newInstance = model.getNewInstance(false);

        Object[][] attributes = {
                {"DisplayName", model.getDisplayName(), newInstance.getDisplayName() },
                {"Required", model.isRequired(), newInstance.isRequired() },
                {"Prefix", model.getPrefix(), newInstance.getPrefix() },
                {"Suffix", model.getSuffix(), newInstance.getSuffix() },
                {"MinimumLength", model.getMinimumLength(), newInstance.getMinimumLength() },
                {"MaximumLength", model.getMaximumLength(), newInstance.getMaximumLength() },
                {"Pattern", model.getPattern(), newInstance.getPattern() },
                {"Pattern", model.getPatternMatchFailureMessage(), newInstance.getPatternMatchFailureMessage() },
                {"ConvertToUpperCase", model.getConvertToUpperCase(), newInstance.getConvertToUpperCase() },
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