/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
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
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Tests the FileTransferUtil class.
 *
 * @author Eric Trautman
 */
public class FileTransferUtilTest {

    private static final Log LOG =
            LogFactory.getLog(FileTransferUtilTest.class);

    private FileTransferUtil util;
    private int bufferSize;
    private File sourceFile;
    private File targetFile;
    private File nestedTargetFile;

    @Before
    public void setUp() throws Exception {
        bufferSize = 100 * 1024;
        util = new FileTransferUtil(bufferSize, null);
        sourceFile = new File("lib/junit-4.8.1.jar"); // 237,047 bytes
        String targetName = SDF.format(new Date());
        targetFile = new File(targetName + ".test");
        nestedTargetFile = new File(targetName, "target.test");
    }

    @After
    public void tearDown() throws Exception {
        if (targetFile.exists()) {
            LOG.info("deleting " + targetFile.getAbsolutePath());
            //noinspection ResultOfMethodCallIgnored
            targetFile.delete();
        }

        if (nestedTargetFile.exists()) {
            File testDir = nestedTargetFile.getParentFile();
            //noinspection ResultOfMethodCallIgnored
            nestedTargetFile.delete();
            File[] files = testDir.listFiles();
            if ((files == null) || (files.length == 0)) {
                LOG.info("deleting " + testDir.getAbsolutePath());
                //noinspection ResultOfMethodCallIgnored
                testDir.delete();
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidBufferSize() throws Exception {
        new FileTransferUtil(100, null);
    }

    @Test(expected = NoSuchAlgorithmException.class)
    public void testInvalidAlgorithm() throws Exception {
        new FileTransferUtil(bufferSize, "foo");
    }

    @Test
    public void testCalculateDigest() throws Exception {

        DigestBytes digestBytes = util.calculateDigest(sourceFile);
        Assert.assertNull("null digest should be returned " +
                          "when null algorithm is specified",
                          digestBytes);

        MessageDigest validDigest;
        DigestBytes validDigestBytes;
        for (String algorithm : DigestAlgorithms.getValidNames()) {
            util = new FileTransferUtil(bufferSize, algorithm);
            if (DigestAlgorithms.NONE.equals(algorithm)) {
                digestBytes = util.calculateDigest(sourceFile);
                Assert.assertNull("null digest should be returned " +
                                  "when 'none' algorithm is specified",
                                  digestBytes);
            } else {
                digestBytes = util.calculateDigest(sourceFile);

                validDigest = DigestAlgorithms.getMessageDigest(algorithm);
                SafeFileTransfer.recursiveHashValidationHelper(sourceFile,
                                                               validDigest);
                validDigestBytes = new DigestBytes(validDigest.digest());
                Assert.assertEquals("invalid " + algorithm + " digest returned",
                                    validDigestBytes, digestBytes);

                // recalculate to prove util object can be reused
                digestBytes = util.calculateDigest(sourceFile);
                Assert.assertEquals("after reuse, invalid " + algorithm +
                                    " digest returned",
                                    validDigestBytes, digestBytes);
            }
        }

    }

    @Test(expected = IOException.class)
    public void testCopyWithMissingFromFile() throws Exception {
        util.copy(new File("this-file-should-not-exist"), targetFile);
    }

    @Test(expected = IOException.class)
    public void testCopyWithExistingToFile() throws Exception {
        util.copy(sourceFile, sourceFile);
    }

    @Test
    public void testCopyWithoutDigest() throws Exception {
        DigestBytes copyDigestBytes = util.copy(sourceFile, targetFile);
        Assert.assertNull(copyDigestBytes);

        util = new FileTransferUtil(bufferSize, AdlerMessageDigest.NAME);
        DigestBytes sourceDigestBytes = util.calculateDigest(sourceFile);
        DigestBytes targetDigestBytes = util.calculateDigest(targetFile);
        Assert.assertEquals("copy and target digests do not match",
                            sourceDigestBytes, targetDigestBytes);
    }

    @Test
    public void testCopy() throws Exception {
        util = new FileTransferUtil(bufferSize, CrcMessageDigest.NAME);
        DigestBytes copyDigestBytes = util.copy(sourceFile, targetFile);
        DigestBytes actualDigestBytes = util.calculateDigest(targetFile);
        Assert.assertEquals("copy and target digests do not match",
                            copyDigestBytes, actualDigestBytes);

        util = new FileTransferUtil((int)sourceFile.length() + 100,
                                    DigestAlgorithms.MD5);
        copyDigestBytes = util.copy(sourceFile, nestedTargetFile);
        actualDigestBytes = util.calculateDigest(nestedTargetFile);
        Assert.assertEquals("copy and nested target digests do not match",
                            copyDigestBytes, actualDigestBytes);
    }

    @Test
    public void testCopyAndValidateWithoutValidate() throws Exception {
        util.copyAndValidate(sourceFile, targetFile, false);

        util = new FileTransferUtil(bufferSize, DigestAlgorithms.SHA1);
        util.copyAndValidate(sourceFile, nestedTargetFile, false);
    }

    @Test
    public void testCopyAndValidate() throws Exception {
        util = new FileTransferUtil(bufferSize, DigestAlgorithms.SHA1);
        util.copyAndValidate(sourceFile, targetFile, true);
    }

    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("'transfer-test-'yyyyMMddHHmmssSSS");
}