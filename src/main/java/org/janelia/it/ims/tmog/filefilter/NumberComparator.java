/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
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
    private int primaryGroup;
    private int numberGroup;
    private int secondaryGroup;

    public NumberComparator() {
        this("(.*)(\\d++)\\.(.*)");
    }

    /**
     * Constructs a comparator using the specified pattern string and default group indexes.
     *
     * @param  patternString  pattern with 3 groups.
     *
     * @throws IllegalArgumentException
     *   if the specified patternString is invalid.
     */
    public NumberComparator(String patternString)
            throws IllegalArgumentException {
        this(patternString, 1, 2, 3);
    }

    /**
     * Constructs a comparator using the specified pattern string and group indexes.
     *
     * @param  patternString   pattern with 3 groups.
     * @param  primaryGroup    index of group that identifies common names to be sorted by number.
     * @param  numberGroup     index of group that identifies the number.
     * @param  secondaryGroup  index of group that identifies characters to use for sorting names
     *                         with the same primary name and number.
     *
     * @throws IllegalArgumentException
     *   if the any arguments are invalid.
     */
    public NumberComparator(String patternString,
                            int primaryGroup,
                            int numberGroup,
                            int secondaryGroup)
            throws IllegalArgumentException {

        validatePatternString(patternString);
        this.pattern = Pattern.compile(patternString);

        validateGroupIndexRange("primary", primaryGroup);
        validateGroupIndexRange("number", numberGroup);
        validateGroupIndexRange("secondary", secondaryGroup);

        validateUniqueGroupIndex("primary", primaryGroup, numberGroup);
        validateUniqueGroupIndex("primary", primaryGroup, secondaryGroup);
        validateUniqueGroupIndex("number", numberGroup, secondaryGroup);

        this.primaryGroup = primaryGroup;
        this.numberGroup = numberGroup;
        this.secondaryGroup = secondaryGroup;
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
            final String prefix1 = matcher1.group(primaryGroup);
            final int number1 = Integer.parseInt(matcher1.group(numberGroup));
            Matcher matcher2 = pattern.matcher(name2);
            if (matcher2.matches()) {
                isNumberInBothFileNames = true;
                final String prefix2 = matcher2.group(primaryGroup);
                compareResult = prefix1.compareTo(prefix2);
                if (compareResult == 0) {
                    final int number2 = Integer.parseInt(matcher2.group(numberGroup));
                    compareResult = number1 - number2;
                    if (compareResult == 0) {
                        final String suffix1 = matcher1.group(secondaryGroup);
                        final String suffix2 = matcher2.group(secondaryGroup);
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
                    "a primary group that identifies common names to be sorted by number, " +
                    "a number group that identifies the number, and " +
                    "a secondary group that identifies characters to use for sorting names " +
                    "with the same primary name and number.");
        }
    }

    private void validateGroupIndexRange(String context,
                                         int indexToValidate)
            throws IllegalArgumentException {

        if ((indexToValidate < 1) || (indexToValidate > 3)) {
            throw new IllegalArgumentException("The " + context + " index must be an integer value between 1 and 3.");
        }
    }

    private void validateUniqueGroupIndex(String context,
                                          int index1,
                                          int index2)
            throws IllegalArgumentException {

        if (index1 == index2) {
            throw new IllegalArgumentException("The " + context + " index must differ from all other indexes.");
        }
    }

}
