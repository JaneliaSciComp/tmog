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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowListener;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Tests the {@link SageLoaderPlugin} class.
 *
 * @author Eric Trautman
 */
public class SageLoaderPluginTest
        extends TestCase {

    private static final Log LOG =
            LogFactory.getLog(SageLoaderPluginTest.class);

    private static final String BASE_URL = "http://trautmane-ws1:8180/rest-v1";

    private SageLoaderPlugin plugin = new SageLoaderPlugin();

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public SageLoaderPluginTest(String name) {
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
        return new TestSuite(SageLoaderPluginTest.class);
    }

    /**
     * Tests the {@link SageLoaderPlugin#getStatusLink} method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetStatusLink() throws Exception {

        final String expectedHref = "http://server/test/123/checkStatus";
        final String statusXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<currentTaskStatus>\n" +
                "    <taskId>1593757153544372426</taskId>\n" +
                "    <lastEventType>test type</lastEventType>\n" +
                "    <lastEventDescription>test description</lastEventDescription>\n" +
                "    <lastEventTimestamp>2011/04/01 14:08:37</lastEventTimestamp>\n" +
                "    <href>" + expectedHref + "</href>\n" +
                "</currentTaskStatus>";
        InputStream statusXmlStream =
                new ByteArrayInputStream(statusXml.getBytes());
        final String statusLink = plugin.getStatusLink(statusXmlStream);
        assertEquals("invalid status link parsed",
                     expectedHref, statusLink);
    }

    /**
     * Tests the {@link SageLoaderPlugin#init} method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testInit() throws Exception {
        plugin.init(getConfig());
    }

    /**
     * Tests the {@link SageLoaderPlugin#init} method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testInitWithoutOwnerName() throws Exception {
        PluginConfiguration config = getConfig();
        config.removeProperty(SageLoaderPlugin.OWNER_PARAMETER_NAME);
        plugin.init(config);
    }

    /**
     * Tests the {@link SageLoaderPlugin#init} method with
     * bad configuration parameters.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testBadConfigurations() throws Exception {
        PluginConfiguration config = getConfig();

        ArrayList<String> parametersToRemove = new ArrayList<String>();
        parametersToRemove.add(SageLoaderPlugin.TEST_URL_NAME);
        for (int i = SageLoaderPlugin.REQUIRED_QUERY_PARAMETERS.length; i > 0; i--) {
            parametersToRemove.add(SageLoaderPlugin.REQUIRED_QUERY_PARAMETERS[i-1]);
        }
        parametersToRemove.add(SageLoaderPlugin.SERVICE_URL_NAME);

        for (String removedParameter : parametersToRemove) {
            config.removeProperty(removedParameter);
            try {
                plugin.init(config);
                fail("missing " + removedParameter +
                     " should have caused failure");
            } catch (ExternalSystemException e) {
                final String msg = e.getMessage();
                LOG.info("init failure message is: " + msg);
                assertTrue("message missing parameter name '" +
                           removedParameter + "'",
                           msg.contains(removedParameter));
            }
        }

        config = getConfig();
        final String[] badDepthValues = {"a", "-2"};
        for (String badValue : badDepthValues) {
            config.setProperty(SageLoaderPlugin.RELATIVE_PATH_DEPTH_NAME,
                               badValue);
            try {
                plugin.init(config);
                fail("bad depth value '" + badValue +
                     "' should have caused failure");
            } catch (ExternalSystemException e) {
                final String msg = e.getMessage();
                LOG.info("init failure message is: " + msg);
                // test passed
            }
        }
    }

    /**
     * Tests the {@link SageLoaderPlugin#processEvent} method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testProcessEvent() throws Exception {

        plugin.init(getConfig());

        final File testFile = new File(
                "/test/confocalStacks/20110407/FLFL_20110911230347810_1535.lsm");
        final FileTarget testTarget = new FileTarget(testFile);
        DataRow testDataRow = new DataRow(testTarget);
        final PluginDataRow testRow = new PluginDataRow(testDataRow);

        plugin.processEvent(RowListener.EventType.END_ROW_SUCCESS,
                            testRow);
    }

    private PluginConfiguration getConfig() {
        PluginConfiguration config = new PluginConfiguration();
        config.setProperty(SageLoaderPlugin.SERVICE_URL_NAME,
                           BASE_URL + "/sageLoader");
        config.setProperty(SageLoaderPlugin.OWNER_PARAMETER_NAME,
                           "system");
        config.setProperty(SageLoaderPlugin.CONFIG_PARAMETER_NAME,
                           "/groups/scicomp/informatics/data/flylightflip_light_imagery-config.xml");
        config.setProperty(SageLoaderPlugin.GRAMMAR_PARAMETER_NAME,
                           "/groups/jacs/jacsDev/informatics/grammar/flylightflip.gra");
        config.setProperty(SageLoaderPlugin.LAB_PARAMETER_NAME,
                           "flylight");
        config.setProperty(SageLoaderPlugin.TEST_URL_NAME,
                           BASE_URL + "/task/1593757153544372426/currentStatus");
        return config;
    }
}