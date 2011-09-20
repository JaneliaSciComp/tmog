/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.mwt;

import java.math.BigDecimal;

/**
 * Time information for a stimulus as defined by
 *
 * <a href="http://wiki/wiki/display/collab/Zlatic+Data+Analysis+Project">
 * http://wiki/wiki/display/collab/Zlatic+Data+Analysis+Project
 * </a>.
 * <br>
 *
 * Specifically, a time specification has the form:
 * <pre>
 *     (onset)s(number)x(duration)s(interval)s
 * </pre>
 *
 * where
 *
 * <ul>
 *     <li>   onset:    beginning of (first) stimulus, in seconds,
 *                      relative to beginning of trial
 *                      (may be floating point)
 *     </li>
 *     <li>   number:   how many applications of stimulus;
 *                      1 = single; >1 = pulses
 *     </li>
 *     <li>   duration: length of each stimulus application in seconds
 *                      (may be floating point)
 *     </li>
 *     <li>   interval: total length of time in seconds between
 *                      the beginning of one stimulus application and
 *                      the beginning of next stimulus application
 *                      (may be floating point)
 *     </li>
 * </ul>
 *
 * @author Eric Trautman
 */
public class StimulusTimes {

    private String stimulusName;
    private String onset;
    private int number;
    private String duration;
    private String interval;

    public StimulusTimes(String stimulusName) {
        this.stimulusName = stimulusName;
    }

    /**
     * Sets the onset value.
     * The specified value is converted from milliseconds to seconds
     * before being saved.
     *
     * @param  onset  relative onset time
     *                (in milliseconds).
     */
    public void setOnset(String onset) {
        this.onset = scaleValue(onset, ONE_THOUSAND);
    }

    /**
     * Sets the number of stimulus applications.
     *
     * @param  number  number of stimulus applications.
     */
    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * Sets the duration value.
     * The specified value is converted from tenths of milliseconds to seconds
     * before being saved.
     *
     * @param  duration  length of each stimulus application
     *                   (in tenths of milliseconds).
     */
    public void setDuration(String duration) {
        this.duration = scaleValue(duration, TEN_THOUSAND);
    }

    /**
     * Sets the interval value.
     * The specified value is converted from tenths of milliseconds to seconds
     * before being saved.
     *
     * @param  interval  total length of time between
     *                   the beginning of one stimulus application and
     *                   the beginning of next stimulus application
     *                   (in tenths of milliseconds).
     */
    public void setInterval(String interval) {
        this.interval = scaleValue(interval, TEN_THOUSAND);
    }

    /**
     * @return the specification string for this set of stimulus times.
     */
    public String getSpecification() {
        StringBuilder sb = new StringBuilder(32);
        sb.append(onset);
        sb.append('s');
        sb.append(number);
        sb.append('x');
        sb.append(duration);
        sb.append('s');
        sb.append(interval);
        sb.append('s');
        return sb.toString();
    }

    @Override
    public String toString() {
        return getSpecification();
    }

    /**
     * @return a JSON representation of this object.
     */
    public String toJson() {
        return "{\"stimulusName\":\"" + stimulusName +
               "\", \"timeSpecification\":\"" + getSpecification() +
               "\"}";
    }

    /**
     * Divide and scale the value, trimming off any trailing insignificant
     * zeros (e.g. '1.200' => '1.2' and '1.000' => '1').
     * The resulting value will have a maximum scale of 3.
     *
     * @param  valueString  value to covert.
     * @param  divisor      divide the specified value by this
     *                      before converting.
     *
     * @return a scaled and trimmed value.
     */
    private String scaleValue(String valueString,
                              BigDecimal divisor) {
        final int scale = 3;
        BigDecimal value = new BigDecimal(valueString);
        value = value.divide(divisor, scale, BigDecimal.ROUND_HALF_UP);

        String scaledValueString = value.toString();

        // trim trailing zeros
        int last = scaledValueString.length() - 1;
        final int decimalPoint = last - scale; // 0.000
        for (; last > decimalPoint; last--) {
            if (scaledValueString.charAt(last) != '0') {
                break;
            }
        }

        if (last == decimalPoint) {
            last--;
        }

        scaledValueString = scaledValueString.substring(0,(last+1));

        return scaledValueString;
    }

    private static final BigDecimal ONE_THOUSAND = new BigDecimal(1000);
    private static final BigDecimal TEN_THOUSAND = new BigDecimal(10000);
}
