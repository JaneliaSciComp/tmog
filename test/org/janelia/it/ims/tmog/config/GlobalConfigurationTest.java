/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */
package org.janelia.it.ims.tmog.config;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the GlobalConfiguration class.
 *
 * @author Eric Trautman
 */
public class GlobalConfigurationTest
        extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public GlobalConfigurationTest(String name) {
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
        return new TestSuite(GlobalConfigurationTest.class);
    }

    /**
     * Tests the verify method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testVerify() throws Exception {

        GlobalConfiguration config = new GlobalConfiguration();
        config.setMinimumVersion("2.2.2");

        String validVersions[] = {
                "2.2.2", "2.2.2.1", "2.2.3", "2.3", "3", "99.99.99.99"
        };

        for (String validVersion : validVersions) {
            try {
                config.verify(validVersion);
            } catch (ConfigurationException e) {
                fail("valid version '" + validVersion +
                     "' caused exception: " + e);
            }
        }

        String invalidVersions[] = {
                "2.2.1", "2.2.1.9", "2.1", "1", "abc", "5.5.5.5.5", "100.1"
        };

        for (String invalidVersion : invalidVersions) {
            try {
                config.verify(invalidVersion);
                fail("invalid version '" + invalidVersion +
                     "' did NOT cause exception");
            } catch (ConfigurationException e) {
                assertTrue(true); // test passed
            }
        }

    }
}