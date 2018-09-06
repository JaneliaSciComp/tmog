/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.filefilter;

import java.io.File;
import java.util.Comparator;

/**
 * This comparator will sort file names in alphabetic order.
 *
 * @author Eric Trautman
 */
public class AlphabeticComparator implements Comparator<File> {

    public int compare(File o1, File o2) {
        String name1 = o1.getName();
        String name2 = o2.getName();
        return name1.compareTo(name2);
    }
}