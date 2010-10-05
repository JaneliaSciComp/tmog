/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
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
     * Tests the validate method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testValidate() throws Exception {

        final PluginConfiguration pluginConfig = new PluginConfiguration();
        ChacrmLineValidator validator = new ChacrmLineValidator();
        validator.init(pluginConfig);

        VerifiedTextModel field = new VerifiedTextModel();
        field.setDisplayName("Line");
        field.setText("GMR_15E08_AE_01");
        DataRow dataRow = new DataRow(new FileTarget(new File("foo")));
        dataRow.addField(field);
        RenamePluginDataRow row = new RenamePluginDataRow(new File("bar"),
                                                          dataRow,
                                                          new File("."));

        final long startTime = System.currentTimeMillis();

        for (int i = 0; i < 3000; i++) {
            validator.validate(row);
            if ((System.currentTimeMillis() - startTime) > 5000) {
                fail("Cached transformant id checks are taking too long.");
            }
        }

        field.setText("GMR_Bad_Name");
        try {
            validator.validate(row);
            fail("invalid transformant id should have caused data exception");
        } catch (ExternalDataException e) {
            // test passed!
        }
    }

    /**
     * Tests the validate method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testValidateWithAlternatePrefix() throws Exception {

        final PluginConfiguration pluginConfig = new PluginConfiguration();
        pluginConfig.setProperty(
                ChacrmLineValidator.CHACRM_LINE_PREFIX_PROPERTY_NAME, "GL_");
        ChacrmLineValidator validator = new ChacrmLineValidator();
        validator.init(pluginConfig);

        VerifiedTextModel field = new VerifiedTextModel();
        field.setDisplayName("Line");
        field.setText("GL_15E08_AE_01");
        DataRow dataRow = new DataRow(new FileTarget(new File("foo")));
        dataRow.addField(field);
        RenamePluginDataRow row = new RenamePluginDataRow(new File("bar"),
                                                          dataRow,
                                                          new File("."));

        validator.validate(row);
        // row should be valid (no exception thrown)
        
        field.setText("GL_Bad_Name");
        try {
            validator.validate(row);
            fail("invalid transformant id should have caused data exception");
        } catch (ExternalDataException e) {
            // test passed!
        }
    }

}