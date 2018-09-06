/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.utils;

import loci.formats.CoreMetadata;
import loci.formats.in.ZeissLSMReader;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple cache of core meta data for LSM files. 
 *
 * @author Eric Trautman
 */
public class LsmCoreMetaDataCache {

    private Map<File, CoreMetadata> fileToDataMap;

    public LsmCoreMetaDataCache() {
        this.fileToDataMap = new ConcurrentHashMap<File, CoreMetadata>();
    }

    public Object getValue(File lsmFile,
                           String propertyName) {
        CoreMetadata data = fileToDataMap.get(lsmFile);
        if (data == null) {
            try {
                ZeissLSMReader zlr = new ZeissLSMReader();
                zlr.initFile(lsmFile.getAbsolutePath());
                data = zlr.getCore();
            } catch (Exception e) {
                data = new CoreMetadata();
                LOG.error("failed to load Zeiss data from " +
                          lsmFile.getAbsolutePath(), e);
            }
            fileToDataMap.put(lsmFile, data);
        }

        return data.seriesMetadata.get(propertyName);
    }

    private static final Logger LOG =
            Logger.getLogger(LsmCoreMetaDataCache.class);
}
