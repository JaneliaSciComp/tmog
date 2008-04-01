/*
 * Copyright © 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.imagerenamer.field.SourceFileDefaultValue.MatchType;

import java.io.File;

/**
 * Tests the SourceFileDefaultValue class.
 *
 * @author Eric Trautman
 */
public class SourceFileDefaultValueTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public SourceFileDefaultValueTest(String name) {
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
        return new TestSuite(SourceFileDefaultValueTest.class);
    }

    /**
     * Tests the getValue method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetValue() throws Exception {

        File file = new File("./" +
                "a1-Gal4-UAS-MCD8-GFP-nc82-GFP-10-20-06.mdb/" +
                "a1-Gal4-UAS-MCD8-GFP-nc82-GFP-10-20-06-1a0.lsm");
        SourceFileDefaultValue defaultValue =
                new SourceFileDefaultValue(
                        "a1.*nc82-GFP-(\\d\\d?-\\d\\d?-\\d\\d?){1}.*\\.lsm",
                        MatchType.name);

        String value = defaultValue.getValue(file);
        assertEquals("invalid value for " + defaultValue + " and file " +
                     file.getAbsolutePath(),
                     "10-20-06", value);

        defaultValue = new SourceFileDefaultValue(".*\\.(mdb).*",
                                                  MatchType.name);

        value = defaultValue.getValue(file);
        assertNull("invalid value '" + value + "' for " + defaultValue +
                   " and file " + file.getAbsolutePath(),
                   value);

        defaultValue.setMatchType(MatchType.path.name());
        value = defaultValue.getValue(file);
        assertEquals("invalid value for " + defaultValue + " and file " +
                     file.getAbsolutePath(),
                     "mdb", value);

        file = new File ("./" +
                "CG9887-Gal4-2-1-CYO-UAS-MCD8-GFP-nc82-GFP-8-22-07.mdb/" +
                "CG9887-Gal4-2-1-CYO-UAS-MCD8-GFP-nc82-GFP-8-22-07.mdb/" +
                "CG9887-Gal4-8-22-07_L6_Sum.lsm");

        defaultValue = new SourceFileDefaultValue(".*mdb[/\\\\].*Gal4-(.*)-UAS.*mdb[/\\\\].*\\.lsm",
                                                  MatchType.path);
        value = defaultValue.getValue(file);
        assertEquals("invalid value for " + defaultValue + " and file " +
                     file.getAbsolutePath(),
                     "2-1-CYO", value);

        file = new File (".\\" +
                "CG9887-Gal4-2-1-CYO-UAS-MCD8-GFP-nc82-GFP-8-22-07.mdb\\" +
                "CG9887-Gal4-2-1-CYO-UAS-MCD8-GFP-nc82-GFP-8-22-07.mdb\\" +
                "CG9887-Gal4-8-22-07_L6_Sum.lsm");

        defaultValue = new SourceFileDefaultValue(".*mdb[/\\\\].*Gal4-(.*)-UAS.*mdb[/\\\\].*\\.lsm",
                                                  MatchType.path);
        value = defaultValue.getValue(file);
        assertEquals("invalid value for " + defaultValue + " and file " +
                     file.getAbsolutePath(),
                     "2-1-CYO", value);

    }
}