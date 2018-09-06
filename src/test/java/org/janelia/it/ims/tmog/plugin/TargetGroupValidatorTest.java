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
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tests the {@link TargetGroupValidator} class.
 *
 * @author Eric Trautman
 */
public class TargetGroupValidatorTest
        extends TestCase {

    private TargetGroupValidator validator;

    private String groupNamePattern;
    private PluginConfiguration config;
    private String sessionName;

    private List<DataRow> allRows;

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public TargetGroupValidatorTest(String name) {
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
        return new TestSuite(TargetGroupValidatorTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        groupNamePattern = ".*[/\\\\](\\d{8}_\\d{6})[/\\\\][^/\\\\]+";
        final String[] requiredPatterns = new String[] {
                ".*\\.blob[s]?", ".*\\.png", ".*\\.set", ".*\\.summary"
        };

        final String[] groupNames = new String[] {
                "/tmp/19991122_001111/GMR_33B03_AE_01@UAS_ChRh2D@t6@100@r$095dc50$30s1x30s0s@r$095dc50$90s4x5s15s@n@n",
                "/tmp/19991122_002222/test-2@n",
                "/tmp/19991122_003333/test-3@t99@n@n@n"
        };

        config = new PluginConfiguration();
        config.setProperty(TargetGroupValidator.GROUP_NAME_PATTERN_PROPERTY,
                           groupNamePattern);
        for (int i = 0; i < requiredPatterns.length; i++) {
            config.setProperty(TargetGroupValidator.MEMBER_PATTERN_PROPERTY_PREFIX + i,
                               requiredPatterns[i]);
        }

        config.setProperty(TargetGroupValidator.USE_FULL_PATH_PROPERTY,
                           "true");

        final String windowsTestName = groupNames[0].replace("/", "\\") +
                                       "@_00000k.blobs";
        config.setProperty(TargetGroupValidator.TEST_PROPERTY,
                           windowsTestName);

        sessionName = "test-session";

        allRows = new ArrayList<DataRow>();

        String[] suffixList = new String[] {
                ".blobs", ".png", ".set", ".summary"
        };

        String fileName;
        for (String name : groupNames) {
            for (String suffix : suffixList) {
                fileName = name + suffix;
                allRows.add(new DataRow(new FileTarget(new File(fileName))));
            }
        }

        validator = new TargetGroupValidator();
        validator.init(config);
    }

    public void testValidateSuccesses() throws Exception {
        validator.startSessionValidation(sessionName, allRows);

        for (DataRow row : allRows) {
            validator.validate(sessionName, new PluginDataRow(row));
        }

        validator.stopSessionValidation(sessionName);
    }

    public void testValidateFailures() throws Exception {

        allRows.remove(allRows.size() - 1);

        validator.startSessionValidation(sessionName, allRows);

        validator.validate(sessionName,
                           new PluginDataRow(allRows.get(0)));

        verifyValidateException(sessionName,
                                allRows.size() - 1,
                                "match the following pattern:");

        sessionName = sessionName + "-2";
        allRows.remove(allRows.size() - 1);

        validator.startSessionValidation(sessionName, allRows);

        verifyValidateException(sessionName,
                                allRows.size() - 1,
                                "match the following patterns:");

        allRows.add(new DataRow(new FileTarget(new File("/tmp/foo.txt"))));

        verifyValidateException(sessionName,
                                allRows.size() - 1,
                                "Unable to derive group name");
    }

    public void testMissingGroupNamePattern() throws Exception {
        Map<String, String> props = config.getProperties();
        props.remove(TargetGroupValidator.GROUP_NAME_PATTERN_PROPERTY);
        verifyInitException(TargetGroupValidator.GROUP_NAME_PATTERN_PROPERTY);
    }

    public void testInvalidGroupNamePattern() throws Exception {
        config.setProperty(TargetGroupValidator.GROUP_NAME_PATTERN_PROPERTY,
                           "unclosedParen(");
        verifyInitException("could not be parsed");

        config.setProperty(TargetGroupValidator.GROUP_NAME_PATTERN_PROPERTY,
                           ".*");
        verifyInitException("must contain parentheses");
    }

    public void testMissingRequiredPatterns() throws Exception {
        Map<String, String> props = config.getProperties();
        props.clear();
        config.setProperty(TargetGroupValidator.GROUP_NAME_PATTERN_PROPERTY,
                           groupNamePattern);
        verifyInitException(TargetGroupValidator.MEMBER_PATTERN_PROPERTY_PREFIX);
    }

    public void testInvalidRequiredPattern() throws Exception {
        config.setProperty(TargetGroupValidator.MEMBER_PATTERN_PROPERTY_PREFIX + "99",
                           "unclosedParen(");
        verifyInitException("could not be parsed");
    }

    public void testMissingTestName() throws Exception {
        Map<String, String> props = config.getProperties();
        props.remove(TargetGroupValidator.TEST_PROPERTY);
        verifyInitException(TargetGroupValidator.TEST_PROPERTY);
    }

    public void testInvalidTestNameForGroupPattern() throws Exception {
        config.setProperty(TargetGroupValidator.TEST_PROPERTY,
                           "testNameWithoutAtSymbol.blobs");
        verifyInitException("does not match the");
    }

    public void testInvalidTestNameForRequiredPattern() throws Exception {
        config.setProperty(TargetGroupValidator.TEST_PROPERTY,
                           "/foo/bar/19990102_030405/test@without@valid@.suffix");
        verifyInitException("does not match any");
    }

    private void verifyInitException(String expectedMessageFragment) {
        try {
            validator.init(config);
            fail("init call should have failed");
        } catch (ExternalSystemException e) {
            String msg = e.getMessage();
            assertTrue("\nThe init exception message:\n     " + msg +
                       "\ndoes not contain '" + expectedMessageFragment + "'.",
                       msg.contains(expectedMessageFragment));
        }

    }

    private void verifyValidateException(String sessionName,
                                         int rowIndex,
                                         String expectedMessageFragment)
            throws ExternalSystemException {
        try {
            validator.validate(sessionName,
                               new PluginDataRow(allRows.get(rowIndex)));
            fail("validate call should have failed");
        } catch (ExternalDataException e) {
            String msg = e.getMessage();
            assertTrue("\nThe validate exception message:\n     " + msg +
                       "\ndoes not contain '" + expectedMessageFragment + "'.",
                       msg.contains(expectedMessageFragment));
        }
    }

}