/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.chacrm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PluginUtil;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowValidator;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * This class validates that lines entered with GMR_ prefixes actually
 * exist in the ChaCRM database.
 *
 * @author Eric Trautman
 */
public class ChacrmLineValidator
        implements RowValidator {

    /** The logger for this class. */
    private static final Log LOG =
            LogFactory.getLog(ChacrmLineValidator.class);

    /** The data access object for retrieving and updating transformant data. */
    private TransformantDao dao;

    /** The name of the property that contains the ChaCRM line. */
    private String linePropertyName;

    /**
     * The maximum amount of time (in milliseconds) between cache
     * references before the cache should be cleared.  This is intended to
     * keep the cache from getting stale.
     */
    private long clearCacheDuration;

    /** Time the cache was last referenced. */
    private long lastCacheAccessTime;

    /**
     * Cache for previously validated transformant identifiers.
     *
     * This improves performance but is actually needed to reduce
     * the number of TCP/IP sockets created for ChaCRM database connections.
     * Although each connection is properly closed, the OS can leave the
     * socket in a TIME_WAIT state for up to 4 minutes after the close.
     * With a large enough file set, all sockets can eventually be taken.
     * A database connection pool would also resolve this problem.
     *
     * TODO: consider adding database connection pooling support
     */
    private Set<String> validTransformantIds;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public ChacrmLineValidator() {
        this.linePropertyName = "Line";
        this.clearCacheDuration = 60 * 1000; // one minute
        this.lastCacheAccessTime = System.currentTimeMillis();
        this.validTransformantIds = new HashSet<String>();
    }

    /**
     * Verifies that the plugin is ready for use.
     *
     * @param  config  the plugin configuration.
     *
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config) throws ExternalSystemException {
        try {
            setDao();
            dao.checkAvailability();
            String configuredLinePropertyName =
                    config.getProperty("linePropertyName");
            if (configuredLinePropertyName != null) {
                this.linePropertyName = configuredLinePropertyName;
            }
            String configuredClearCacheDuration =
                    config.getProperty("clearCacheDuration");
            if (configuredClearCacheDuration != null) {
                this.clearCacheDuration =
                        Long.parseLong(configuredClearCacheDuration);
            }
        } catch (Exception e) {
            throw new ExternalSystemException(
                    "Failed to initialize ChacrmLineValidator plugin.  " +
                    e.getMessage(),
                    e);
        }
    }

    /**
     * If the row identifies a GMR line, validate that the line
     * exists in ChaCRM.
     *
     * @param  row  the user supplied meta-data to be validated.
     *
     * @throws ExternalDataException
     *   if the data is not valid.
     *
     * @throws ExternalSystemException
     *   if any error occurs while validating the data.
     */
    public void validate(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        RenamePluginDataRow dataRow = PluginUtil.castRenameRow(row, this);

        String transformantId = getTransformantID(dataRow);
        if (transformantId != null) {

            clearCacheIfStale();

            boolean isValid = validTransformantIds.contains(transformantId);
            if (! isValid) {
                try {
                    dao.getTransformant(transformantId, false);
                    addTransformantIdToCache(transformantId);
                } catch (TransformantNotFoundException e) {
                    File fromFile = dataRow.getFromFile();
                    String fileName = fromFile.getName();
                    throw new ExternalDataException(
                            "Transformant ID '" + transformantId +
                            "' does not exist in the ChaCRM database.  " +
                            "Please verify the " + linePropertyName +
                            " specified for the file " +
                            fileName, e);
                } catch (ExternalSystemException e) {
                    throw new ExternalSystemException(
                            "Failed to retrieve ChaCRM status for " +
                            "transformant ID '" + transformantId +
                            "' because of a system error.", e);
                }
            }
        }
    }

    /**
     * Utility to parse a transformant identifier from the line property.
     *
     * @param  row  the set of entered data fields.
     *
     * @return the corresponding transformant identifier or null if the
     *         line does not fit the ChaCRM line format.
     */
    private String getTransformantID(PluginDataRow row) {
        String transformantID = null;
        String line = row.getCoreValue(linePropertyName);
        if ((line != null) && line.startsWith("GMR_") && (line.length() > 4)) {
            transformantID = line.substring(4);
        }
        return transformantID;
    }

    /**
     * Create the dao for this plug-in if it does not already exist.
     *
     * @throws ExternalSystemException
     *   if any error occurs during creation.
     */
    private synchronized void setDao() throws ExternalSystemException {
        if (dao == null) {
            dao = new TransformantDao();
        }
    }

    /**
     * Clears the cache if it has not been accessed recently.
     */
    private synchronized void clearCacheIfStale() {

        if ((System.currentTimeMillis() - lastCacheAccessTime) >
            clearCacheDuration) {

            LOG.info("clearing cache containing " +
                     validTransformantIds.size() + " items");
            validTransformantIds.clear();
        }

        lastCacheAccessTime = System.currentTimeMillis();
    }

    /**
     * Adds the specified transformant identifier to the cache.
     *
     * @param  transformantId  identifier to add.
     */
    private synchronized void addTransformantIdToCache(String transformantId) {
        validTransformantIds.add(transformantId);
    }
}