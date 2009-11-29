/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */
package org.janelia.it.ims.tmog.config.preferences;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;

/**
 * Tests the TransmogrifierPreferences class.
 *
 * @author Eric Trautman
 */
public class TransmogrifierPreferencesTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public TransmogrifierPreferencesTest(String name) {
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
        return new TestSuite(TransmogrifierPreferencesTest.class);
    }

    /**
     * Tests the load and toXml methods.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testLoadAndToXml() throws Exception {

        TransmogrifierPreferences prefs = new TransmogrifierPreferences();
        String validXml =
                "<transmogrifierPreferences>\n" +
                "  <projectPreferences name=\"testProject1\">\n" +
                "    <fieldDefaultSet name=\"testSetA\">\n" +
                "      <fieldDefault name=\"f1\">v1</fieldDefault>\n" +
                "      <fieldDefault name=\"f2\">v2</fieldDefault>\n" +
                "      <fieldDefault name=\"f3\">v3</fieldDefault>\n" +
                "    </fieldDefaultSet>\n" +
                "  </projectPreferences>\n" +
                "  <projectPreferences name=\"testProject2\">\n" +
                "    <fieldDefaultSet name=\"testSetB\">\n" +
                "      <fieldDefault name=\"f4\">v4</fieldDefault>\n" +
                "      <fieldDefault name=\"f5\">v5</fieldDefault>\n" +
                "      <fieldDefault name=\"f6\">v6</fieldDefault>\n" +
                "    </fieldDefaultSet>\n" +
                "  </projectPreferences>\n" +
                "</transmogrifierPreferences>";


        prefs.load(new ByteArrayInputStream(validXml.getBytes()));
        String actualXml = prefs.toXml();
        assertEquals("invalid xml returned", validXml, actualXml);
    }
}