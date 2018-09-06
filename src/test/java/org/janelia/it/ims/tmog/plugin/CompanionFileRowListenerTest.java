/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.StaticDataModel;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Tests the {@link CompanionFileRowListener} class.
 *
 * @author Eric Trautman
 */
public class CompanionFileRowListenerTest
        extends TestCase {

    private CompanionFileRowListener rowListener;

    private PluginConfiguration config;

    private int lsmFileCount;
    private File[] testLsmFiles;
    private File[] testCompanionFiles;
    private File[] renamedCompanionFiles;
    private List<RenamePluginDataRow> allRows;

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public CompanionFileRowListenerTest(String name) {
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
        return new TestSuite(CompanionFileRowListenerTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        final String sourceSuffix = ".lsm";
        final String companionSuffix = ".log.csv";

        final File outputDirectory = new File(".");

        allRows = new ArrayList<RenamePluginDataRow>();

        final SimpleDateFormat sdf =
                new SimpleDateFormat("yyyyMMdd-HHmmss-SSS-");
        final String dateString = sdf.format(new Date());

        lsmFileCount = 4;
        testLsmFiles = new File[lsmFileCount];
        testCompanionFiles = new File[lsmFileCount];
        renamedCompanionFiles = new File[lsmFileCount];

        try {
            DataRow dataRow;
            RenamePluginDataRow pluginRow;
            String baseFileName;
            String renamedBaseFileName;
            for (int i = 0; i < lsmFileCount; i++) {
                baseFileName = dateString + i + "-test";
                renamedBaseFileName = baseFileName + "-renamed";
                testLsmFiles[i] = createTestFile(baseFileName +".lsm");


                dataRow = new DataRow(new FileTarget(testLsmFiles[i]));
                dataRow.addField(
                        new StaticDataModel("field-1",
                                            renamedBaseFileName + ".lsm"));
                pluginRow = new RenamePluginDataRow(testLsmFiles[i],
                                                    dataRow,
                                                    outputDirectory);

                // only create a companion log for half of the lsm files
                if (i % 2 == 0) {
                    testCompanionFiles[i] = createTestFile(baseFileName +
                                                           companionSuffix);
                    renamedCompanionFiles[i] = new File(renamedBaseFileName +
                                                        companionSuffix);
                }

                allRows.add(pluginRow);
            }

            config = new PluginConfiguration();
            config.setProperty(
                    CompanionFileRowListener.SOURCE_SUFFIX_PROPERTY,
                    sourceSuffix);
            config.setProperty(
                    CompanionFileRowListener.COMPANION_SUFFIX_PROPERTY,
                    companionSuffix);
            config.setProperty(
                    CompanionFileRowListener.DELETE_AFTER_RENAME_PROPERTY,
                    "true");

            rowListener = new CompanionFileRowListener();

            rowListener.init(config);

        } catch (Exception e) {
            try {
                tearDown();
            } catch (Exception tde) {
                LOG.warn("failed to tear down test after setup failure", tde);
            }
            throw e;
        }
    }

    @Override
    protected void tearDown() throws Exception {
        deleteTestFiles(testLsmFiles);
        deleteTestFiles(testCompanionFiles);
        deleteTestFiles(renamedCompanionFiles);
    }

    public void testPluginWithDelete() throws Exception {
        processRowsAndValidateFileChanges(true);
    }

    public void testPluginWithoutDelete() throws Exception {
        config.setProperty(
                CompanionFileRowListener.DELETE_AFTER_RENAME_PROPERTY,
                "false");
        rowListener.init(config);
        processRowsAndValidateFileChanges(false);
    }

    public void testExistingRenamedCompanionFile() throws Exception {
        createTestFile(renamedCompanionFiles[0].getName());
        verifyProcessException(0, "Failed to rename companion file");
    }    

    public void testMissingSourceSuffixPattern() throws Exception {
        Map<String, String> props = config.getProperties();
        props.remove(CompanionFileRowListener.SOURCE_SUFFIX_PROPERTY);
        verifyInitException(
                CompanionFileRowListener.SOURCE_SUFFIX_PROPERTY);
    }

    private File createTestFile(String name) throws Exception {
        File file = new File(name);
        final boolean isFileCreated = file.createNewFile();
        if (! isFileCreated) {
            throw new IllegalStateException(file.getAbsolutePath() +
                                            " already exists");
        }
        return file;
    }

    private void deleteTestFiles(File[] files) throws Exception {
        boolean isFileDeleted;
        for (File file : files) {
            if ((file != null) &&  file.exists()) {
                isFileDeleted = file.delete();
                if (! isFileDeleted) {
                    LOG.warn("failed to delete " + file.getAbsolutePath());
                }
            }
        }
    }

    private void processRowsAndValidateFileChanges(boolean removeOriginalCompanionFiles)
            throws Exception {

        for (RenamePluginDataRow row : allRows) {
            rowListener.processEvent(RowListener.EventType.END_ROW_SUCCESS,
                                     row);
        }

        String lsmName;
        for (int i = 0; i < lsmFileCount; i = i + 2) {
            lsmName = testLsmFiles[i].getAbsolutePath();
            assertNotNull("setup did not record original companion file for " +
                          lsmName,
                          testCompanionFiles[i]);
            assertNotNull("setup did not record renamed companion file for " +
                          lsmName,
                          renamedCompanionFiles[i]);
            assertTrue(renamedCompanionFiles[i].getAbsolutePath() +
                       " was not created for " + lsmName,
                       renamedCompanionFiles[i].exists());
            if (removeOriginalCompanionFiles) {
                assertFalse(testCompanionFiles[i].getAbsolutePath() +
                            " was not removed",
                            testCompanionFiles[i].exists());
            } else {
                assertTrue(testCompanionFiles[i].getAbsolutePath() +
                           " was removed",
                           testCompanionFiles[i].exists());
            }
        }
    }

    private void verifyInitException(String expectedMessageFragment) {
        try {
            rowListener.init(config);
            fail("init call should have failed");
        } catch (ExternalSystemException e) {
            String msg = e.getMessage();
            assertTrue("\nThe init exception message:\n     " + msg +
                       "\ndoes not contain '" + expectedMessageFragment + "'.",
                       msg.contains(expectedMessageFragment));
        }

    }

    private void verifyProcessException(int rowIndex,
                                        String expectedMessageFragment) {
        try {
            rowListener.processEvent(RowListener.EventType.END_ROW_SUCCESS,
                                     allRows.get(rowIndex));
            fail("processEvent call should have failed");
        } catch (Exception e) {
            String msg = e.getMessage();
            assertTrue("\nThe processEvent exception message:\n     " + msg +
                       "\ndoes not contain '" + expectedMessageFragment + "'.",
                       msg.contains(expectedMessageFragment));
        }
    }

    private static final Log LOG =
            LogFactory.getLog(CompanionFileRowListenerTest.class);

}