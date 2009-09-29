/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.filefilter;

import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This comparator will sort file names that contain L-numbers using
 * the numeric order of the L-numbers instead of their alphabetic order.
 *
 * @author Eric Trautman
 */
public class LNumberComparator implements Comparator<FileTarget> {

    private static final Pattern PATTERN =
            Pattern.compile("(.*)_L(\\d++)_(.*)\\.lsm");

    public int compare(FileTarget o1, FileTarget o2) {

        int compareResult = 0;
        boolean isLNumberInBothFileNames = false;

        final File f1 = o1.getFile();
        final File f2 = o2.getFile();
        final String name1 = f1.getName();
        final String name2 = f2.getName();
        Matcher matcher1 = PATTERN.matcher(name1);

        if (matcher1.matches()) {
            final String prefix1 = matcher1.group(1);
            final int number1 = Integer.parseInt(matcher1.group(2));
            Matcher matcher2 = PATTERN.matcher(name2);
            if (matcher2.matches()) {
                isLNumberInBothFileNames = true;
                final String prefix2 = matcher2.group(1);
                compareResult = prefix1.compareTo(prefix2);
                if (compareResult == 0) {
                    final int number2 = Integer.parseInt(matcher2.group(2));
                    compareResult = number1 - number2;
                    if (compareResult == 0) {
                        final String suffix1 = matcher1.group(3);
                        final String suffix2 = matcher2.group(3);
                        compareResult = suffix1.compareTo(suffix2);
                    }
                }
            }
        }

        if (! isLNumberInBothFileNames) {
            compareResult = name1.compareTo(name2);
        }

        return compareResult;
    }
}
