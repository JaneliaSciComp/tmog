/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config;

import org.apache.log4j.Logger;
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

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(InputFileSorter.class);

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
    private String algorithmName;
    private String patternString;
    private int[] patternGroupIndexes;

    public InputFileSorter() {
        this.comparator = FileTarget.ALPHABETIC_COMPARATOR;
        this.algorithmName = null;
        this.patternString = null;
        this.patternGroupIndexes = null;
    }

    public Comparator<FileTarget> getComparator() {
        return comparator;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setSortAlgorithm(String algorithmName) {
        this.algorithmName = algorithmName;
        if (NUMBER_NAME.equals(algorithmName)) {
            setNumberComparator();
        } else if (LNUMBER_NAME.equals(algorithmName)) {
            comparator = LNUMBER_COMPARATOR;
        } else if (PATH_NAME.equals(algorithmName)) {
            comparator = PATH_COMPARATOR;
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setPatternString(String patternString) {
        this.patternString = patternString;
        if (NUMBER_NAME.equals(algorithmName)) {
            setNumberComparator();
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setPatternGroupIndexes(String patternGroupIndexes) {
        String indexNames[] = patternGroupIndexes.split(",");
        if (indexNames.length == 3) {
            try {
                this.patternGroupIndexes = new int[indexNames.length];
                for (int i = 0; i < indexNames.length; i++) {
                    this.patternGroupIndexes[i] = Integer.parseInt(indexNames[i]);
                }
            } catch (NumberFormatException e) {
                this.patternGroupIndexes = null;
                LOG.warn("ignoring invalid pattern group indexes value '" + patternGroupIndexes + "'", e);
            }
        } else {
            LOG.warn("ignoring invalid pattern group indexes value '" + patternGroupIndexes + "'");
        }

        if (NUMBER_NAME.equals(algorithmName)) {
            setNumberComparator();
        }
    }

    private void setNumberComparator() {
        if (patternString == null) {
            comparator = NUMBER_COMPARATOR;
        } else if (patternGroupIndexes == null) {
            comparator = new NumberComparator(patternString);
        } else {
            comparator = new NumberComparator(patternString,
                                              patternGroupIndexes[0],
                                              patternGroupIndexes[1],
                                              patternGroupIndexes[2]);
        }
    }
}