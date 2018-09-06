/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import loci.common.RandomAccessInputStream;
import loci.common.RandomAccessOutputStream;
import loci.formats.in.ZeissLSMReader;
import loci.formats.tiff.IFD;
import loci.formats.tiff.TiffIFDEntry;
import loci.formats.tiff.TiffParser;
import loci.formats.tiff.TiffSaver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.config.PluginConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

    protected PluginDataRow insertMetaData(PluginDataRow row)
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
                final Boolean isLittleEndian = parser.checkHeader();
                final boolean isBigTiff = parser.isBigTiff();
                final long[] ifdOffsets = parser.getIFDOffsets();
                final int firstIFDIndex = 0;
                final long firstIFDOffset = ifdOffsets[firstIFDIndex];
                final long secondIFDOffset = ifdOffsets[1];
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

                    firstIFD.put(TIFF_JF_TAGGER_TAG, xml.toString());
                    out.seek(endOfFile);

                    tiffSaver.writeIFD(firstIFD, secondIFDOffset);

                    if (isBigTiff || (endOfFile >= Integer.MAX_VALUE)) {
                        LOG.warn(fileName + " has " + endOfFile +
                                 " bytes, isBigTiff is " + isBigTiff +
                                 ", leaving header offset alone so meta data " +
                                 "will not be reachable via IFD traversal");
                    } else {
                        out.seek(4);
                        out.writeInt((int) endOfFile);
                    }

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

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(LsmMetaDataPlugin.class);

    /** Tag number reserved by Gene Myers for his tiff formatted files. */
    private static final int TIFF_JF_TAGGER_TAG = 36036;

    private static final int TIFF_ZEISS_LSM_TAG = 34412;

    /**
     * @param  fileName  name of tiff file to parse.
     *                 
     * @return janelia metadata store in file or null if none can be found.
     */
    public static String readMetaData(String fileName) {
        String metaData = null;
        try {
            final TiffParser parser = new TiffParser(fileName);
            final TiffIFDEntry entry =
                    parser.getFirstIFDEntry(TIFF_JF_TAGGER_TAG);
            metaData = String.valueOf(parser.getIFDValue(entry));
        } catch (Exception e) {
            LOG.error("failed to retrieve Janelia metadata from " + fileName,
                      e);
        }
        return metaData;
    }

    /**
     * @param  fileName  name of tiff file to parse.
     *                   
     * @return true if a Zeiss LSM IFD block exists in the file;
     *         otherwise false.
     */
    public static boolean hasZeissLsmDirectory(String fileName) {
        boolean hasDirectory = false;
        try {
            TiffParser parser = new TiffParser(fileName);
            parser.getFirstIFDEntry(TIFF_ZEISS_LSM_TAG);
            hasDirectory = true;
            LOG.info("found Zeiss LSM Tag in " + fileName);
        } catch (Exception e) {
            LOG.error("failed to find Ziess LSM Tag in " + fileName, e);
        }
        return hasDirectory;
    }

    /**
     * Restores the first IFD offset value in the specified tiff file's
     * header to it's default value of 8.  This will orphan any Janelia
     * metadata at the end of the file.
     *
     * @param  fileName  name of lsm file to restore.
     *                   
     * @throws Exception
     *   if any errors occur during the restoration.
     */
    private static void restoreFirstIFDOffset(String fileName)
            throws Exception {

        RandomAccessOutputStream out = null;
        try {

            TiffParser parser = new TiffParser(fileName);
            
            if (parser.isBigTiff()) {
                throw new IllegalStateException(
                        "big tiff format file headers " +
                        "cannot be restored by this method");
            }

            final Boolean isLittleEndian = parser.checkHeader();
            TiffSaver tiffSaver = new TiffSaver(fileName);
            tiffSaver.setLittleEndian(isLittleEndian);
            out = tiffSaver.getStream();
            out.seek(4);
            out.writeInt(8);

        } finally {

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

    private static boolean confirm(String question) {
        System.out.print(question + " [y|n] ");
        BufferedReader br = 
                new BufferedReader(new InputStreamReader(System.in));
        boolean isConfirmed = false;
        try {
            String r = br.readLine();
            isConfirmed = "y".equalsIgnoreCase(r);
        } catch (IOException e) {
            LOG.error("failed to read response", e);
        }
        return isConfirmed;
    }

    private static void writeZeiss(String fileName) {
        ZeissLSMReader zlr = new ZeissLSMReader();
        try {
            zlr.initFile(fileName);
            zlr.printAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        final String meta = "--meta";
        final String restore = "--restore";
        final String zeiss = "--zeiss";
        final String writeZeiss = "--writeZeiss";
        final String usage =
                "\n\nUSAGE: java " + LsmMetaDataPlugin.class.getName() + 
                " [" + meta + "] [" + restore + "] [" + zeiss + "] [" +
                writeZeiss + "] file [file ...]\n";
        
        boolean isMeta = false;
        boolean isRestore = false;
        boolean isZeiss = false;
        boolean isWriteZeiss = false;
        Set<String> fileNames = new HashSet<String>();
        for (String value : args) {
            if (meta.equals(value)) {
                isMeta = true;
            } else if (restore.equals(value)) {
                isRestore = true;
            } else if (zeiss.equals(value)) {
                isZeiss = true;
            } else if (writeZeiss.equals(value)) {
                isWriteZeiss = true;
            } else {
                fileNames.add(value);
            }
        }
        
        String metaData;
        if ((fileNames.size() > 0) && (isMeta || isRestore || isZeiss || isWriteZeiss)) {
            for (String fileName : fileNames) {

                if (isZeiss) {
                    hasZeissLsmDirectory(fileName);
                }

                if (isWriteZeiss) {
                    writeZeiss(fileName);
                }

                if (isMeta) {
                    metaData = readMetaData(fileName);
                    if (metaData != null) {
                        LOG.info("Janelia metadata for " + fileName +
                                 " is:\n" + metaData);
                    }
                }

                if (isRestore) {
                    boolean hasZeiss = hasZeissLsmDirectory(fileName);
                    
                    if ((! hasZeiss) &&
                        (confirm("Are you sure you wish to restore " +
                                 fileName + "?"))) {
                        try {
                            restoreFirstIFDOffset(fileName);
                        } catch (Exception e) {
                            LOG.error("failed to restore " + fileName, e);
                        }

                        hasZeissLsmDirectory(fileName);
                    } else {
                        LOG.info("skipping restore of " + fileName);
                    }
                }

            }
        } else {
            System.out.println(usage);
        }
    }
}