/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.target.Target;

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
        String xmlFileName;
        File xmlDirectory = baseDirectory;
        if (targetFile == null) {
            final DataRow dataRow = row.getDataRow();
            final Target target = dataRow.getTarget();
            xmlFileName = target.getName() + ".xml";
            if (baseDirectory == null) {
                xmlDirectory = new File(".");
            }
        } else {
            xmlFileName = targetFile.getName() + ".xml";
            if (baseDirectory == null) {
                xmlDirectory = targetFile.getParentFile();
            }
        }
        return new File(xmlDirectory, xmlFileName);
    }

    @Override
    protected String getInitFailureMessage() {
        return "Failed to initialize XML writer plug-in.  ";
    }
}