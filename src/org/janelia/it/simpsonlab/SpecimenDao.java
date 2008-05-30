/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.simpsonlab;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.imagedb.ImageDao;
import org.janelia.it.utils.db.DbConfigException;
import org.janelia.it.utils.db.DbManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This class supports management of image data within the Simpson Lab
 * data repository.
 *
 * @author Eric Trautman
 */
public class SpecimenDao extends ImageDao {

    /**
     * Constructs a dao using the default manager and configuration.
     *
     * @throws ExternalSystemException
     *   if the database configuration information cannot be loaded.
     */
    public SpecimenDao() throws ExternalSystemException {
        super("simpson_nighthawk");
    }

    /**
     * Retrieves a the next line specimen number for the specified namespace.
     *
     * @param  line  the fly line of interest.
     *
     * @return the next line specimen number for the specified namespace.
     *
     * @throws ExternalSystemException
     *   if errors occur while retrieving the data.
     */
    public int getNextSpecimenNumber(Line line)
            throws ExternalSystemException {

        int nextNumber = 1;
        String namespace = line.getSpecimenNamespace();
        int rowsUpdated;

        Connection connection = null;
        ResultSet selectResultSet = null;
        PreparedStatement select = null;
        PreparedStatement increment = null;

        try {
            DbManager dbManager = getDbManager();
            connection = dbManager.getConnection();
            connection.setAutoCommit(false);
            select = connection.prepareStatement(SQL_SELECT_SPECIMEN_NUMBER);
            select.setString(1, namespace);
            selectResultSet = select.executeQuery();
            if (selectResultSet.next()) {
                Integer currentNumber = selectResultSet.getInt(1);
                nextNumber = currentNumber + 1;
                increment =
                        connection.prepareStatement(SQL_UPDATE_SPECIMEN_NUMBER);
                increment.setInt(1, nextNumber);
                increment.setString(2, namespace);
                rowsUpdated = increment.executeUpdate();
                if (rowsUpdated != 1) {
                    throw new ExternalSystemException(
                            "Failed to update next specimen number for " +
                            namespace + ".  Attempted to update " +
                            rowsUpdated + " rows.");
                }
            } else {
                increment =
                        connection.prepareStatement(SQL_INSERT_SPECIMEN_NUMBER);
                increment.setString(1, namespace);
                rowsUpdated = increment.executeUpdate();
                if (rowsUpdated != 1) {
                    throw new ExternalSystemException(
                            "Failed to create specimen number for " +
                            namespace + ".  Attempted to create " +
                            rowsUpdated + " rows.");
                }
            }

            connection.commit();

        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ExternalSystemException(
                    "Failed to determine next specimen number for " +
                    namespace, e);
        } finally {
            DbManager.closeResources(selectResultSet, select, null, LOG);
            DbManager.closeResources(null, increment, connection, LOG);
        }

        return nextNumber;
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(SpecimenDao.class);

    /**
     * SQL for retrieving the current (max) specimen number for a line.
     *   Parameter 1 is the line specimen namespace.
     */
    private static final String SQL_SELECT_SPECIMEN_NUMBER =
            "SELECT sequence_number FROM namespace_sequence_number " +
            "WHERE namespace=? FOR UPDATE";

    /**
     * SQL for inserting the first specimen number for a line.
     *   Parameter 1 is the line specimen namespace.
     */
    private static final String SQL_INSERT_SPECIMEN_NUMBER =
            "INSERT INTO namespace_sequence_number " +
            "(namespace, sequence_number) VALUES (?, 1)";

    /**
     * SQL for updating the max specimen number for a line.
     *   Parameter 1 is the new specimen number.
     *   Parameter 2 is the line specimen namespace.
     */
    private static final String SQL_UPDATE_SPECIMEN_NUMBER =
            "UPDATE namespace_sequence_number SET sequence_number=? " +
            "WHERE namespace=?";

}