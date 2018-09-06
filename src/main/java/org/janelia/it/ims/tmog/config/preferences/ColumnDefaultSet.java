/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config.preferences;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collection of named column defaults.
 *
 * @author Eric Trautman
 */
public class ColumnDefaultSet {

    private Map<String, ColumnDefault> columnDefaults;

    /**
     * Constructs an empty set.
     */
    public ColumnDefaultSet() {
        this.columnDefaults = new LinkedHashMap<String, ColumnDefault>();
    }

    /**
     * @param  columnName  name of desired column.
     *
     * @return the default for the specified name or null if none exists.
     */
    public ColumnDefault getColumnDefault(String columnName) {
        return columnDefaults.get(columnName);
    }

    /**
     * Adds the specified default to this set.
     *
     * @param  column  default to add.
     */
    public void addColumnDefault(ColumnDefault column) {
        this.columnDefaults.put(column.getName(), column);
    }

    /**
     * Adds all defaults in the specified set to this set.
     *
     * @param  columns  set of defaults to add.
     */
    public void addAllColumnDefaults(ColumnDefaultSet columns) {
        for (ColumnDefault column : columns.columnDefaults.values()) {
            this.columnDefaults.put(column.getName(), column);
        }
    }

    /**
     * @return the number defaults in this set.
     */
    public int size() {
        return columnDefaults.size();
    }

    /**
     * @return a deep copy (clone) of this set.
     */
    public ColumnDefaultSet deepCopy() {
        ColumnDefaultSet setCopy = new ColumnDefaultSet();
        ColumnDefault defaultCopy;
        for (ColumnDefault columnDefault : columnDefaults.values()) {
            defaultCopy = columnDefault.deepCopy();
            setCopy.addColumnDefault(defaultCopy);
        }
        return setCopy;
    }

    /**
     * @return the sum of the widths of this set's defaults.
     *         If this set is empty, zero will be returned.
     */
    public int getTotalWidth() {
        int totalWidth = 0;
        for (ColumnDefault columnDefault : columnDefaults.values()) {
            totalWidth += columnDefault.getTotalWidth();
        }
        return totalWidth;
    }

    /**
     * Scales the widths of this set's defaults by the specified factor.
     *
     * @param  factor  factor by which each width is multiplied.
     */
    public void scale(double factor) {
        for (ColumnDefault columnDefault : columnDefaults.values()) {
            columnDefault.scale(factor);
        }
    }
    
    /**
     * @param  indent         indentation string to prepend to all elements.
     * @param  stringBuilder  builder to use for appending element xml strings.
     */
    // TODO: replace this with jaxb annotations whenever we can drop jdk1.5
    protected void appendXml(String indent,
                             StringBuilder stringBuilder) {
        for (ColumnDefault columnDefault : columnDefaults.values()) {
            columnDefault.appendXml(indent, stringBuilder);
        }
    }
}