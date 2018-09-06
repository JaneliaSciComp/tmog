/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config;

import org.janelia.it.utils.filexfer.DigestAlgorithms;
import org.janelia.it.utils.filexfer.SafeFileTransfer;

import java.security.NoSuchAlgorithmException;

/**
 * This class encapsulates configuration information about the
 * file transfer process.
 *
 * @author Eric Trautman
 */
public class FileTransferConfiguration {

    private int bufferSize;
    private String digestAlgorithm;
    private boolean validationRequired;
    private boolean nioRequired;

    public FileTransferConfiguration() {
        this.bufferSize = SafeFileTransfer.BUFFER_SIZE;
        this.digestAlgorithm = SafeFileTransfer.DIGEST_ALGORITHM;
        this.validationRequired = true;
        this.nioRequired = false;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;

        // stream transfer util doesn't support configurable buffer sizes
        if ((! nioRequired) && (SafeFileTransfer.BUFFER_SIZE != bufferSize)) {
            nioRequired = true;
        }
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
        if ((digestAlgorithm == null) ||
            DigestAlgorithms.NONE.equalsIgnoreCase(digestAlgorithm)) {
            this.validationRequired = false;
        }

        // stream transfer util doesn't support configurable digest algorithms
        if ((! nioRequired) &&
            (! SafeFileTransfer.DIGEST_ALGORITHM.equalsIgnoreCase(digestAlgorithm))) {
            nioRequired = true;
        }
    }

    public boolean isValidationRequired() {
        return validationRequired;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setValidationRequired(boolean validationRequired) {
        this.validationRequired = validationRequired;
        if (! validationRequired) {
            this.digestAlgorithm = DigestAlgorithms.NONE;
        }
    }

    public boolean isNioRequired() {
        return nioRequired;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setNioRequired(boolean nioRequired) {
        this.nioRequired = nioRequired;
    }

    @Override
    public String toString() {
        return "FileTransferConfiguration{" +
               "bufferSize=" + bufferSize +
               ", digestAlgorithm='" + digestAlgorithm + '\'' +
               ", validationRequired=" + validationRequired +
               ", nioRequired=" + nioRequired +
               '}';
    }

    /**
     * Validates the configured transfer settings.
     *
     * @throws ConfigurationException
     *   if any of the settings are invalid.
     */
    public void verify() throws ConfigurationException {
        if (validationRequired) {
            try {
                DigestAlgorithms.getMessageDigest(digestAlgorithm);
            } catch (NoSuchAlgorithmException e) {
                throw new ConfigurationException(
                        "The configured file transfer digest algorithm '" +
                        digestAlgorithm + "' is not valid.  " +
                        "Accepted values are: " +
                        DigestAlgorithms.getValidNames() + ".",
                        e);
            }
        }

        // this is a bit of a hack to catch wild buffer size values
        final long maxHeap = Runtime.getRuntime().maxMemory();
        final long typicalRequiredForApp = 50 * 1024 * 1024; // 50 Mb
        final long typicalFree = maxHeap - typicalRequiredForApp;
        final int concurrentBuffers = 3;
        final long maxBuffer = typicalFree / concurrentBuffers;
        if (bufferSize > maxBuffer) {
            final long minHeap =
                    (concurrentBuffers * bufferSize) + typicalRequiredForApp;
            throw new ConfigurationException(
                    "The configured file transfer buffer size (" +
                    bufferSize + " bytes) may be too large for the " +
                    "current maximum java memory setting (" + maxHeap +
                    " bytes).  Please decrease the buffer size to something " +
                    "less than " + maxBuffer + " bytes or increase the " +
                    "maximum memory size to something more than " +
                    minHeap + " bytes."
            );
        }
    }
}