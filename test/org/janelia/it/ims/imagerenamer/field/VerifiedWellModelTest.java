/*
 * Copyright � 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the VerifiedWellModel class.
 *
 * @author Rob Svirskas
 */
public class VerifiedWellModelTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public VerifiedWellModelTest(String name) {
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
        return new TestSuite(VerifiedWellModelTest.class);
    }

    /**
     * Tests the setFormFactor method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testSetFormFactor() throws Exception {

        VerifiedWellModel well = new VerifiedWellModel();
        well.setFormFactor(96);
        assertEquals("Incorrect minimum X coordinate",new Integer(1),well.getMinimumX());
        assertEquals("Incorrect maximum X coordinate",new Integer(12),well.getMaximumX());
        assertEquals("Incorrect minimum Y coordinate",'A',well.getMinimumY());
        assertEquals("Incorrect maximum Y coordinate",'H',well.getMaximumY());
        well.setFormFactor(192);
        assertEquals("Incorrect minimum X coordinate",new Integer(1),well.getMinimumX());
        assertEquals("Incorrect maximum X coordinate",new Integer(24),well.getMaximumX());
        assertEquals("Incorrect minimum Y coordinate",'A',well.getMinimumY());
        assertEquals("Incorrect maximum Y coordinate",'H',well.getMaximumY());
        well.setFormFactor(384);
        assertEquals("Incorrect minimum X coordinate",new Integer(1),well.getMinimumX());
        assertEquals("Incorrect maximum X coordinate",new Integer(24),well.getMaximumX());
        assertEquals("Incorrect minimum Y coordinate",'A',well.getMinimumY());
        assertEquals("Incorrect maximum Y coordinate",'P',well.getMaximumY());

        try {
            well.setFormFactor(77);
            fail("Exception for illegal form factor expected");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

    }

    /**
     * Tests the verify method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testVerify() throws Exception {

        VerifiedWellModel well = new VerifiedWellModel();

        well.setFormFactor(96);

        well.setText("g07");
        boolean isValid = well.verify();
        assertTrue(well.getErrorMessage(),isValid);
        assertEquals("Well g07 not properly converted","G07",well.getFullText());

        well.setText("g7");
        isValid = well.verify();
        assertTrue(well.getErrorMessage(),isValid);
        assertEquals("Well g7 not properly converted","G07",well.getFullText());

        well.setText("A13");
        assertEquals("Exception for illegal well A13",false,well.verify());

        well.setText("A");
        assertEquals("Exception for illegal well A",false,well.verify());

        well.setText("A00");
        assertEquals("Exception for illegal well A00",false,well.verify());

        well.setText("22");
        assertEquals("Exception for illegal well 22",false,well.verify());

        well.setText("G012");
        assertEquals("Exception for illegal well G012",false,well.verify());        
    }
}
