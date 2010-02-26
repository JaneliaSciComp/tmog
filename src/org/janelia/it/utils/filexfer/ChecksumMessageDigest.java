/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.utils.filexfer;

import java.security.MessageDigest;
import java.util.zip.Checksum;

/**
 * This class supports the calculation of message digests based upon the
 * less secure but fast 32 bit {@link java.util.zip.Checksum} algorithms.
 *
 * @author Eric Trautman
 */
public class ChecksumMessageDigest
        extends MessageDigest {

    private Checksum checksum;

    public ChecksumMessageDigest(String algorithmName,
                          Checksum checksum) {
        super(algorithmName);
        this.checksum = checksum;
    }

    @Override
    protected void engineUpdate(byte input) {
        checksum.update(input);
    }

    @Override
    protected void engineUpdate(byte[] input,
                                int offset,
                                int len) {
        checksum.update(input, offset, len);
    }

    @Override
    protected byte[] engineDigest() {
        long l = checksum.getValue();
        byte[] bytes = new byte[4];
        bytes[3] = (byte) ((l & 0xFF000000) >> 24);
        bytes[2] = (byte) ((l & 0x00FF0000) >> 16);
        bytes[1] = (byte) ((l & 0x0000FF00) >> 8);
        bytes[0] = (byte) ((l & 0x000000FF));
        return bytes;
    }

    @Override
    protected void engineReset() {
        checksum.reset();
    }
}