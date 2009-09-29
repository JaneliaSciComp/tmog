/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.config;

import org.janelia.it.ims.tmog.filefilter.LNumberComparator;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.util.Comparator;


/**
 * This class encapsulates configuration information about the
 * input file sorting algorithm.
 *
 * @author Eric Trautman
 */
public class InputFileSorter {

    /** Algrotihm name for sorting by L-Numbers. */
    public static final String LNUMBER_NAME = "LNumber";

    /** Comparator for sorting by L-Numbers. */
    public static final Comparator<FileTarget> LNUMBER_COMPARATOR =
            new LNumberComparator();

    private Comparator<FileTarget> comparator;

    public InputFileSorter() {
        this.comparator = FileTarget.ALPHABETIC_COMPARATOR;
    }

    public Comparator<FileTarget> getComparator() {
        return comparator;
    }

    public void setSortAlgorithm(String algorithmName) {
        if (LNUMBER_NAME.equals(algorithmName)) {
            comparator = LNUMBER_COMPARATOR;
        }                
    }
}