/*
 * Copyright (c) 2018 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple cache of data maps for an image family/database.
 *
 * @author Eric Trautman
 */
public class ImageDataCache {

    private static Map<String, ImageDataCache> familyDbKeyToCacheMap =
            new ConcurrentHashMap<>();

    static ImageDataCache getCache(String dbConfigurationKey,
                                   String family)
            throws ExternalSystemException {

        final String key = dbConfigurationKey + ":" + family;
        ImageDataCache cache = familyDbKeyToCacheMap.get(key);
        if (cache == null) {

            final ImageReader imageReader = new SageImageDao(dbConfigurationKey);

            imageReader.checkAvailability();

            cache = new ImageDataCache(family, imageReader);
            familyDbKeyToCacheMap.put(key, cache);
        }

        return cache;
    }

    private Map<String, Map<String, String>> pathToDataMap;
    private String family;
    private ImageReader imageReader;
    private long staleCacheTime;
    private long lastAccessTime;

    public ImageDataCache(String family,
                          ImageReader imageReader) {
        this.pathToDataMap =
                new ConcurrentHashMap<>();
        this.family = family;
        this.imageReader = imageReader;

        // refresh cache after 10 seconds of inactivity
        this.staleCacheTime = 10000;
        this.lastAccessTime = System.currentTimeMillis();
    }

    public String getValue(String relativePath,
                           String propertyName) {

        final long accessTime = System.currentTimeMillis();
        if ((accessTime - lastAccessTime) > staleCacheTime) {
            pathToDataMap.clear();
            LOG.info("cleared cache for " + family);
        }

        Map<String, String> data = pathToDataMap.get(relativePath);

        if (data == null) {
            try {
                data = imageReader.getImageData(family, relativePath);
            } catch (ExternalSystemException e) {
                LOG.warn("failed to retrieve properties for " + family +
                         " image " + relativePath, e);
                data = new HashMap<>();
            }
            pathToDataMap.put(relativePath, data);
        }

        lastAccessTime = System.currentTimeMillis();

        return data.get(propertyName);
    }

   /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(ImageDataCache.class);
}