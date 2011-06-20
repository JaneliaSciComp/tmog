/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import loci.common.RandomAccessInputStream;
import loci.common.RandomAccessOutputStream;
import loci.formats.FormatException;
import loci.formats.tiff.IFD;
import loci.formats.tiff.TiffConstants;
import loci.formats.tiff.TiffIFDEntry;
import loci.formats.tiff.TiffParser;
import loci.formats.tiff.TiffSaver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.config.PluginConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This plug-in embeds an xml representation of each row's data fields into
 * the source LSM file.
 *
 * @author Eric Trautman
 */
public class LsmMetaDataPlugin
        implements RowListener {

    /** Name of the root xml element for each row. */
    private String rootElement;

    /**
     * Map of element names to aggregate tokens for additional data elements
     * to be included for each row.
     */
    private Map<String, PropertyTokenList> additionalData;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public LsmMetaDataPlugin() {
        this.additionalData = new LinkedHashMap<String, PropertyTokenList>();
    }
    
    /**
     * Verifies that the plugin is ready for use by checking external
     * dependencies.
     *
     * @param  config  the plugin configuration.
     *
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config) throws ExternalSystemException {

        final Map<String, String> props = config.getProperties();

        this.rootElement = props.remove("rootElement");
        if ((this.rootElement == null) || (this.rootElement.length() == 0)) {
            this.rootElement = "janeliaMetadata";
        }
        
        for (String name : props.keySet()) {
            if (! GroupPropertyToken.isGroupPropertyToken(name)) {
                additionalData.put(name,
                                   new PropertyTokenList(props.get(name),
                                                         props));
            }
        }
    }

    /**
     * Notifies this plug-in that an event has occurred.
     *
     * @param  eventType  type of event.
     * @param  row        details about the event.
     *
     * @return the field row for processing (with any updates from this plugin).
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        if (EventType.START_ROW.equals(eventType)) {
            row = insertMetaData(row);
        }
        return row;
    }

    private PluginDataRow insertMetaData(PluginDataRow row)
            throws ExternalSystemException {

        if (row instanceof RenamePluginDataRow) {

            XmlStringBuilder xml = new XmlStringBuilder(rootElement);
            xml.setRow(row, additionalData);

            final File file = ((RenamePluginDataRow) row).getFromFile();
            final String fileName = file.getAbsolutePath();
            RandomAccessInputStream in = null;
            RandomAccessOutputStream out = null;
            try {

                TiffParser parser = new TiffParser(fileName);
                Boolean isLittleEndian = parser.checkHeader();
                long[] ifdOffsets = parser.getIFDOffsets();
                final int firstIFDIndex = 0;
                long firstIFDOffset = ifdOffsets[firstIFDIndex];
                IFD firstIFD = parser.getIFD(firstIFDOffset);

                in = parser.getStream();

                TiffSaver tiffSaver = new TiffSaver(fileName);
                tiffSaver.setLittleEndian(isLittleEndian);
                out = tiffSaver.getStream();
                long endOfFile = out.length();

                if (firstIFD.containsKey(TIFF_JF_TAGGER_TAG)) {

                    tiffSaver.overwriteIFDValue(in,
                                                firstIFDIndex,
                                                TIFF_JF_TAGGER_TAG,
                                                xml.toString());
                    LOG.info("replaced LSM meta data for " + fileName);

                } else {

                    long next = getNextOffsetLocation(in, firstIFDOffset);
                    long nextValue = (next & ~0xffffffffL) |
                                     (in.readInt() & 0xffffffffL);

                    firstIFD.put(TIFF_JF_TAGGER_TAG, xml.toString());
                    out.seek(endOfFile);

                    tiffSaver.writeIFD(firstIFD, nextValue);

                    out.seek(4);
                    out.writeInt((int) endOfFile);

                    LOG.info("added LSM meta data to " + fileName);
                }

            } catch (Exception e) {
                throw new ExternalSystemException(
                        "failed to insert meta data into " + fileName, e);
            } finally {

                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        LOG.warn("failed to close input stream for " +
                                 fileName,
                                 e);
                    }
                }

                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        LOG.warn("failed to close output stream for " +
                                 fileName,
                                 e);
                    }
                }
            }
        }

        return row;
    }

    private long getNextOffsetLocation(RandomAccessInputStream in,
                                       long offset)
            throws IOException, FormatException {

        in.seek(offset);
        int nEntries = in.readUnsignedShort();
        in.skipBytes(nEntries * TiffConstants.BYTES_PER_ENTRY);
        return in.getFilePointer();
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(LsmMetaDataPlugin.class);

    /** Tag number reserved by Gene Myers for his tiff formatted files. */
    private static final int TIFF_JF_TAGGER_TAG = 36036;

    public static void main(String[] args) {
        if (args.length > 0) {
            try {
                final String fileName = args[0];
                final TiffParser parser = new TiffParser(fileName);
                final TiffIFDEntry entry =
                        parser.getFirstIFDEntry(TIFF_JF_TAGGER_TAG);
                System.out.println(parser.getIFDValue(entry));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}