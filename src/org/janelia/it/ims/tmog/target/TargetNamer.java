/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.target;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class derives desired target names from full names through
 * pattern matching.
 *
 * @author Eric Trautman
 */
public class TargetNamer {

    private Pattern compiledPattern;
    private Integer patternGroupNumber;

    /**
     * Contructs a namer.
     *
     * @param  pattern             pattern for derivation.
     *
     * @param  patternGroupNumber  identifies desired pattern group
     *                             to be used as the target name
     *                             (must be greater than zero).
     *
     * @throws IllegalArgumentException
     *   if the group number is not greater than zero.
     */
    public TargetNamer(String pattern,
                       Integer patternGroupNumber)
            throws IllegalArgumentException {

        this.compiledPattern = Pattern.compile(pattern);
        if ((patternGroupNumber == null) || (patternGroupNumber < 1)) {
            throw new IllegalArgumentException(
                    "pattern group number must be greater than zero");
        }
        this.patternGroupNumber = patternGroupNumber;
    }

    /**
     * @param  fullName  the target's full name.
     *
     * @return a name derived from the specified full name.
     */
    public String getName(String fullName) {
        String name = fullName;
        Matcher matcher = compiledPattern.matcher(fullName);
        if (matcher.matches()) {
            if (matcher.groupCount() >= patternGroupNumber) {
                name = matcher.group(patternGroupNumber);
            }
        }
        return name;
    }

}