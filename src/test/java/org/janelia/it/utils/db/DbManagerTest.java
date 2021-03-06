/*
 * Copyright (c) 2016 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.utils.db;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.util.Properties;

/**
 * Tests the DbManager class.
 *
 * @author Eric Trautman
 */
public class DbManagerTest extends TestCase {

    private static final Log LOG = LogFactory.getLog(DbManagerTest.class);

    /**
     * Constructs a test case with the given name.
     *
     * @param name name of the test case.
     */
    public DbManagerTest(String name) {
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
        return new TestSuite(DbManagerTest.class);
    }

    /**
     * Tests the getConnection method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetConnection() throws Exception {

        final String dbName = "sage";
        Properties props = AbstractDao.loadDatabaseProperties(dbName);
        DbManager mgr = new DbManager(dbName, props);

        Exception connectionException = null;
        Connection connection = null;

        try {
            connection = mgr.getConnection();
        } catch (Exception e) {
            connectionException = e;
        }
        finally {
            DbManager.closeResources(null, null, connection, LOG);
        }

        if (connectionException != null) {
            throw connectionException;
        }
    }

}
