/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.filefilter;

import java.io.File;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This comparator will sort file names that contain L-numbers using
 * the numeric order of the L-numbers instead of their alphabetic order.
 *
 * @author Eric Trautman
 */
public class LNumberComparator implements Comparator<File> {

    Pattern PATTERN = Pattern.compile("(.*)_L(\\d++)_(.*)\\.lsm");

    public int compare(File o1, File o2) {

        int compareResult = 0;
        boolean isLNumberInBothFileNames = false;

        String name1 = o1.getName();
        String name2 = o2.getName();
        Matcher matcher1 = PATTERN.matcher(name1);

        if (matcher1.matches()) {
            String prefix1 = matcher1.group(1);
            int number1 = Integer.parseInt(matcher1.group(2));
            Matcher matcher2 = PATTERN.matcher(name2);
            if (matcher2.matches()) {
                isLNumberInBothFileNames = true;
                String prefix2 = matcher2.group(1);
                compareResult = prefix1.compareTo(prefix2);
                if (compareResult == 0) {
                    int number2 = Integer.parseInt(matcher2.group(2));
                    compareResult = number1 - number2;
                    if (compareResult == 0) {
                        String suffix1 = matcher1.group(3);
                        String suffix2 = matcher2.group(3);
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
