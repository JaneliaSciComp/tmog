/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.StaticDataModel;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;

/**
 * Tests the CrossFieldValidator class.
 *
 * @author Eric Trautman
 */
public class CrossFieldValidatorTest
        extends TestCase {

    private String fieldName;
    private String referenceFieldName;

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public CrossFieldValidatorTest(String name) {
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
        return new TestSuite(CrossFieldValidatorTest.class);
    }

    /**
     * Tests the parseTokens method with valid tokens.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testValidate() throws Exception {

        CrossFieldValidator validator = new CrossFieldValidator();

        PluginConfiguration config = new PluginConfiguration();
        fieldName = "Gender";
        config.setProperty(CrossFieldValidator.VALIDATE_FIELD_NAME,
                           fieldName);
        config.setProperty(CrossFieldValidator.MATCHES_PATTERN_NAME,
                           "\\S+");
        referenceFieldName = "Age";
        config.setProperty(CrossFieldValidator.WHEN_REFERENCE_FIELD_NAME,
                           referenceFieldName);
        config.setProperty(CrossFieldValidator.MATCHES_REFERENCE_PATTERN_NAME,
                           "^[^L].*");
        config.setProperty(CrossFieldValidator.ERROR_MESSAGE_NAME,
                           "Gender value is required when age is ${Age}.");

        validator.init(config);

        PluginDataRow row = getRow("L01", null);

        try {
            validator.validate(SESSION_NAME, row);
        } catch (Exception e) {
            fail("non-matching reference without field value failed, " +
                 "error message is " + e.getMessage());
        }

        row = getRow("L01", "F");

        try {
            validator.validate(SESSION_NAME, row);
        } catch (Exception e) {
            fail("non-matching reference with field value failed, " +
                 "error message is " + e.getMessage());
        }

        row = getRow("A01", null);

        try {
            validator.validate(SESSION_NAME, row);
            fail("matching reference without field value passed");
        } catch (Exception e) {
            // test passed!
        }

        row = getRow("A01", "F");

        try {
            validator.validate(SESSION_NAME, row);
        } catch (Exception e) {
            fail("matching reference with field value failed, " +
                 "error message is " + e.getMessage());
        }

    }

    private PluginDataRow getRow(String referenceFieldValue,
                                 String fieldValue) {
        DataRow dataRow = new DataRow(new FileTarget(new File("test-target")));
        dataRow.addField(new StaticDataModel(referenceFieldName,
                                             referenceFieldValue));
        dataRow.addField(new StaticDataModel(fieldName,
                                             fieldValue));
        return new PluginDataRow(dataRow);
    }

    private static final String SESSION_NAME = "test-session";
}