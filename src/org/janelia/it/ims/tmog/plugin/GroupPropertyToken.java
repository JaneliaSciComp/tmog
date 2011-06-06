/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.DataFieldGroupModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class encapsulates a list of tokens parsed from
 * a plug-in configuration property for a field group.
 *
 * @author Eric Trautman
 */
public class GroupPropertyToken
        extends PropertyToken {

    private PropertyTokenList rowTokenList;
    private int startIndex;
    private Integer stopIndex;
    private String concatenateValue;
    private boolean sortRowValues;

    /**
     * Constructs a token for a group field.
     * Configuration properties should be used to set different behaviors.
     *
     * The following example checks each line in a group individually:
     * <pre>
     *       property name="serviceUrl" value="http://sage.int.janelia.org/sage-ws/lines/rubin/${GROUP-1:Line}.janelia-sage"
     *       property name="errorMessage" value="There is no Rubin lab line named '${GROUP-1:Line}' in the SAGE database."
     *       property name="GROUP-1:Line:rowFormat" value="GMR_${Plate}${Well}_${Vector ID}_${Landing Site}"
     * </pre>
     *
     * The following example concatenates the lines in a group together:
     * <pre>
     *       property name="line" value="${GROUP:Line}"
     *       property name="GROUP:Line:concatenate" value="_"
     *       property name="GROUP:Line:sort" value="true"
     *       property name="GROUP:Line:row-format" value="GMR_${Plate}${Well}_${Vector ID}_${Landing Site}"
     * </pre>
     *
     * @param  value       the name of the group field.
     * @param  properties  the configuration properties used to set
     *                     the behavior for this token.
     *
     * @throws IllegalArgumentException
     *   if the specified value is improperly formatted or
     *   if any required configuration properties are missing.
     */
    public GroupPropertyToken(String value,
                              Map<String, String> properties)
            throws IllegalArgumentException {

        super(false, trimGroupPrefix(value));

        // TODO: set start and stop index from properties (error check values, stop value should be stop + 1 - exclusive)
        this.startIndex = 0;
        this.stopIndex = null;

        final String concatenatePropertyName = value + ":concatenate";
        this.concatenateValue = properties.get(concatenatePropertyName);

        final String sortPropertyName = value + ":sort";
        this.sortRowValues =
                Boolean.parseBoolean(properties.get(sortPropertyName));

        final String rowFormatPropertyName = value + ":rowFormat";
        final String rowFormat = properties.get(rowFormatPropertyName);
        if ((rowFormat == null) || (rowFormat.length() == 0)) {
            throw new IllegalArgumentException(
                    "Row format property '" + rowFormatPropertyName +
                    "' is missing.");
        }

        this.rowTokenList = new PropertyTokenList(rowFormat, properties);
    }

    /**
     * @param  nameToFieldMap  map of field names to instances for
     *                         value derivation.
     *
     * @return the number of values (rows) in the group field identified
     *         by this token.
     */
    public int getNumberOfValues(Map<String, DataField> nameToFieldMap) {
        int numberOfValues = 0;
        final DataFieldGroupModel fieldGroup = (DataFieldGroupModel)
                nameToFieldMap.get(getValue());
        final List<List<DataField>> groupRows = fieldGroup.getFieldRows();
        final int numberOfRows = groupRows.size();

        if (concatenateValue == null) {

            if (startIndex < numberOfRows) {
                int stop = numberOfRows;
                if ((stopIndex != null) && (stopIndex < numberOfRows)) {
                    stop = stopIndex;
                }
                numberOfValues = stop - startIndex;
            }

        } else if (numberOfRows > 0) {
            numberOfValues = 1;
        }

        return numberOfValues;
    }

    /**
     * @param  nameToFieldMap  map of field names to instances for
     *                         value derivation.
     * 
     * @param  index           the row index for the desired value
     *                         (only relevant for field groups).
     *
     * @return the derived value for this token based upon the specified map.
     *         If the token is literal, the raw value is simply returned.
     */
    @Override
    public String getValue(Map<String, DataField> nameToFieldMap,
                           int index) {

        StringBuilder sb = new StringBuilder();

        final DataFieldGroupModel fieldGroup = (DataFieldGroupModel)
                nameToFieldMap.get(getValue());
        final List<List<DataField>> groupRows = fieldGroup.getFieldRows();

        Map<String, DataField> groupRowFieldMap;
        if (concatenateValue == null) {

            final int currentIndex = startIndex + index;
            if (currentIndex < groupRows.size()) {
                groupRowFieldMap = getFieldMap(groupRows.get(currentIndex));
                for (String s : rowTokenList.deriveValues(groupRowFieldMap,
                                                          false)) {
                    sb.append(s); // should only be one value
                }
            }

        } else {

            int stop = groupRows.size();
            if ((stopIndex != null) && (stopIndex < stop)) {
                stop = stopIndex;
            }

            List<String> rowValues = new ArrayList<String>(groupRows.size());
            List<DataField> groupRow;
            for (int i = startIndex; i < stop; i++) {
                groupRow = groupRows.get(i);
                groupRowFieldMap = getFieldMap(groupRow);
                for (String s : rowTokenList.deriveValues(groupRowFieldMap,
                                                          false)) {
                    rowValues.add(s); // should only be one value
                }
            }

            if (sortRowValues) {
                Collections.sort(rowValues);
            }

            for (int i = 0; i < rowValues.size(); i++) {
                if (i > 0) {
                    sb.append(concatenateValue);
                }
                sb.append(rowValues.get(i));
            }
        }

        return sb.toString();
    }

    private Map<String, DataField> getFieldMap(List<DataField> groupRow) {
        Map<String, DataField> map = new HashMap<String, DataField>();
        for (DataField field : groupRow) {
            map.put(field.getDisplayName(), field);
        }
        return map;
    }

    public static boolean isGroupPropertyToken(String value) {
        return value.startsWith("GROUP");
    }

    private static String trimGroupPrefix(String value)
            throws IllegalArgumentException {
        String trimmedValue;
        Matcher m = GROUP_PATTERN.matcher(value);
        if (m.matches() && (m.groupCount() == 1)) {
            trimmedValue = m.group(1);
        } else {
            throw new IllegalArgumentException(
                    "Group token '" + value + "' is malformed.  " +
                    "It should have the form: '${GROUP-#:field_name}'.");
        }
        return trimmedValue;
    }

    private static final Pattern GROUP_PATTERN =
            Pattern.compile("^GROUP-\\d+:(.+)");
}

