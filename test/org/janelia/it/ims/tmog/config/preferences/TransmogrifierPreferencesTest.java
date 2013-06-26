/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
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

        TransmogrifierPreferences preferences = new TransmogrifierPreferences();
        String validXml =
                "<transmogrifierPreferences>\n" +
                "  <projectPreferences name=\"tmog-global-preferences\">\n" +
                "  </projectPreferences>\n" +
                "  <projectPreferences name=\"testProject1\">\n" +
                "    <fieldDefaultSet name=\"testSetA\">\n" +
                "      <fieldDefault name=\"f1\">v1</fieldDefault>\n" +
                "      <fieldDefault name=\"f2\">v2</fieldDefault>\n" +
                "      <fieldDefault name=\"f3\">v3</fieldDefault>\n" +
                "    </fieldDefaultSet>\n" +
                "    <viewDefault name=\"current\">\n" +
                "      <pathDefault name=\"source\">/tmp/source</pathDefault>\n" +
                "      <pathDefault name=\"target\">/tmp/target</pathDefault>\n" +
                "      <columnDefault name=\"c1\" width=\"10\"/>\n" +
                "      <columnDefault name=\"c2\" width=\"20\"/>\n" +
                "      <columnDefault name=\"c3\" width=\"30\">\n" +
                "        <columnDefault name=\"c31\" width=\"31\"/>\n" +
                "        <columnDefault name=\"c32\" width=\"32\"/>\n" +
                "        <columnDefault name=\"c33\" width=\"33\"/>\n" +
                "      </columnDefault>\n" +
                "    </viewDefault>\n" +
                "    <viewDefault name=\"old\">\n" +
                "      <columnDefault name=\"c1\" width=\"10\"/>\n" +
                "    </viewDefault>\n" +
                "  </projectPreferences>\n" +
                "  <projectPreferences name=\"testProject2\">\n" +
                "    <fieldDefaultSet name=\"testSetB\">\n" +
                "      <fieldDefault name=\"f4\">v4</fieldDefault>\n" +
                "    </fieldDefaultSet>\n" +
                "    <fieldDefaultSet name=\"testSetC\">\n" +
                "      <fieldDefault name=\"f5\">v5</fieldDefault>\n" +
                "      <fieldDefaultSet name=\"groupOne\">\n" +
                "        <fieldDefaultSet name=\"1\">\n" +
                "          <fieldDefault name=\"f6\">v61</fieldDefault>\n" +
                "          <fieldDefault name=\"f7\">v71</fieldDefault>\n" +
                "        </fieldDefaultSet>\n" +
                "        <fieldDefaultSet name=\"2\">\n" +
                "          <fieldDefault name=\"f6\">v62</fieldDefault>\n" +
                "          <fieldDefault name=\"f7\">v72</fieldDefault>\n" +
                "        </fieldDefaultSet>\n" +
                "      </fieldDefaultSet>\n" +
                "    </fieldDefaultSet>\n" +
                "  </projectPreferences>\n" +
                "</transmogrifierPreferences>\n";

        preferences.load(new ByteArrayInputStream(validXml.getBytes()));
        String actualXml = preferences.toXml();
        assertEquals("invalid xml returned", validXml, actualXml);

        validXml =
                "<transmogrifierPreferences>\n" +
                "  <projectPreferences name=\"tmog-global-preferences\">\n" +
                "  </projectPreferences>\n" +
                "</transmogrifierPreferences>\n";

        preferences = new TransmogrifierPreferences();
        preferences.load(new ByteArrayInputStream(validXml.getBytes()));
        actualXml = preferences.toXml();
        assertEquals("invalid xml returned for empty object",
                     validXml, actualXml);
    }
}