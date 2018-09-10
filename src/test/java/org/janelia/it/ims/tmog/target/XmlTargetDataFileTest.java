/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.target;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * Tests the {@link XmlTargetDataFile} class.
 *
 * @author Eric Trautman
 */
public class XmlTargetDataFileTest
        extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public XmlTargetDataFileTest(String name) {
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
        return new TestSuite(XmlTargetDataFileTest.class);
    }

    /**
     * Tests the getTargets method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetTargets() throws Exception {

        XmlTargetDataFile inputDataFile = new XmlTargetDataFile();
        
        inputDataFile.setTargetGroupPath("samples/sample");
        inputDataFile.addRelativeGroupPropertyPath("lineName");
        inputDataFile.addRelativeGroupPropertyPath("gender");
        inputDataFile.setRelativeTargetPath("lsm");
        inputDataFile.setRelativeTargetNamePath("fileName");
        inputDataFile.addRelativeTargetPropertyPath("laminaPresent");
        inputDataFile.addRelativeTargetPropertyPath("gender");

        final String xml =
                "<samples>\n" +
                "  <sample>\n" +
                "    <lineName><![CDATA[line1]]></lineName>\n" +
                "    <gender><![CDATA[female]]></gender>\n" +
                "    <lsm>\n" +
                "      <fileName><![CDATA[settings.gradle]]></fileName>\n" +
                "      <laminaPresent><![CDATA[true]]></laminaPresent>\n" +
                "    </lsm>\n" +
                "    <lsm>\n" +
                "      <fileName>file-does-not-exist</fileName>\n" +
                "    </lsm>\n" +
                "    <lsm>\n" +
                "      <fileName>build.gradle</fileName>\n" +
                "      <gender>male</gender>\n" +
                "    </lsm>\n" +
                "  </sample>\n" +
                "  <sample>\n" +
                "    <lineName>line2</lineName>\n" +
                "    <gender>male</gender>\n" +
                "    <lsm>\n" +
                "      <fileName>settings.gradle</fileName>\n" +
                "      <laminaPresent>false</laminaPresent>\n" +
                "    </lsm>\n" +
                "  </sample>\n" +
                "</samples>";

        final ByteArrayInputStream stream =
                new ByteArrayInputStream(xml.getBytes());

        TargetList wrapper = inputDataFile.getTargets(stream);
        List<FileTarget> targetList = wrapper.getList();
        assertEquals("incorrect number of targets parsed",
                     3, targetList.size());

        // --------------------------------------------------
        Target target = targetList.get(0);
        assertEquals("first returned target has invalid class",
                     FileTarget.class, target.getClass());

        FileTarget fileTarget = (FileTarget) target;
        String[][] testData = {
                {"lineName", "line1"},
                {"gender", "female"},
                {"laminaPresent", "true"}
        };

        for (String[] test : testData) {
            assertEquals("first target returned invalid value for '" +
                         test[0] + "' property",
                         test[1], fileTarget.getProperty(test[0]));
        }

        // --------------------------------------------------
        target = targetList.get(1);
        assertEquals("second returned target has invalid class",
                     FileTarget.class, target.getClass());

        fileTarget = (FileTarget) target;
        testData = new String[][] {
                {"lineName", "line1"},
                {"gender", "male"}
        };

        for (String[] test : testData) {
            assertEquals("second target returned invalid value for '" +
                         test[0] + "' property",
                         test[1], fileTarget.getProperty(test[0]));
        }

        // --------------------------------------------------
        target = targetList.get(2);
        assertEquals("third returned target has invalid class",
                     FileTarget.class, target.getClass());

        fileTarget = (FileTarget) target;
        testData = new String[][] {
                {"lineName", "line2"},
                {"gender", "male"},
                {"laminaPresent", "false"}
        };

        for (String[] test : testData) {
            assertEquals("third target returned invalid value for '" +
                         test[0] + "' property",
                         test[1], fileTarget.getProperty(test[0]));
        }

    }
}