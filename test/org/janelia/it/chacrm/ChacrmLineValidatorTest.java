/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.chacrm;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.VerifiedTextModel;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;

/**
 * Tests the ChacrmLineValidator class.
 *
 * @author Eric Trautman
 */
public class ChacrmLineValidatorTest
        extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public ChacrmLineValidatorTest(String name) {
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
        return new TestSuite(ChacrmLineValidatorTest.class);
    }

    /**
     * Tests the validate method with a default line prefix.
     * Also verifies that caching is working.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testDefaultValidateAndCaching() throws Exception {
        verifyValidAndInvalidLineNames(null, 3000);
    }

    /**
     * Tests the validate method with an alternate line prefix.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testAlternatePrefixValidate() throws Exception {
        verifyValidAndInvalidLineNames("GL_", 1);
    }

    /**
     * Tests the validate method with an empty line prefix.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testEmptyPrefixValidate() throws Exception {
        verifyValidAndInvalidLineNames("", 1);
    }

    /**
     * Tests the validate method using transformant component fields
     * (plate, well, ...) instead of a single line name field.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testTransformantComponentFieldsValidate()
            throws Exception {

        PluginConfiguration pluginConfig = new PluginConfiguration();
        ChacrmLineValidator validator = new ChacrmLineValidator();
        validator.init(pluginConfig);

        DataRow dataRow = new DataRow(new FileTarget(new File("foo")));
        // see ChacrmEventManager.getTransformantID()
        String[][] fieldData = {
                {"Plate", PLATE},
                {"Well", WELL},
                {"Vector ID", VECTOR},
                {"Landing Site", INSERTION_SITE}
        };
        for (String[] data : fieldData) {
            VerifiedTextModel field = new VerifiedTextModel();
            field.setDisplayName(data[0]);
            field.setText(data[1]);
            dataRow.addField(field);
        }

        RenamePluginDataRow row = new RenamePluginDataRow(new File("bar"),
                                                          dataRow,
                                                          new File("."));

        validator.validate(SESSION_NAME, row);
        // row should be valid (no exception thrown)

        VerifiedTextModel field = (VerifiedTextModel) dataRow.getField(0);
        field.setText(INVALID_LINE_NAME);
        try {
            validator.validate(SESSION_NAME, row);
            fail("invalid plate name should have caused data exception");
        } catch (ExternalDataException e) {
            // test passed!
        }
    }

    private void verifyValidAndInvalidLineNames(String linePrefix,
                                                int numberOfValidChecks)
            throws Exception {

        final String lineFieldPropertyName = "Line";
        PluginConfiguration pluginConfig = new PluginConfiguration();
        pluginConfig.setProperty(
                ChacrmLineValidator.LINE_FIELD_PROPERTY_NAME,
                lineFieldPropertyName);
        if (linePrefix != null) {
            pluginConfig.setProperty(
                    ChacrmLineValidator.LINE_PREFIX_PROPERTY_NAME,
                    linePrefix);
        }
        ChacrmLineValidator validator = new ChacrmLineValidator();
        validator.init(pluginConfig);

        if (linePrefix == null) {
            linePrefix = ChacrmLineValidator.DEFAULT_LINE_PREFIX;
        }

        VerifiedTextModel field = new VerifiedTextModel();
        field.setDisplayName(lineFieldPropertyName);
        field.setText(linePrefix + VALID_LINE_NAME);
        DataRow dataRow = new DataRow(new FileTarget(new File("foo")));
        dataRow.addField(field);
        RenamePluginDataRow row = new RenamePluginDataRow(new File("bar"),
                                                          dataRow,
                                                          new File("."));

        long startTime;
        for (int i = 0; i < numberOfValidChecks; i++) {
            startTime = System.currentTimeMillis();
            validator.validate(SESSION_NAME, row);
            // row should be valid (no exception thrown)
            if ((i > 0 ) && ((System.currentTimeMillis() - startTime) > 1000)) {
                fail("Cached transformant id checks are taking too long.");
            }
        }

        String invalidLineName = linePrefix + INVALID_LINE_NAME;
        field.setText(invalidLineName);
        try {
            validator.validate(SESSION_NAME, row);
            fail("'" + invalidLineName + "' should have caused data exception");
        } catch (ExternalDataException e) {
            // test passed!
        }
    }

    private static final String PLATE = "15";
    private static final String WELL = "E08";
    private static final String VECTOR = "AE";
    private static final String INSERTION_SITE = "01";
    private static final String VALID_LINE_NAME =
            PLATE + WELL + '_' + VECTOR + '_' + INSERTION_SITE;
    private static final String INVALID_LINE_NAME = "Invalid-Line";
    private static final String SESSION_NAME = "test-session";

}