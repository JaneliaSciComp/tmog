/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.chacrm;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import static org.janelia.it.chacrm.Transformant.Status;

/**
 * Tests the Transformant class.
 *
 * @author Eric Trautman
 */
public class TransformantTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public TransformantTest(String name) {
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
        return new TestSuite(TransformantTest.class);
    }

    /**
     * Tests the setStatus method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testSetStatus() throws Exception {

        Transformant transformant;

        Status[][] successfulTransitions = {
                { Status.transformant, Status.imaged },
                { Status.imaged, Status.imaged }
        };

        for (Status[] transition : successfulTransitions) {
            Status fromStatus = transition[0];
            Status toStatus = transition[1];
            transformant = new Transformant("test", fromStatus, -1);
            try {
                transformant.setStatus(toStatus);
            } catch (IllegalStateException e) {
                fail("Transition from " + fromStatus + " to " + toStatus +
                        " should have succeeded.  Exception was thrown with " +
                        "message: " + e.getMessage());
            }
        }

        Status[][] failedTransitions = {
                { null, Status.imaged },
                { Status.imaged, Status.transformant },
                { Status.imaged, null },
        };

        for (Status[] transition : failedTransitions) {
            Status fromStatus = transition[0];
            Status toStatus = transition[1];
            transformant = new Transformant("test", fromStatus, -1);
            try {
                transformant.setStatus(toStatus);
                fail("Transition from " + fromStatus + " to " + toStatus +
                     " should have failed.");
            } catch (IllegalStateException e) {
                assertTrue(true); // test passed!
            }
        }

    }
}
