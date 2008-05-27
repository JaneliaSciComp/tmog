/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.config;

import org.janelia.it.ims.tmog.filefilter.AlphabeticComparator;
import org.janelia.it.ims.tmog.filefilter.LNumberComparator;

import java.io.File;
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
    public static final Comparator<File> LNUMBER_COMPARATOR =
            new LNumberComparator();

    /** Comparator for sorting alphbetically. */
    public static final Comparator<File> ALPHABETIC_COMPARATOR =
            new AlphabeticComparator();

    private Comparator<File> comparator;

    public InputFileSorter() {
        this.comparator = ALPHABETIC_COMPARATOR;
    }

    public Comparator<File> getComparator() {
        return comparator;
    }

    public void setSortAlgorithm(String algorithmName) {
        if (LNUMBER_NAME.equals(algorithmName)) {
            comparator = LNUMBER_COMPARATOR;
        }                
    }
}