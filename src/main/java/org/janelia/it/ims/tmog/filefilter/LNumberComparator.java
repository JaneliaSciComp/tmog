/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.filefilter;

/**
 * This comparator will sort file names that contain L-numbers using
 * the numeric order of the L-numbers instead of their alphabetic order.
 *
 * @author Eric Trautman
 */
public class LNumberComparator extends NumberComparator {

    public LNumberComparator() {
        super("(.*)_L(\\d++)_(.*)\\.lsm");
    }

}
