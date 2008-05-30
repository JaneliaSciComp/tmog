/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.simpsonlab;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.utils.db.DbManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Tests the SpecimenDao class.
 *
 * @author Eric Trautman
 */
public class SpecimenDaoTest extends TestCase {

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(SpecimenDaoTest.class);

    private SpecimenDao dao;
    private Line testLine;

    /**
     * This flag can be used to stop database cleanup in each test's
     * tearDown method when you need to debug problems in the database.
     */
    private boolean isCleanupNeeded = true;

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public SpecimenDaoTest(String name) {
        super(name);
    }

    /**
     * Static method to return a suite of all tests.
     * <p/>
     * The JUnit framework uses Java reflection to build a suite of all public
     * methods that have names like "testXXXX()".
     *
     * @return suite of all tests defined in this class.
     */
    public static Test suite() {
        return new TestSuite(SpecimenDaoTest.class);
    }

    protected void setUp() throws Exception {
        dao = new SpecimenDao();
        testLine = new Line(LINE_NAME.format(new Date()), null);
    }

    protected void tearDown() throws Exception {
        if (isCleanupNeeded) {
            deleteTestSequenceNumber();
        }
    }

    /**
     * Tests the getNextSpecimenNumber method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetNextSpecimenNumber() throws Exception {
        int specimenNumber = dao.getNextSpecimenNumber(testLine);

        assertEquals("invalid initial number", 1, specimenNumber);

        specimenNumber = dao.getNextSpecimenNumber(testLine);
        assertEquals("invalid update number", 2, specimenNumber);
    }


    private void deleteTestSequenceNumber() throws Exception {
        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement statement = null;
        try {
            DbManager dbManager = dao.getDbManager();
            connection = dbManager.getConnection();
            statement = connection.prepareStatement(
                    SQL_DELETE_NAMESPACE_SEQUENCE_NUMBER);
            statement.setString(1, testLine.getSpecimenNamespace());
            int rowsUpdated = statement.executeUpdate();
            LOG.info("deleteTestSequenceNumber: completed, " + rowsUpdated +
                     " row(s) updated");
        } finally {
            DbManager.closeResources(resultSet, statement, connection, LOG);
        }
    }

    /**
     * SQL for deleting a namespace sequence number.
     *   Parameter 1 is the line specimen namespace.
     */
    private static final String SQL_DELETE_NAMESPACE_SEQUENCE_NUMBER =
            "DELETE FROM namespace_sequence_number WHERE namespace=?";

    private static final SimpleDateFormat LINE_NAME =
            new SimpleDateFormat("'testLine'yyyyMMddHHmmssSSS");
}