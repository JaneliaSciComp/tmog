/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.StaticDataModel;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.utils.filexfer.DigestAlgorithms;
import org.janelia.it.utils.filexfer.FileTransferUtil;

import java.io.File;

/**
 * Tests the LsmMetaDataPlugin class.
 *
 * @author Eric Trautman
 */
public class LsmMetaDataPluginTest
        extends TestCase {

    private LsmMetaDataPlugin plugin;
    private File testDirectory;
    private File testFile;
    private String testFilePath;
    
    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public LsmMetaDataPluginTest(String name) {
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
        return new TestSuite(LsmMetaDataPluginTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        plugin = new LsmMetaDataPlugin();
        PluginConfiguration config = new PluginConfiguration();
        plugin.init(config);

        // NOTE: Change this to an appropriate test directory based upon
        // where these tests are being run.  The directory should contain
        // one LSM file that is less than 4GB named 'testFileSmall.lsm' and
        // one LSM file that is greater than 4GB named 'testFileBig.lsm'.
        testDirectory = new File("/Users/trautmane/Desktop/flfl");
        testFile = new File(testDirectory, "LsmMetaDataPluginTest.lsm");
        testFilePath = testFile.getAbsolutePath();
    }

    @Override
    protected void tearDown() throws Exception {
        if ((testFile != null) && testFile.exists()) {
            System.out.println("cleaning up test file " + testFilePath);
            boolean successfulDelete = testFile.delete();
            if (! successfulDelete) {
                System.out.println("failed to delete " + testFilePath);
            }
        }
    }

    /**
     * Tests inserting Janelia metadata into a file that is less than 4GB.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testInsertMetaDataIntoSmallFile() throws Exception {
        runTestIfFileExists("small", "testFileSmall.lsm");
    }

    /**
     * Tests inserting Janelia metadata into a file that is greater than 4GB.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testInsertMetaDataIntoBigFile() throws Exception {
        runTestIfFileExists("big", "testFileBig.lsm");
    }

    private void runTestIfFileExists(String context,
                                     String srcFileName) throws Exception {
        final File srcFile = new File(testDirectory, srcFileName);
        if (srcFile.exists()) {
            internalInsertMetaDataTest(context, srcFile);
        } else {
            System.out.println("skipping " + this.getClass().getName() + " test since " +
                               srcFile.getAbsolutePath() + " does not exist");
        }
    }

    private void internalInsertMetaDataTest(String context,
                                            File srcFile) throws Exception {

        assertTrue("missing " + context + " source file: " +
                   srcFile.getAbsolutePath() + ", ignore this failure " +
                   "unless you intend to test metadata insertion",
                   srcFile.exists());

        // copy src file to a test file because the test will change the file

        FileTransferUtil util = new FileTransferUtil(100000000,
                                                     DigestAlgorithms.NONE);

        final String errorMessageSuffix = context + " test file: " +
                                          testFilePath;

        util.copy(srcFile, testFile);

        assertTrue("before insert, Zeiss Directory is missing from " +
                   errorMessageSuffix,
                   LsmMetaDataPlugin.hasZeissLsmDirectory(testFilePath));

        DataRow dataRow = new DataRow(new FileTarget(testFile));
        dataRow.addField(new StaticDataModel("test_element",
                                             "value"));
        final RenamePluginDataRow renameRow =
                new RenamePluginDataRow(testFile, dataRow, null);

        plugin.insertMetaData(renameRow);

        assertTrue("after insert Zeiss Directory is missing from " +
                   errorMessageSuffix,
                   LsmMetaDataPlugin.hasZeissLsmDirectory(testFilePath));

        if (srcFile.length() < Integer.MAX_VALUE) {
            String metaData =
                    LsmMetaDataPlugin.readMetaData(testFilePath);

            final String expectedMetaData =
                    "<janeliaMetadata>\n" +
                    "  <test_element>value</test_element>\n" +
                    "</janeliaMetadata>\n";

            assertEquals("invalid meta data stored in " +
                         errorMessageSuffix,
                         expectedMetaData, metaData);
        }
    }
    
}