/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.utils.PathUtil;
import org.janelia.it.utils.StringUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * This class supports writing image properties to an XML file.
 *
 * @author Eric Trautman
 */
public class ImagePropertyFileWriter implements ImagePropertyWriter {

    private File baseDirectory;

    public ImagePropertyFileWriter(String baseDirectoryName) {
        this.baseDirectory = new File(PathUtil.convertPath(baseDirectoryName));
    }

    /**
     * Verifies that the writer is available.
     *
     * @throws ExternalSystemException
     *   if this writer cannot be used.
     */
    public void checkAvailability()
            throws ExternalSystemException {

        if (! this.baseDirectory.exists()) {
            throw new ExternalSystemException(
                    "The configured base directory for XML image property " +
                    "files (" + this.baseDirectory.getAbsolutePath() +
                    ") does not exist.");
        }

        if (! this.baseDirectory.canWrite()) {
            throw new ExternalSystemException(
                    "You do not have access to write to the configured base " +
                    "directory for XML image property files (" +
                    this.baseDirectory.getAbsolutePath() + ").");
        }
    }

    /**
     * Writes (saves) the specified image properties to an XML file.
     * 
     * @param  image  image to be persisted.
     *
     * @return the specified image (unmodified).
     *
     * @throws ExternalSystemException
     *   if the write fails.
     */
    public Image saveProperties(Image image) throws ExternalSystemException {
        String relativePath = image.getRelativePath();

        File dataFile = new File(baseDirectory, relativePath + ".xml");
        File dataParentDirectory = dataFile.getParentFile();
        if (dataParentDirectory != null) {
            try {
                dataParentDirectory.mkdirs();
            } catch (Throwable t) {
                throw new ExternalSystemException(
                        "Failed to create directory for image properties: " +
                        dataParentDirectory.getAbsolutePath() +
                        ".");
            }
        }

        String xmlData = getXmlForImageProperties(image);

        FileWriter writer = null;
        try {
            writer = new FileWriter(dataFile);
            writer.write(xmlData);
        } catch (Throwable t) {
            throw new ExternalSystemException(
                    "Failed to write image properties to " +
                    dataFile.getAbsolutePath() + ".", t);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOG.warn("ignoring exception: " +
                             "failed to close image property file", e);
                }
            }
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("successfully persisted image properties to " +
                     dataFile.getAbsolutePath());
        }

        return image;
    }

    private String getXmlForImageProperties(Image image) {
        StringBuilder xmlData = new StringBuilder(1024);
        xmlData.append("<imageData relativePath=\"");
        xmlData.append(image.getRelativePath());
        Date captureDate = image.getCaptureDate();
        if (captureDate != null) {
            xmlData.append("\" captureDate=\"");
            xmlData.append(CAPTURE_DATE_FMT.format(image.getCaptureDate()));
        }
        xmlData.append("\" family=\"");
        xmlData.append(StringUtil.getDefinedXmlValue(image.getFamily()));
        xmlData.append("\">");
        xmlData.append(StringUtil.LINE_SEPARATOR);

        Map<String, String> properties = image.getPropertyTypeToValueMap();
        String value;
        for (String type : properties.keySet()) {
            value = properties.get(type);
            if (StringUtil.isDefined(value)) {
                xmlData.append("  <property name=\"");
                xmlData.append(StringUtil.getDefinedXmlValue(type));
                xmlData.append("\">");
                xmlData.append(StringUtil.getDefinedXmlValue(value));
                xmlData.append("</property>");
                xmlData.append(StringUtil.LINE_SEPARATOR);
            }
        }

        xmlData.append("</imageData>");
        xmlData.append(StringUtil.LINE_SEPARATOR);
        return xmlData.toString();
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(ImagePropertyFileWriter.class);

    private static final SimpleDateFormat CAPTURE_DATE_FMT =
            new SimpleDateFormat("yyyy-MM-dd");
}