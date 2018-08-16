/*
 * Copyright (c) 2018 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.plugin.imagedb.ImageDataDefaultValue;
import org.janelia.it.ims.tmog.plugin.imagedb.SageImageDataDefaultValue;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests the ImageDataDefaultValue class.
 *
 * @author Eric Trautman
 */
public class ImageDataDefaultValueTest
        extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public ImageDataDefaultValueTest(String name) {
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
        return new TestSuite(ImageDataDefaultValueTest.class);
    }

    /**
     * Tests the getValue method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetValue() throws Exception {

        ImageDataDefaultValue dataDefault = new ImageDataDefaultValue();
        Map<String, String> properties = new HashMap<>();
        properties.put("family", "flylight_polarity");
        properties.put("image_property", "effector");
        dataDefault.init(properties);

        final File lsmFile = new File(
                "20121110",
                "FLPO_20121114222136397_1.lsm");
        final FileTarget target = new FileTarget(lsmFile);

        String value = dataDefault.getValue(target);
        assertEquals("invalid value",
                     "TLN-V5_myr-FLAG_Syt-HA_23_0037", value);

        properties.put("image_property", "gender");
        ImageDataDefaultValue dataDefault2 = new ImageDataDefaultValue();
        dataDefault2.init(properties);

        value = dataDefault2.getValue(target);
        assertEquals("invalid value",
                     "f", value);
    }

    /**
     * Tests the getValue method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetSageValue() throws Exception {

        SageImageDataDefaultValue dataDefault = new SageImageDataDefaultValue();
        Map<String, String> properties = new HashMap<>();
        properties.put("family", "baker_lab");
        properties.put("image_property", "short_genotype");
        dataDefault.init(properties);

        final File lsmFile = new File(
                "poxn-Gal4_14-1-7",
                "BSB_poxn-Gal4_14-1-7__dsxnull__fv-A-84-44_20081120151144498.lsm");
        final FileTarget target = new FileTarget(lsmFile);

        String value = dataDefault.getValue(target);
        assertEquals("invalid value",
                     "dsxnull", value);
    }

}