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

    /**
     * Name of the property that identifies
     * the name of the data row field that contains the ChaCRM line.
     */
    public static final String LINE_FIELD_PROPERTY_NAME = "linePropertyName";

    /** Name of the property containing the ChaCRM line prefix. */
    public static final String LINE_PREFIX_PROPERTY_NAME = "linePrefix";

    /** Default ChaCRM line prefix value. */
    public static final String DEFAULT_LINE_PREFIX = "GMR_";

    /** The logger for this class. */
    private static final Log LOG =
            LogFactory.getLog(ChacrmLineValidator.class);

    /** The data access object for retrieving and updating transformant data. */
    private TransformantDao dao;

    /** The name of the data row field that contains the ChaCRM line. */
    private String lineFieldName;

    /** The prefix used to identify Chacrm lines (default is 'GMR_'). */
    private String linePrefix;
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
        this.lineFieldName = null;
        this.linePrefix = DEFAULT_LINE_PREFIX;
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
                    config.getProperty(LINE_FIELD_PROPERTY_NAME);
            if (configuredLinePropertyName != null) {
                this.lineFieldName = configuredLinePropertyName;
            }
            String configuredChacrmLinePrefix =
                    config.getProperty(LINE_PREFIX_PROPERTY_NAME);
            if (configuredChacrmLinePrefix != null) {
                this.linePrefix = configuredChacrmLinePrefix;
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
                            "Please verify the " + lineFieldName +
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
        if (lineFieldName == null) {
            transformantID = ChacrmEventManager.getTransformantID(row);
        } else {
            String line = row.getCoreValue(lineFieldName);
            final int prefixLength = linePrefix.length();
            if ((line != null) &&
                line.startsWith(linePrefix) &&
                (line.length() > prefixLength)) {
                transformantID = line.substring(prefixLength);
            }
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