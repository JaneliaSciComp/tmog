/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.utils.db;

import org.apache.commons.logging.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * This class supports the management and configuration of basic
 * JDBC connections.  It also provides common database utility methods.
 *
 * @author Eric Trautman
 */
public class DbManager {

    /** The logical (configuration) name for this set of properties. */
    private String logicalName;

    /** The configuration properties for this manager. */
    private Properties properties;

    /** The JDBC driver class name. */
    private String driverClassName;

    /** The JDBC URL. */
    private String url;

    /** The user id for establishing connections. */
    private String user;

    /** The user's password. */
    private String password;

    /** The database schema. */
    private String schema;

    /**
     * Simple constructor that saves the logical name for this manager
     * (configuration).  Properties are loaded when the first connection
     * is established (see {@link #getConnection()}.
     *
     * @param  logicalName  the logical (configuration) name for this manager.
     * @param  properties   the configuration properties for this manager.
     */
    public DbManager(String logicalName,
                     Properties properties) {
        this.logicalName = logicalName;
        this.properties = properties;
        this.driverClassName = null;
        this.url = null;
        this.user = null;
        this.password = null;
        this.schema = null;
    }

    /**
     * Creates a connection to this database.
     *
     * @return a new connection for this database.
     *
     * @throws DbConfigException
     *   if any errors occur while loading configuration information or
     *   or while creating the connection.
     */
    public Connection getConnection() throws DbConfigException {

        this.init();

        Connection connection;
        try {
            connection = DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            throw new DbConfigException("Failed to get JDBC connection for " +
                                      this.toString(),
                                      e);
        }
        return connection;
    }

    /**
     * @return the default schema for this database.
     */
    public String getSchema() {
        return schema;
    }

    /**
     * @return a string representation of the object.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(512);
        sb.append("{DbManager: logicalName=");
        sb.append(this.logicalName);
        sb.append(", driverClassName=");
        sb.append(this.driverClassName);
        sb.append(", url=");
        sb.append(this.url);
        sb.append(", user=");
        sb.append(this.user);
        // leave password out of logs for security reasons
        // sb.append(", password=");
        // sb.append(this.password);
        sb.append(", schema=");
        sb.append(this.schema);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Utility to close any non-null data resource objects.
     *
     * @param   resultSet    result set to close (or null).
     * @param   statement    statement to close (or null).
     * @param   connection   connection to close (or null).
     * @param   logger       the current logger (for logging any close errors).
     */
    public static void closeResources(ResultSet resultSet,
                                      Statement statement,
                                      Connection connection,
                                      Log logger) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                logger.error("failed to close result set, ignoring error", e);
            }
        }

        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                logger.error("failed to close statement, ignoring error", e);
            }
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("failed to close connection, ignoring error", e);
            }
        }
    }

    /**
     * Lazy loads this manager's properties and driver.
     *
     * @throws DbConfigException
     *   if any errors occur while loading.
     */
    private void init() throws DbConfigException {
        if (this.driverClassName == null) {
            this.loadProperties();
            this.loadDriver();
        }
    }

    /**
     * Loads this manager's properties from the global configuration file.
     *
     * @throws DbConfigException
     *   if any errors occur reading the configuration file.
     */
    private void loadProperties() throws DbConfigException {
        String prefix = "db." + logicalName + ".";
        this.driverClassName = getRequiredProperty(prefix, "driver_class");
        this.url = getRequiredProperty(prefix, "url");
        this.user = getRequiredProperty(prefix, "username");
        this.password = getRequiredProperty(prefix, "password");
        //this.schema = getRequiredProperty(prefix, "schema");
    }

    /**
     * Utility to read required elements from the configuration properties.
     *
     * @param prefix        the property name prefix for this manager.
     * @param propertyName  the base property name to retrieve.
     *
     * @return the associated property value.
     *
     * @throws DbConfigException
     *   if the property value does not exist.
     */
    private String getRequiredProperty(String prefix,
                                       String propertyName)
            throws DbConfigException {

        String fullPropertyName = prefix + propertyName;
        String value = properties.getProperty(fullPropertyName);
        if (value == null) {
            throw new DbConfigException(
                    "Configuration error.  Failed to find " +
                            fullPropertyName + " in properties: " +
                            properties);
        }
        return value;
    }

    /**
     * Loads and registers a database driver class.
     *
     * @throws DbConfigException
     *   if the configured JDBC driver cannot be found.
     */
    private void loadDriver() throws DbConfigException {
        try {
            Class driverClass = Class.forName(driverClassName);
            driverClass.newInstance();
        } catch (Exception e) {
            throw new DbConfigException(
                    "Failed to load configured JDBC driver " + driverClassName +
                    ".  Check runtime classpath or database configuration.  " +
                    ".  Properties loaded for this instance are: " + this,
                    e);
        }
    }
}
