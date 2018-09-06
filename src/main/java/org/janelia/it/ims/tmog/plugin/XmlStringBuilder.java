/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.DataFieldGroupModel;
import org.janelia.it.utils.StringUtil;

import java.util.List;
import java.util.Map;

/**
 * This utility facilitates building an xml string representation
 * for a data row.
 *
 * @author Eric Trautman
 */
public class XmlStringBuilder {

    private String rowElementName;
    private StringBuilder xml;

    /**
     * Constructs a new builder.
     *
     * @param  rowElementName  name of the parent element for each row.
     */
    public XmlStringBuilder(String rowElementName) {
        this.rowElementName = rowElementName;
        this.xml = new StringBuilder(256);
    }

    /**
     * Rebuilds this xml string representation using fields from the
     * specified row along with any additional aggregate fields.
     *
     * @param  row             source row for the xml representation.
     * @param  additionalData  map of element names to tokens for any
     *                         additional aggregate elements (or null
     *                         if no additional data exists).
     */
    public void setRow(PluginDataRow row,
                       Map<String, PropertyTokenList> additionalData) {

        xml.setLength(0);

        final DataRow dataRow = row.getDataRow();

        xml.append("<");
        xml.append(rowElementName);
        xml.append(">\n");

        for (DataField field : dataRow.getFields()) {
            // exclude separator fields from xml
            if (field.getDisplayName() != null) {
                appendFieldXml(field, "  ");
            }
        }

        if ((additionalData != null) && (additionalData.size() > 0)) {
            final Map<String, DataField> nameToFieldMap =
                    row.getDisplayNameToFieldMap();
            PropertyTokenList tokenList;
            List<String> values;
            for (String name : additionalData.keySet()) {
                tokenList = additionalData.get(name);
                values = tokenList.deriveValues(nameToFieldMap, false);
                for (String value : values) {
                    appendXml(name, value, "  ");
                }
            }
        }

        xml.append("</");
        xml.append(rowElementName);
        xml.append(">\n");
    }

    /**
     * @return the xml string representation for the current row.
     */
    @Override
    public String toString() {
        return xml.toString();
    }

    private void appendFieldXml(DataField field,
                                String indent) {
        if (field instanceof DataFieldGroupModel) {
            appendFieldGroupXml((DataFieldGroupModel) field,
                                indent);
        } else {
            appendXml(field.getDisplayName(),
                      field.getCoreValue(),
                      indent);
        }
    }

    private void appendFieldGroupXml(DataFieldGroupModel group,
                                     String indent) {
        final String elementName =
                StringUtil.getXmlElementName(group.getDisplayName());
        final int rowCount = group.getRowCount();
        final int colCount = group.getColumnCount();
        final String groupIndent = indent + "  ";
        Object value;
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            xml.append(indent);
            xml.append("<");
            xml.append(elementName);
            xml.append(">\n");

            for (int colIndex = 0; colIndex < colCount; colIndex++) {
                value = group.getValueAt(rowIndex, colIndex);
                if (value instanceof DataField) {
                    appendFieldXml((DataField) value, groupIndent);
                }
            }

            xml.append(indent);
            xml.append("</");
            xml.append(elementName);
            xml.append(">\n");
        }
    }

    private void appendXml(String name,
                           String value,
                           String indent) {
            final String elementName =
                    StringUtil.getXmlElementName(name);
            xml.append(indent);
            xml.append("<");
            xml.append(elementName);
            xml.append(">");

            xml.append(StringUtil.getDefinedXmlValue(value));

            xml.append("</");
            xml.append(elementName);
            xml.append(">\n");
    }

}