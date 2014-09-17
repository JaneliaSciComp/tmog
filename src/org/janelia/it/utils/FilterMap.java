/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to parse a configured string into a map that can be used to
 * locate filter strings associated with some key value.
 *
 * The configured filter map string should look something like:
 * <pre>
 *     key1=filter1A,filter1B|key2=filter2A|key3=filter3A,filter3B,filter3C
 * </pre>
 *
 * @author Eric Trautman
 */
public class FilterMap {

    private Map<String, String[]> keyToFiltersMap;

    public FilterMap(String mapString) {
        build(mapString);
    }

    public void build(String mapString) {
        Map<String, String[]> map = new HashMap<String, String[]>();
        final Matcher m = MAP_STRING_PATTERN.matcher(mapString);
        while (m.find() && (m.groupCount() == 4)) {
            addFilters(m.group(2), m.group(3), map);
        }
        this.keyToFiltersMap = map;
    }

    public String[] getFilters(String forKey) {
        return keyToFiltersMap.get(forKey);
    }

    private void addFilters(String forKey,
                            String filterString,
                            Map<String, String[]> toMap) {
        final String[] filters = CSV_PATTERN.split(filterString);
        if (filters.length > 0) {
            toMap.put(forKey, filters);
        }
    }

    private static final Pattern MAP_STRING_PATTERN = Pattern.compile("(([^=]+)=([^\\|]+))(\\|?)");

    private static final Pattern CSV_PATTERN = Pattern.compile(",");
}
