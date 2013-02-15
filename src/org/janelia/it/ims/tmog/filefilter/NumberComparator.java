/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.filefilter;

import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This comparator will sort file names that contain numbers using
 * numeric order instead of alphabetic order.
 *
 * @author Eric Trautman
 */
public class NumberComparator implements Comparator<FileTarget> {

    private Pattern pattern;

    public NumberComparator() {
        this("(.*)(\\d++)\\.(.*)");
    }

    /**
     * Constructs a comparator using the specified pattern string.
     *
     * @param  patternString  pattern with 3 groups: the first group
     *                        identifies characters before the number,
     *                        the second group identifies the number,
     *                        and the third group identifies characters
     *                        after the number.
     *
     * @throws IllegalArgumentException
     *   if the specified patternString is invalid.
     */
    public NumberComparator(String patternString)
            throws IllegalArgumentException {
        validatePatternString(patternString);
        this.pattern = Pattern.compile(patternString);
    }

    public int compare(FileTarget o1, FileTarget o2) {

        int compareResult = 0;
        boolean isNumberInBothFileNames = false;

        final File f1 = o1.getFile();
        final File f2 = o2.getFile();
        final String name1 = f1.getName();
        final String name2 = f2.getName();
        Matcher matcher1 = pattern.matcher(name1);

        if (matcher1.matches()) {
            final String prefix1 = matcher1.group(1);
            final int number1 = Integer.parseInt(matcher1.group(2));
            Matcher matcher2 = pattern.matcher(name2);
            if (matcher2.matches()) {
                isNumberInBothFileNames = true;
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

        if (! isNumberInBothFileNames) {
            compareResult = name1.compareTo(name2);
        }

        return compareResult;
    }

    public boolean isNumberInTargetName(FileTarget target) {
        final String name = target.getName();
        final Matcher matcher = pattern.matcher(name);
        return matcher.matches();
    }

    private void validatePatternString(String patternString)
            throws IllegalArgumentException {
        int openCount = 0;
        int closeCount = 0;
        final int len = patternString.length();
        char c;
        for (int i = 0; i < len; i++) {
            c = patternString.charAt(i);
            if (c == '(') {
                openCount++;
            } else if (c == ')') {
                closeCount++;
            }
        }

        if ((openCount < 3) || (closeCount < 3)) {
            throw new IllegalArgumentException(
                    "The patternString must contain 3 groups: " +
                    "the first group identifies characters before the number, " +
                    "the second group identifies the number, and " +
                    "the third group identifies characters after the number.");
        }
    }
}
