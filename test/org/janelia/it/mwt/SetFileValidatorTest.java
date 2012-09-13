/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.mwt;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.StaticDataModel;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;

/**
 * Tests the {@link SetFileValidator} class.
 *
 * @author Eric Trautman
 */
public class SetFileValidatorTest
        extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public SetFileValidatorTest(String name) {
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
        return new TestSuite(SetFileValidatorTest.class);
    }

    /**
     * Tests the getBatchName method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testValidate() throws Exception {
        PluginConfiguration config = new PluginConfiguration();
        SetFileValidator validator = new SetFileValidator();
        validator.init(config);

        PluginDataRow row = getPluginDataRow(".summary",
                                             "45s1x30s0s",
                                             "105s10x0.2s10s");
        try {
            validator.validate(SESSION_NAME, row);
        } catch (Exception e) {
            e.printStackTrace();
            fail("unexpected exception thrown: " + e.getMessage());
        }

        row = getPluginDataRow("_000000k.blobs",
                               "45s1x30s0s",
                               "105s10x0.2s10s");
        try {
            // second call should use cache
            validator.validate(SESSION_NAME, row);
        } catch (Exception e) {
            e.printStackTrace();
            fail("unexpected exception thrown: " + e.getMessage());
        }

        row = getPluginDataRow(".summary",
                               "45s1x30s0s",
                               "bad-time-spec");
        try {
            validator.validate(SESSION_NAME, row);
            fail("exception should be thrown for bad time spec");
        } catch (Exception e) {
            // test passed
        }

        row = getPluginDataRow("bad-suffix",
                               "45s1x30s0s",
                               "105s10x0.2s10s");
        try {
            validator.validate(SESSION_NAME, row);
            fail("exception should be thrown for bad file name");
        } catch (Exception e) {
            // test passed
        }

    }

    private RenamePluginDataRow getPluginDataRow(String fromFileSuffix,
                                                 String stimulus1Times,
                                                 String stimulus2Times) {
        final String baseFilePath =
                "test/org/janelia/it/mwt/p_45s1x30s0s#p_105s10x0.2s10s";
        File fromFile = new File(baseFilePath + fromFileSuffix);
        DataRow dataRow = new DataRow(new FileTarget(fromFile));
        StaticDataModel field = new StaticDataModel();
        field.setName("Stimulus 1 Times");
        field.setValue(stimulus1Times);
        dataRow.addField(field);

        field = new StaticDataModel();
        field.setName("Stimulus 2 Times");
        field.setValue(stimulus2Times);
        dataRow.addField(field);

        return new RenamePluginDataRow(fromFile,
                                       dataRow,
                                       new File("outDirectory"));
    }

    private static final String SESSION_NAME = "test-session";
}