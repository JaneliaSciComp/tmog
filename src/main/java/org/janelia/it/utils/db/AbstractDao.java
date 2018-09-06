/*
 * Copyright 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.utils.db;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.utils.security.StringEncrypter;

import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;

/**
 * This class provides common methods for data access objects.
 *
 * @author Eric Trautman
 */
public abstract class AbstractDao {

    /**
     * The manager used to establish connections with the database.
     */
    private DbManager dbManager;

    private String dbConfigurationKey;

    /**
     * Constructs a dao using the default manager and configuration.
     *
     * @param  dbConfigurationKey  the key for loading database
     *                             configuration information.
     *
     * @throws ExternalSystemException
     *   if the database configuration information cannot be loaded.
     */
    public AbstractDao(String dbConfigurationKey) throws ExternalSystemException {
        Properties props = loadDatabaseProperties(dbConfigurationKey);
        this.dbManager = new DbManager(dbConfigurationKey, props);
        this.dbConfigurationKey = dbConfigurationKey;
    }

    public DbManager getDbManager() {
        return dbManager;
    }

    public String getDbConfigurationKey() {
        return dbConfigurationKey;
    }

    /**
     * Verifies that a connection to the database can be established.
     *
     * @throws org.janelia.it.ims.tmog.plugin.ExternalSystemException
     *   if a connection to the database can not be established.
     */
    public void checkAvailability() throws ExternalSystemException {
        Connection connection = null;
        try {
            connection = dbManager.getConnection();
        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } finally {
            DbManager.closeResources(null, null, connection, LOG);
        }
    }

    /**
     * Utility to load database properties from classpath.
     *
     * @param  dbConfigurationKey  the key for loading database
     *                             configuration information.
     *
     * @return populated database properties object.
     *
     * @throws ExternalSystemException
     *   if the load fails.
     */
    public static Properties loadDatabaseProperties(String dbConfigurationKey)
            throws ExternalSystemException {
        Properties props = new Properties();
        final String propFileName = "/" + dbConfigurationKey + ".properties";
        InputStream dbIn = AbstractDao.class.getResourceAsStream(propFileName);
        try {
            props.load(dbIn);
        } catch (Exception e) {
            throw new ExternalSystemException(
                    "Failed to load " + dbConfigurationKey +
                    " configuration properties.",
                    e);
        }

        String passwordKey = "db." + dbConfigurationKey + ".password";
        String encryptedPassword = props.getProperty(passwordKey);
        if ((encryptedPassword != null) && (encryptedPassword.length() > 0)) {
            try {
                StringEncrypter encrypter =
                        new StringEncrypter(
                                StringEncrypter.DES_ENCRYPTION_SCHEME,
                                StringEncrypter.DEFAULT_ENCRYPTION_KEY);
                String clearPassword = encrypter.decrypt(encryptedPassword);
                props.put(passwordKey, clearPassword);
            } catch (Exception e) {
                throw new ExternalSystemException(
                        "Failed to decrypt " + dbConfigurationKey +
                        " password.", e);
            }
        }

        return props;
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(AbstractDao.class);

}