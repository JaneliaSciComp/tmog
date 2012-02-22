/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import java.io.File;

/**
 * This plug-in writes transmogrifier row data to an XML file.
 *
 * @author Eric Trautman
 */
public class XmlWriterPlugin extends RowWriterPlugin {

    @Override
    protected String getRowRepresentation(PluginDataRow row) {
        XmlStringBuilder xml = new XmlStringBuilder("row");
        xml.setRow(row, null);
        return xml.toString();
    }
           
    @Override
    protected File getFile(PluginDataRow row,
                           File baseDirectory) {
        final File targetFile = row.getTargetFile();
        final String xmlFileName = targetFile.getName() + ".xml";
        File xmlDirectory;
        if (baseDirectory != null) {
            xmlDirectory = baseDirectory;
        } else {
            xmlDirectory = targetFile.getParentFile();
        }
        return new File(xmlDirectory, xmlFileName);
    }

    @Override
    protected String getInitFailureMessage() {
        return "Failed to initialize XML writer plug-in.  ";
    }
}