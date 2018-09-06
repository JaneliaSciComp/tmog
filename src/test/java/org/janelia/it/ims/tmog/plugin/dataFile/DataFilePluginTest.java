/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.dataFile;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.PluginDataModel;
import org.janelia.it.ims.tmog.field.StaticDataModel;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.junit.Assert;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Tests the {@link DataFilePlugin} class.
 *
 * @author Eric Trautman
 */
public class DataFilePluginTest
        extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public DataFilePluginTest(String name) {
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
        return new TestSuite(DataFilePluginTest.class);
    }

    /**
     * Tests JAXB de-serialization.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testJAXB() throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(Data.class);
        Unmarshaller unm = ctx.createUnmarshaller();
        Object o = unm.unmarshal(new StringReader(XML_DATA));
        if (o instanceof Data) {
            Data data = (Data) o;
            Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter writer = new StringWriter();
            m.marshal(data, writer);
            StringBuffer sb = writer.getBuffer();
            String marshalledXml = sb.toString();
            Assert.assertEquals("input and output xml do not match",
                                XML_DATA, marshalledXml);
        } else {
            Assert.fail("returned an object of type " +
                        o.getClass().getName() + " instead of type " +
                        Data.class.getName());
        }
    }

    public void testPluginForXmlFile() throws Exception {
        writeDataAndTestPlugin("data-file-plugin-test.xml", XML_DATA);
    }

    public void testPluginForTsvFile() throws Exception {
        writeDataAndTestPlugin("data-file-plugin-test.tsv", TSV_DATA);
    }

    private void writeDataAndTestPlugin(String dataFileName,
                                        String data) throws Exception {
        File file = new File(dataFileName);
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(data);
            fileWriter.close();

            PluginDataModel line = new PluginDataModel();
            line.setDisplayName("Line");

            PluginDataModel age = new PluginDataModel();
            age.setDisplayName("Age");

            PluginConfiguration config = new PluginConfiguration();
            config.setProperty(DataFilePlugin.TMOG_ROW_KEY_PROPERTY_NAME,
                               "${Mount Date}_${Slide Number}");
            config.setProperty(DataFilePlugin.FILE_PROPERTY_NAME,
                               dataFileName);
            config.setProperty(DataFilePlugin.TSV_FILE_KEY_PROPERTY_NAME,
                               "slide");
            config.setProperty(line.getDisplayName(), "line");
            config.setProperty(age.getDisplayName(), "age");

            DataFilePlugin plugin = new DataFilePlugin();
            plugin.init(config);

            DataRow dataRow = new DataRow(new FileTarget(file));
            dataRow.addField(line);
            dataRow.addField(age);
            dataRow.addField(new StaticDataModel("Mount Date", "19991122"));
            dataRow.addField(new StaticDataModel("Slide Number", "77"));

            PluginDataRow pluginDataRow = new PluginDataRow(dataRow);

            plugin.updateRow(pluginDataRow);

            Assert.assertEquals("line not properly set",
                                "GMR_FOO_A", line.getValue());

            Assert.assertEquals("age not properly set",
                                "GMR_FOO_A", line.getValue());

        } finally {
            boolean deleteSucceeded = false;
            try {
                deleteSucceeded = file.delete();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            if (deleteSucceeded) {
                System.out.println("deleted " + file.getAbsolutePath());
            } else {
                System.out.println("WARNING: failed to delete " +
                                   file.getAbsolutePath());
            }

        }

    }

    private static final String XML_DATA =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<data>\n" +
            "    <item name=\"19991122_77\">\n" +
            "        <property name=\"line\">GMR_FOO_A</property>\n" +
            "        <property name=\"age\">E12</property>\n" +
            "        <property name=\"gender\">f</property>\n" +
            "    </item>\n" +
            "    <item name=\"19991122_88\">\n" +
            "        <property name=\"line\">GMR_BAR_B</property>\n" +
            "    </item>\n" +
            "</data>\n";

    private static final String TSV_DATA =
            "slide\tline\tage\tgender\n" +
            "19991122_77\tGMR_FOO_A\tE12\tf\n" +
            "19991122_88\tGMR_BAR_B\t\t\n";

}