/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.mwt;

import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Container for the data parsed from a multi-worm tracker .set XML file.
 *
 * @author Eric Trautman
 */
public class SetFile {

    private Map<String, SetFileNamedObject> nameToObjectMap;
    private String absolutePath;
    private List<StimulusTimes> stimulusTimesList;
    private Digester digester;

    public SetFile() {
        this.nameToObjectMap = new HashMap<String, SetFileNamedObject>();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void addObject(SetFileNamedObject object) {
        nameToObjectMap.put(object.getName(), object);
    }

    /**
     * @return the absolute path for this set file.
     */
    public String getAbsolutePath() {
        return absolutePath;
    }

    /**
     * @return list of stimulus times parsed for this set file.
     */
    public List<StimulusTimes> getStimulusTimesList() {
        return stimulusTimesList;
    }

    /**
     * @param  file  multi-worm tracker .set XML file to parse.
     *
     * @throws IllegalArgumentException
     *   if any errors occur during parsing.
     */
    public void parse(File file)
            throws IllegalArgumentException {

        absolutePath = file.getAbsolutePath();
        nameToObjectMap.clear();
        stimulusTimesList = new ArrayList<StimulusTimes>();
        setOrClearDigester();

        digester.push(this);
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            digester.parse(stream);
        } catch (Throwable t) {
            throw new IllegalArgumentException(t);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    LOG.warn("failed to close " + absolutePath, e);
                }
            }
        }

        final String[] names = {"Puff", "Tap", "Custom 1", "Custom 2"};
        for (String name : names) {
            addStimulusTimes(name);
        }

        Collections.sort(stimulusTimesList, StimulusTimes.ONSET_COMPARATOR);
    }

    @Override
    public String toString() {
        return toJson();
    }

    /**
     * @return a JSON representation of this object.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\n ");
        if (nameToObjectMap.size() > 0) {
            final TreeSet<String> sortedNames =
                    new TreeSet<String>(nameToObjectMap.keySet());
            SetFileNamedObject object;
            boolean isFirst = true;
            for (String name : sortedNames) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    sb.append(",\n ");
                }
                object = nameToObjectMap.get(name);
                sb.append(object.toJson());
            }
        }
        sb.append("\n}");
        return sb.toString();
    }

    /**
     * Prints a JSON representation of the parsed data for each specified file.
     *
     * @param  args  list of file names to parse.
     */
    public static void main(String[] args) {
        SetFile setFile = new SetFile();
        File file;
        List<StimulusTimes> timesList;
        for (String fileName : args) {
            file = new File(fileName);
            setFile.parse(file);
            timesList = setFile.getStimulusTimesList();
            System.out.println("\nComponents for " +
                               file.getAbsolutePath() + " are:\n");
            for (StimulusTimes times : timesList) {
                System.out.println(times.toJson());
            }
            System.out.println("\nParsed data is:");
            System.out.println(setFile.toJson());
            System.out.println();
        }
    }

    private void setOrClearDigester() {

        if (digester == null) {

            digester = new Digester();
            digester.setValidating(false);

            final String objectPath = "LVData/Cluster/Cluster";
            digester.addObjectCreate(objectPath,
                                     SetFileNamedObject.class);
            digester.addSetNext(objectPath, "addObject");

            final String arrayPath = "LVData/Cluster/Array";
            digester.addObjectCreate(arrayPath,
                                     SetFileNamedObject.class);
            digester.addSetNext(arrayPath, "addObject");

            final String arrayNamePath = arrayPath + "/Name";
            digester.addCallMethod(arrayNamePath, "addValue", 1);
            digester.addCallParam(arrayNamePath, 0);

            final String namePath = "*/Val";
            digester.addCallMethod(namePath, "addValue", 1);
            digester.addCallParam(namePath, 0);

            final String dimSizePath = "*/Dimsize";
            digester.addCallMethod(dimSizePath, "addDimSize", 1);
            digester.addCallParam(dimSizePath, 0);

        } else {

            digester.clear();

        }
    }

    private String getValue(String name)
            throws IllegalStateException {
        String value = null;
        final SetFileNamedObject object = nameToObjectMap.get(name);
        if (object != null) {
            value = object.getValue();
        }
        return value;
    }

    private List<String> getValues(String name) {
        List<String> values = null;
        final SetFileNamedObject object = nameToObjectMap.get(name);
        if (object != null) {
            values = object.getValues();
        }
        return values;
    }

    /**
     * Examine parsed data for the the specified element name and
     * add associated stimulus time protocol objects to this file's
     * list of times.
     *
     * @param  name  stimulus element name
     *               (e.g. 'Puff', 'Tap', 'Custom 1', or 'Custom 2').
     */
    private void addStimulusTimes(String name) {
        final String groupValue = getValue(name);
        if ("1".equals(groupValue)) {

            List<StimulusTimes> timesList =
                    new ArrayList<StimulusTimes>();

            final String lowerName = name.toLowerCase();

            // To set interval and duration, look at "time between onset" array
            // (array should contain a pair of elements for each stimulus).

            final String onsetName = "time between " + lowerName + " onset";
            final List<String> onsetValues = getValues(onsetName);
            final int totalNumberOfOnsetValues = onsetValues.size();

            if (totalNumberOfOnsetValues > 1) {
                StimulusTimes times;
                for (int i = 0; (i + 1) < totalNumberOfOnsetValues; i = i + 2) {
                    times = new StimulusTimes(name);
                    times.setInterval(onsetValues.get(i));
                    times.setDuration(onsetValues.get(i + 1));
                    timesList.add(times);
                }
            } else {
                LOG.warn(onsetName + " size is " + totalNumberOfOnsetValues +
                         " in " + absolutePath);
            }

            // To set onset and number values, look at delta
            // between each "timing array" element.  If the delta
            // differs, set onset for the next stimulus times object
            // and reset count number.

            final String timingName = lowerName + " timing array";
            final List<String> timingValues = getValues(timingName);
            final int totalNumberOfTimes = timingValues.size();

            if (totalNumberOfTimes > 1) {

                StimulusTimes stimulusTimes = null;
                int componentIndex = 0;
                int numberOfStimulusApplications = 1;
                String previousTimeString;
                BigDecimal previousTime;
                BigDecimal time;
                BigDecimal previousDelta = null;
                BigDecimal delta;
                for (int i = 1; i < timingValues.size(); i = i + 1) {

                    previousTimeString = timingValues.get(i - 1);
                    previousTime = getTimeValue(previousTimeString);
                    time = getTimeValue(timingValues.get(i));
                    delta = time.subtract(previousTime);

                    if ((previousDelta != null) &&
                        (delta.compareTo(previousDelta) == 0)) {

                        numberOfStimulusApplications++;

                    } else {

                        if (stimulusTimes != null) {
                            stimulusTimes.setNumber(
                                    numberOfStimulusApplications);
                        }
                        stimulusTimes = timesList.get(componentIndex);
                        stimulusTimes.setOnset(previousTimeString);
                        numberOfStimulusApplications = 1;
                        componentIndex++;

                    }

                    previousDelta = delta;

                }

                if (stimulusTimes != null) {
                    stimulusTimes.setNumber(numberOfStimulusApplications + 1);
                }

            } else {

                LOG.warn(timingName + " size is " + totalNumberOfTimes +
                         " in " + absolutePath);

            }

            stimulusTimesList.addAll(timesList);
        }
    }

    private BigDecimal getTimeValue(String stringValue) {
        final BigDecimal value = new BigDecimal(stringValue);
        return value.setScale(0, BigDecimal.ROUND_HALF_UP);
    }

    private static final Logger LOG = Logger.getLogger(SetFile.class);
}
