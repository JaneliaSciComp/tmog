/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.utils.filexfer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Tests the {@link SafeFileTransfer} class.
 *
 * @author Eric Trautman
 */
public class SafeFileTransferTest {

    private static final Log LOG =
            LogFactory.getLog(SafeFileTransferTest.class);

    private FileTransferUtil validationUtil;
    private File sourceFile;
    private File targetFile;

    @Before
    public void setUp() throws Exception {
        validationUtil =
                new FileTransferUtil(500000,
                                     SafeFileTransfer.DIGEST_ALGORITHM);

        sourceFile = new File("build.gradle");

        String targetName = SDF.format(new Date());
        // need to specify parent path to avoid NPE in copy calls
        targetFile = new File(".", targetName + ".test");
    }

    @After
    public void tearDown() {
        if (targetFile.exists()) {
            LOG.info("deleting " + targetFile.getAbsolutePath());
            //noinspection ResultOfMethodCallIgnored
            targetFile.delete();
        }
    }

    @Test
    public void testCopy() throws Exception {
        SafeFileTransfer.copy(sourceFile, targetFile, false);

        final DigestBytes sourceDigestBytes =
                validationUtil.calculateDigest(sourceFile);
        final DigestBytes copyDigestBytes =
                validationUtil.calculateDigest(targetFile);
        Assert.assertEquals("copy and target digests do not match",
                            sourceDigestBytes, copyDigestBytes);

        SafeFileTransfer.copy(sourceFile, targetFile, true);

        final DigestBytes overwriteCopyDigestBytes =
                validationUtil.calculateDigest(targetFile);
        Assert.assertEquals("overwrite copy and target digests do not match",
                            sourceDigestBytes, overwriteCopyDigestBytes);

        try {
            SafeFileTransfer.copy(sourceFile, targetFile, false);
            Assert.fail("overwrite of exiting file should have caused exception");
        } catch (FileCopyFailedException e) {
            LOG.info("test passed: expected exception thrown for overwrite", e);
        }

        final File nonExistentSourceFile = new File(".", "this-does-not-exist.txt");
        try {
            SafeFileTransfer.copy(nonExistentSourceFile, targetFile, false);
            Assert.fail("copy of non-existent file should have caused exception");
        } catch (FileCopyFailedException e) {
            LOG.info("test passed: expected exception thrown for non-existent file", e);
        }

    }

    @Test
    public void testCalculateDigestRetries() throws Exception {
        // create file during retry attempts to exercise
        // both failure and success code
        Thread fileCreator = new AsynchronousTargetFileCreator();
        fileCreator.start();

        MessageDigest digest =
                MessageDigest.getInstance(SafeFileTransfer.DIGEST_ALGORITHM);

        // calculate digest for target file
        SafeFileTransfer.recursiveHashValidationHelper(targetFile,
                                                       digest,
                                                       1);
        final DigestBytes helperDigestBytes = new DigestBytes(digest.digest());

        final DigestBytes targetDigestBytes =
                validationUtil.calculateDigest(targetFile);

        Assert.assertEquals("helper and target digests do not match",
                            targetDigestBytes, helperDigestBytes);
    }

    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("'transfer-test-'yyyyMMddHHmmssSSS");

    /**
     * Creates the target file after a short rest.
     */
    private class AsynchronousTargetFileCreator extends Thread {
        @Override
        public void run() {
            try {

                LOG.info("AsynchronousTargetFileCreator.run: sleeping");

                try {
                    sleep(FileTransferUtil.DIGEST_CALCULATION_RETRY_WAIT + 200);
                } catch (InterruptedException e) {
                    LOG.warn("failed to sleep before creating " +
                             targetFile.getAbsolutePath(), e);
                }

                LOG.info("AsynchronousTargetFileCreator.run: creating " +
                         targetFile.getAbsolutePath());

                final boolean isCreateSuccessful = targetFile.createNewFile();
                if (! isCreateSuccessful) {
                    throw new IOException("createNewFile returned false");
                }

            } catch (IOException e) {
                LOG.error("failed to create " + targetFile.getAbsolutePath(),
                          e);
            }
        }
    }
}