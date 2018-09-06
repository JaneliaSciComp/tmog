/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import loci.formats.CoreMetadata;
import loci.formats.in.ZeissLSMReader;
import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * This class handles row and session events "published" by the
 * transmogrifier tool.  As renaming is performed, this plug-in
 * records the set of plugin data for each successfully processed row.
 * Upon completion of session processing, the plug-in creates
 * tab separated value (tsv) summary files that contain Zeiss data for
 * the successfully processed lsm files.  One summary file is created
 * for each distinct lsm parent directory (and is placed in that directory).
 * The data fields included in each file can be limited by specifying
 * key patterns in the plug-in configuration.
 * </p>
 *
 * <p>
 * This plug-in is designed to simply log any processing errors and will
 * not throw an exception if an error occurs while processing an event.
 * However, it could throw an exception during initialization.
 * </p>
 *
 * <p>
 * Note that this plug-in should be configured solely as a "sessionListener".
 * The {@link org.janelia.it.ims.tmog.config.PluginFactory#constructInstances}
 * method takes care of adding it as a "rowListener".
 * </p>
 *
 * @author Eric Trautman
 */
public class SessionCompleteZeissDataWriter
        implements SessionListener, RowListener {

    /**
     * Plug-in instances are shared across all session threads,
     * so we need to track successfully processed rows for each thread.
     */
    private Map<Thread, List<PluginDataRow>> threadToSuccessfulRows;

    /** The base name for each meta data file. */
    private String baseFileName;

    /**
     * The list of metadata key patterns used to identify what data values
     * are included in the summary file.  If no key patterns are
     * configured (this list is empty) then all data values are included.
     */
    private List<Pattern> keyPatternList;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public SessionCompleteZeissDataWriter() {
        this.threadToSuccessfulRows =
                new ConcurrentHashMap<Thread, List<PluginDataRow>>();
        this.keyPatternList = new ArrayList<Pattern>();
    }

    /**
     * Verifies that the plugin is ready for use.
     *
     * @param config the plugin configuration.
     *
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config)
            throws ExternalSystemException {

        baseFileName = config.getProperty("baseFileName");
        if (baseFileName == null) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "Please specify a baseFileName property.");
        }

        Map<String, String> props = config.getProperties();
        String patternString;
        for (String key : props.keySet()) {
            if (key.startsWith("key")) {
                patternString = props.get(key);
                try {
                    keyPatternList.add(Pattern.compile(patternString));
                } catch (Exception e) {
                    throw new ExternalSystemException(
                            INIT_FAILURE_MSG + "Invalid pattern '" +
                            patternString +
                            "' specified.  Detailed error is: " +
                            e.getMessage());
                }
            }
        }

    }

    /**
     * For successful end row events, saves the row so that it can be
     * referenced later when the session ends.
     *
     * @param  eventType  type of event.
     * @param  row        details about the event.
     *
     * @return the specified row unchanged.
     *
     * @throws ExternalDataException
     *   never.
     * @throws ExternalSystemException
     *   never.
     */
    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        final Thread currentThread = Thread.currentThread();
        if (EventType.END_ROW_SUCCESS.equals(eventType)) {
            List<PluginDataRow> successList =
                    threadToSuccessfulRows.get(currentThread);
            if (successList == null) {
                successList = new ArrayList<PluginDataRow>();
                threadToSuccessfulRows.put(currentThread, successList);
            }
            successList.add(row);
        }
        return row;
    }

    @Override
    public List<DataRow> startSession(List<DataRow> modelRows)
            throws ExternalDataException, ExternalSystemException {
        return null;  // ignored event
    }

    /**
     * Writes an empty file in each directory
     * where successful renamed files were created.
     *
     * @param  message  a message summarizing what was processed.
     *
     * @throws ExternalDataException
     *   never.
     * @throws ExternalSystemException
     *   never.
     */
    @Override
    public void endSession(String message)
            throws ExternalDataException, ExternalSystemException {

        HashMap<File, List<ZeissData>> directoryToZeissMap =
                getDirectoryToZeissMapForCurrentThread();
        if ((directoryToZeissMap != null) && (directoryToZeissMap.size() > 0)) {
            writeDataFiles(directoryToZeissMap);
        }
    }

    /**
     * @return map of distinct directories to lists of Zeiss data objects
     *         for each processed lsm file in the directory.
     *         If key patterns have been specified, the map will only
     *         contain data for files that have matching keys.
     */
    private HashMap<File, List<ZeissData>> getDirectoryToZeissMapForCurrentThread() {

        final Thread currentThread = Thread.currentThread();
        final List<PluginDataRow> successList =
                threadToSuccessfulRows.remove(currentThread);
        HashMap<File, List<ZeissData>> directoryToFileMap = null;

        if ((successList != null) && (successList.size() > 0)) {

            final int size = successList.size();

            LOG.info("getDirectoryToZeissMapForCurrentThread: parsing " + size +
                     " processed files");

            directoryToFileMap = new HashMap<File, List<ZeissData>>(size);
            File targetFile;
            File directory;
            List<ZeissData> fileList;
            ZeissData zeissData;
            for (PluginDataRow row : successList) {
                targetFile = row.getTargetFile();
                if (targetFile.getName().endsWith(".lsm")) {
                    zeissData = new ZeissData(targetFile);
                    if (zeissData.hasMatchingKeys()) {
                        directory = targetFile.getParentFile();
                        fileList = directoryToFileMap.get(directory);
                        if (fileList == null) {
                            fileList = new ArrayList<ZeissData>(size);
                            directoryToFileMap.put(directory, fileList);
                        }
                        fileList.add(zeissData);
                    }
                }
            }

            // sort list in result file order instead of from file order
            for (List<ZeissData> list : directoryToFileMap.values()) {
                Collections.sort(list);
            }
        }

        return directoryToFileMap;
    }

    /**
     * Writes the Zeiss data to directory specific summary files.
     *
     * @param  directoryToZeissMap  map of distinct directories to lists
     *                              of Zeiss data objects for each processed
     *                              lsm file in the directory.
     */
    private void writeDataFiles(HashMap<File, List<ZeissData>> directoryToZeissMap) {

        final String fileNameWithSuffix =
                baseFileName + SDF.format(new Date());

        Set<String> orderedKeys;
        File dataFile;
        FileWriter dataFileWriter;
        List<ZeissData> dataList;
        for (File directory : directoryToZeissMap.keySet()) {
            orderedKeys = new TreeSet<String>();
            dataFile = new File(directory, fileNameWithSuffix);
            dataFileWriter = null;
            try {
                dataFileWriter = new FileWriter(dataFile);
                dataList = directoryToZeissMap.get(directory);

                // determine ordered super-set of all keys since
                // different files may not have the same keys
                for (ZeissData data : dataList) {
                    orderedKeys.addAll(data.getMatchingKeys());
                }

                // write the summary file header
                dataFileWriter.write("File\t");
                for (String orderedKey : orderedKeys) {
                    dataFileWriter.write(orderedKey);
                    dataFileWriter.write('\t');
                }
                dataFileWriter.write('\n');

                // write the Zeiss data for each lsm
                for (ZeissData data : dataList) {
                    dataFileWriter.write(data.getDataRow(orderedKeys));
                }

                LOG.info("writeDataFiles: " + orderedKeys.size() +
                         " fields written for " + dataList.size() +
                         " lsm files to " + dataFile.getAbsolutePath());

            } catch (Throwable t) {
                // only log errors, do not want to raise
                LOG.error("writeDataFiles: failed to write Zeiss data to " +
                          dataFile.getAbsolutePath() + ".", t);
            } finally {
                if (dataFileWriter != null) {
                    try {
                        dataFileWriter.close();
                    } catch (IOException e) {
                        LOG.error("writeDataFiles: failed to close " +
                                  dataFile.getAbsolutePath(), e);
                    }
                }
            }
        }

    }

    /**
     * Helper class for working with data parsed from a Zeiss lsm file.
     */
    private class ZeissData implements Comparable<ZeissData> {

        private File lsmFile;
        private Set<String> matchingKeys;
        private CoreMetadata core;

        public ZeissData(File lsmFile) {
            this.lsmFile = lsmFile;
            this.matchingKeys = new HashSet<String>();
            try {
                ZeissLSMReader zlr = new ZeissLSMReader();
                Matcher m;
                zlr.initFile(lsmFile.getAbsolutePath());
                this.core = zlr.getCore();
                if (keyPatternList.size() > 0) {
                    for (String key : this.core.seriesMetadata.keySet()) {
                        for (Pattern pattern : keyPatternList) {
                            m = pattern.matcher(key);
                            if (m.matches()) {
                                this.matchingKeys.add(key);
                            }
                        }
                    }
                } else {
                    this.matchingKeys = this.core.seriesMetadata.keySet();
                }
            } catch (Throwable t) {
                LOG.error("ZeissData: failed to parse " +
                          lsmFile.getAbsolutePath(), t);
            }
        }

        public boolean hasMatchingKeys() {
            return matchingKeys.size() > 0;
        }

        public Set<String> getMatchingKeys() {
            return matchingKeys;
        }

        public String getDataRow(Set<String> orderedKeys) {
            StringBuilder sb = new StringBuilder(2048);
            sb.append(lsmFile.getName());
            sb.append('\t');
            for (String orderedKey : orderedKeys) {
                if (core != null) {
                    sb.append(core.seriesMetadata.get(orderedKey));
                }
                sb.append('\t');
            }
            sb.append('\n');
            return sb.toString();
        }

        @Override
        public int compareTo(ZeissData that) {
            return lsmFile.compareTo(that.lsmFile);
        }
    }

    public static void main(String[] args) {
        SessionCompleteZeissDataWriter plugin =
                new SessionCompleteZeissDataWriter();

        if (args.length < 2) {
            System.out.println(
                    "USAGE: java " + SessionCompleteZeissDataWriter.class.getName() +
                    " <baseFileName> <lsmFile> [lsmFile ...]");
            System.exit(1);
        }

        PluginConfiguration config = new PluginConfiguration();
        config.setProperty("baseFileName", args[0]);

        List<PluginDataRow> dataRows = new ArrayList<PluginDataRow>();
        File file;
        DataRow dataRow;
        for (int i = 1; i < args.length; i++) {
            file = new File(args[i]);
            if (file.exists()) {
                dataRow = new DataRow(new FileTarget(file));
                dataRows.add(new PluginDataRow(dataRow));
            }
        }
        try {
            plugin.init(config);
            for (PluginDataRow row : dataRows) {
                plugin.processEvent(EventType.END_ROW_SUCCESS, row);
            }
            plugin.endSession("done!");
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    private static final Logger LOG =
            Logger.getLogger(SessionCompleteZeissDataWriter.class);

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize SessionCompleteZeissDataWriter plug-in.  ";

    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("'-'yyyyMMdd'-'HHmmss'-'SSS'.tsv'");
}