/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config.preferences;

import org.janelia.it.utils.StringUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Preference information for a session view.
 *
 * @author Eric Trautman
 */
public class ViewDefault
        extends NamedObject {

    public static final String CURRENT = "current";
    
    private Map<String, PathDefault> pathDefaults;
    private ColumnDefaultSet columnDefaults;

    /**
     * Constructs an empty set of information.
     */
    public ViewDefault() {
        this(null);
    }

    /**
     * Constructs an empty set of information with the specified name.
     *
     * @param  name  name of the view default.
     */
    public ViewDefault(String name) {
        super(name);
        pathDefaults = new LinkedHashMap<String, PathDefault>();
        columnDefaults = new ColumnDefaultSet();
    }

    /**
     * @param  pathDefaultName  name of default to retrieve.
     *
     * @return path default with the specified name or null if none exists.
     */
    public PathDefault getPathDefault(String pathDefaultName) {
        return pathDefaults.get(pathDefaultName);
    }

    /**
     * @return the source path default or null if none exists.
     */
    public PathDefault getSourcePathDefault() {
        return getPathDefault(PathDefault.SOURCE_DIRECTORY);
    }

    /**
     * @return the transfer path default or null if none exists.
     */
    public PathDefault getTransferPathDefault() {
        return getPathDefault(PathDefault.TRANSFER_DIRECTORY);
    }

    /**
     * Adds the specified path default to this view.
     *
     * @param  pathDefault  default to add.
     */
    @SuppressWarnings({"UnusedDeclaration"}) // used by Digester in TransmogrifierPreferences load
    public void addPathDefault(PathDefault pathDefault) {
        pathDefaults.put(pathDefault.getName(), pathDefault);
    }

    /**
     * @return a deep copy (clone) of the set of column defaults for this view.
     */
    public ColumnDefaultSet getColumnDefaultsCopy() {
        return columnDefaults.deepCopy();
    }

    /**
     * Adds the specified column default to this view.
     *
     * @param  columnDefault  default to add.
     */
    @SuppressWarnings({"UnusedDeclaration"}) // used by Digester in TransmogrifierPreferences load
    public void addColumnDefault(ColumnDefault columnDefault) {
        columnDefaults.addColumnDefault(columnDefault);
    }

    /**
     * Sets this view's column default set to a deep copy (clone)
     * of the specified set.
     *
     * @param  columnDefaultSet  set to clone.
     */
    public void deepCopyAndSetColumnDefaults(ColumnDefaultSet columnDefaultSet) {
        columnDefaults = columnDefaultSet.deepCopy();
    }

    /**
     * @return true if this view has any column defaults defined; 
     *         otherwise false.
     */
    public boolean hasColumnDefaults() {
        return (columnDefaults.size() > 0);
    }

    /**
     * @param  indent         indentation string to prepend to all elements.
     * @param  stringBuilder  builder to use for appending element xml strings.
     */
    // TODO: replace this with jaxb annotations whenever we can drop jdk1.5
    protected void appendXml(String indent,
                             StringBuilder stringBuilder) {
        final String name = getName();
        if (StringUtil.isDefined(name)) {
            stringBuilder.append(indent);
            stringBuilder.append("<viewDefault name=\"");
            stringBuilder.append(StringUtil.getDefinedXmlValue(name));
            stringBuilder.append("\">\n");
            final String nestedIndent = indent + "  ";
            for (PathDefault pathDefault : pathDefaults.values()) {
                pathDefault.appendXml(nestedIndent, stringBuilder);
            }
            columnDefaults.appendXml(nestedIndent, stringBuilder);
            stringBuilder.append(indent);
            stringBuilder.append("</viewDefault>\n");
        }
    }
}