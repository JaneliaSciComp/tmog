/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.field.StaticDataModel;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.util.HashMap;

/**
 * Tests the CompositeSetter class.
 *
 * @author Eric Trautman
 */
public class CompositeSetterTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public CompositeSetterTest(String name) {
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
        return new TestSuite(CompositeSetterTest.class);
    }

    /**
     * Tests the getValue method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetValue() throws Exception {
        StaticDataModel field1 = new StaticDataModel();
        field1.setName("field1");
        field1.setValue("value1");

        final FileTarget testTarget = new FileTarget(null);
        DataRow testDataRow = new DataRow(testTarget);
        testDataRow.addField(field1);

        final PluginDataRow testRow = new PluginDataRow(testDataRow);

        final HashMap<String, String> props = new HashMap<String, String>();
        final String compositeFieldString = "a${field1}b";
        final String expectedValue = "avalue1b";
        CompositeSetter setter = new CompositeSetter("propertyType",
                                         compositeFieldString,
                                         props);
        final String value = setter.getValue(testRow);
        assertEquals("incorrect value returned for '" +
                     compositeFieldString + "'",
                     expectedValue, value);
    }

}