/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.utils.filexfer;

import java.util.zip.Adler32;

/**
 * This class supports the calculation of message digests using the
 * Adler 32 bit algorithm.
 *
 * @author Eric Trautman
 */
public class AdlerMessageDigest
        extends ChecksumMessageDigest {

    public static final String NAME = "adler32";
    
    public AdlerMessageDigest() {
        super(NAME, new Adler32());
    }
}