/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config;

import org.janelia.it.ims.tmog.filefilter.LNumberComparator;
import org.janelia.it.ims.tmog.filefilter.NumberComparator;
import org.janelia.it.ims.tmog.filefilter.PathComparator;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.util.Comparator;


/**
 * This class encapsulates configuration information about the
 * input file sorting algorithm.
 *
 * @author Eric Trautman
 */
public class InputFileSorter {

    /** Algorithm name for sorting by numbers. */
    public static final String NUMBER_NAME = "Number";

    /** Comparator for sorting by numbers. */
    public static final Comparator<FileTarget> NUMBER_COMPARATOR =
            new NumberComparator();

    /** Algorithm name for sorting by L-Numbers. */
    public static final String LNUMBER_NAME = "LNumber";

    /** Comparator for sorting by L-Numbers. */
    public static final Comparator<FileTarget> LNUMBER_COMPARATOR =
            new LNumberComparator();

    /** Algorithm name for sorting by path. */
    public static final String PATH_NAME = "Path";

    /** Comparator for sorting by path. */
    public static final Comparator<FileTarget> PATH_COMPARATOR =
            new PathComparator();

    private Comparator<FileTarget> comparator;

    public InputFileSorter() {
        this.comparator = FileTarget.ALPHABETIC_COMPARATOR;
    }

    public Comparator<FileTarget> getComparator() {
        return comparator;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setSortAlgorithm(String algorithmName) {
        if (NUMBER_NAME.equals(algorithmName)) {
            comparator = NUMBER_COMPARATOR;
        } else if (LNUMBER_NAME.equals(algorithmName)) {
            comparator = LNUMBER_COMPARATOR;
        } else if (PATH_NAME.equals(algorithmName)) {
            comparator = PATH_COMPARATOR;
        }
    }
}