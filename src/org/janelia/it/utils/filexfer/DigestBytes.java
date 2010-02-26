/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.utils.filexfer;

import java.util.Arrays;

/**
 * Convenience wrapper object for a digest value byte array that simplifies
 * value comparisons and printing.
 *
 * @author Eric Trautman
 */
public class DigestBytes {

    private byte[] value;

    public DigestBytes(byte[] value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (this == o) {
            isEqual = true;
        } else if (o instanceof DigestBytes) {
            DigestBytes that = (DigestBytes) o;
            isEqual = Arrays.equals(value, that.value);
        }
        return isEqual;
    }

    @Override
    public int hashCode() {
        return value != null ? Arrays.hashCode(value) : 0;
    }

    @Override
    public String toString() {
        String str = null;
        if (value != null) {
            StringBuilder sb = new StringBuilder(value.length);
            for (byte b : value) {
                sb.append(b);
            }
            str = sb.toString();
        }
        return str;
    }

}