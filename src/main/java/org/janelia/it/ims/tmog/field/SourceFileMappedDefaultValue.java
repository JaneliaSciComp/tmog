/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.target.Target;

import java.util.HashMap;
import java.util.Map;

/**
 * This class encapsulates a default field value that is based upon the
 * original name of source file being renamed and a corresponding map.
 * The configured pattern is applied to a source file's name (or path)
 * to derive the map key.  The default value is then set to the mapped
 * value for the key.
 *
 * Configured patterns are expected to contain one and
 * only one "capturing group" that identifies the path fragment to use
 * for the default value.  Java regular expression capturing groups are
 * represented by parentheses in the pattern.
 *
 * @author Eric Trautman
 */
public class SourceFileMappedDefaultValue extends SourceFileDefaultValue {

    private boolean useKeyValueWhenUnmapped;
    private Map<String, String> map;

    @SuppressWarnings({"UnusedDeclaration"})
    public SourceFileMappedDefaultValue() {
        this(null, MatchType.name);
    }

    public SourceFileMappedDefaultValue(String pattern,
                                        MatchType matchType) {
        super(pattern, matchType);
        this.useKeyValueWhenUnmapped = false;
        this.map = new HashMap<String, String>();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void addMappedValue(MappedValue mappedValue) {
        map.put(mappedValue.getFrom(), mappedValue.getTo());
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setUseKeyValueWhenUnmapped(boolean useKeyValueWhenUnmapped) {
        this.useKeyValueWhenUnmapped = useKeyValueWhenUnmapped;
    }

    public String getValue(Target target) {
        String value = null;
        String key = super.getValue(target);
        if (key != null) {
            value = map.get(key);
            if (useKeyValueWhenUnmapped && (value == null)) {
                value = key;
            }
        }
        return value;
    }

    @Override
    public String toString() {
        return "SourceFileMappedDefaultValue{" +
               "matchType=" + getMatchType() +
               ", pattern='" + getPattern() + '\'' +
               ", patternGroupSpec=" + getPatternGroupSpec() +
               ", useKeyValueWhenUnmapped=" + useKeyValueWhenUnmapped +
               ", map=" + map +
               '}';
    }
}