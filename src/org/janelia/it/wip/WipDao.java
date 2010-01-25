/*
 * Copyright 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.wip;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.utils.db.AbstractDao;
import org.janelia.it.utils.db.DbConfigException;
import org.janelia.it.utils.db.DbManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This class supports access to batch data in the
 * Work In Progress (WIP) database.
 *
 * @author Eric Trautman
 */
public class WipDao extends AbstractDao {

    /**
     * Constructs a dao using the default manager and configuration.
     *
     * @param  dbConfigurationKey  the key for loading database
     *                             configuration information.
     *
     * @throws ExternalSystemException
     *   if the database configuration information cannot be loaded.
     */
    public WipDao(String dbConfigurationKey) throws ExternalSystemException {
        super(dbConfigurationKey);
    }

    /**
     * @param  batchNumber  batch number.
     * @param  batchColor   batch color.
     *
     * @return the name of the batch associated with the specified number
     *         and color (or null if none exists).
     *
     * @throws ExternalSystemException
     *   if errors occur while retrieving the data.
     */
    public String getBatchName(String batchNumber,
                               String batchColor)
            throws ExternalSystemException {

        String batchName = null;
        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement select = null;

        try {
            DbManager dbManager = getDbManager();
            connection = dbManager.getConnection();
            final String SQL =
                    "SELECT name FROM batch WHERE alt_name=? and name like ?";
            select = connection.prepareStatement(SQL);
            select.setString(1, batchNumber);
            select.setString(2, "%_" + batchColor);
            resultSet = select.executeQuery();
            if (resultSet.next()) {
                batchName = resultSet.getString(1);
            }

        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ExternalSystemException(
                    "Failed to find name for batch number '" +
                    batchNumber + "' and color '" + batchColor + "'", e);
        } finally {
            DbManager.closeResources(resultSet, select, connection, LOG);
        }

        return batchName;
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(WipDao.class);
}