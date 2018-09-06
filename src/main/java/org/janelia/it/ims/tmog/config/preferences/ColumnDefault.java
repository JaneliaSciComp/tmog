/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config.preferences;

import org.janelia.it.utils.StringUtil;

/**
 * Preference information for a data table column.
 *
 * @author Eric Trautman
 */
public class ColumnDefault
        extends NamedObject {

    private Integer width;
    private ColumnDefaultSet nestedColumnDefaults;

    /**
     * Constructs an empty default.
     */
    public ColumnDefault() {
        this(null);
    }

    /**
     * Constructs a default with the specified name.
     *
     * @param  name  name of the column for this default.
     */
    public ColumnDefault(String name) {
        super(name);
        this.width = null;
        this.nestedColumnDefaults = new ColumnDefaultSet();
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public ColumnDefaultSet getNestedColumnDefaults() {
        return nestedColumnDefaults;
    }

    public boolean hasNestedColumnDefaults() {
        return nestedColumnDefaults.size() > 0;
    }

    /**
     * Adds the specified default to this column's set of nested defaults.
     *
     * @param  nestedColumn  nested column default to add.
     */
    @SuppressWarnings({"UnusedDeclaration"}) // used by Digester in TransmogrifierPreferences load
    public void addColumnDefault(ColumnDefault nestedColumn) {
        this.nestedColumnDefaults.addColumnDefault(nestedColumn);
    }

    /**
     * Adds all of the specified defaults to this column's set of nested
     * defaults.
     *
     * @param  nestedColumns  set of nested column defaults to add.
     */
    public void addAllColumnDefaults(ColumnDefaultSet nestedColumns) {
        this.nestedColumnDefaults.addAllColumnDefaults(nestedColumns);
    }

    /**
     * @return a deep copy (clone) of this default and any of its nested
     *         defaults.
     */
    public ColumnDefault deepCopy() {
        ColumnDefault copy = new ColumnDefault(getName());
        copy.width = width;
        if (hasNestedColumnDefaults()) {
            copy.nestedColumnDefaults = nestedColumnDefaults.deepCopy();
        }
        return copy;
    }

    /**
     * @return this default's width or if null, the sum of the widths of
     *         this default's nested defaults.  If this default is empty
     *         (null width and no nested columns), zero will be returned.
     */
    public int getTotalWidth() {
        int totalWidth = 0;
        if (width != null) {
            totalWidth = width;
        } else if (hasNestedColumnDefaults()) {
            totalWidth = nestedColumnDefaults.getTotalWidth();
        }
        return totalWidth;
    }

    /**
     * Scales this default's width and the widths of its nested defaults
     * by the specified factor.
     *
     * @param  factor  factor by which each width is multiplied.
     */
    public void scale(double factor) {
        double scaledWidth;
        if (width != null) {
            scaledWidth = width * factor;
            width = (int) scaledWidth;
        }
        if (hasNestedColumnDefaults()) {
            nestedColumnDefaults.scale(factor);
        }
    }

    /**
     * @param  indent         indentation string to prepend to all elements.
     * @param  stringBuilder  builder to use for appending element xml strings.
     */
    // TODO: replace this with jaxb annotations whenever we can drop jdk1.5
    protected void appendXml(String indent,
                             StringBuilder stringBuilder) {
        final String name = getName();
        if (StringUtil.isDefined(name) &&
            ((width != null) || hasNestedColumnDefaults())) {
            stringBuilder.append(indent);
            stringBuilder.append("<columnDefault name=\"");
            stringBuilder.append(StringUtil.getDefinedXmlValue(name));
            if (width != null) {
                stringBuilder.append("\" width=\"");
                stringBuilder.append(width);
            }
            if (hasNestedColumnDefaults()) {
                stringBuilder.append("\">\n");
                nestedColumnDefaults.appendXml(indent + "  ", stringBuilder);
                stringBuilder.append(indent);
                stringBuilder.append("</columnDefault>\n");
            } else {
                stringBuilder.append("\"/>\n");
            }
        }
    }
}