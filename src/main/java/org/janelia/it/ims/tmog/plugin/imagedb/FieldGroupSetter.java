/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.DataFieldGroupModel;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.utils.StringUtil;

import java.util.List;

/**
 * This class adds indexed image properties for all fields within a field group.
 *
 * @author Eric Trautman
 */
public class FieldGroupSetter implements ImagePropertySetter {

    public static final String TYPE_SUFFIX = ":field-group";

    private String propertyType;
    private String fieldName;

    public FieldGroupSetter(String propertyType,
                            String fieldName) {
        final int prefixLength = propertyType.length() - TYPE_SUFFIX.length();
        this.propertyType = propertyType.substring(0, prefixLength);
        this.fieldName = fieldName;
    }

    public void setProperty(PluginDataRow row,
                            Image image) {
        DataField field = row.getDataField(fieldName);
        if (field instanceof DataFieldGroupModel) {
            DataFieldGroupModel groupModel = (DataFieldGroupModel) field;
            String propertyNamePrefix;
            String propertyName;
            int groupRowIndex = 1;
            for (List<DataField> groupRow : groupModel.getFieldRows()) {
                // NOTE: separator must not be '-'
                //       generation of views (e.g. image_property_vw) may
                //       fail otherwise.
                propertyNamePrefix = propertyType + "_" + groupRowIndex + "_";
                for (DataField groupField : groupRow) {
                    propertyName =
                            propertyNamePrefix +
                            StringUtil.getXmlElementName(
                                    groupField.getDisplayName());
                    image.addProperty(propertyName, groupField.getCoreValue());
                }
                groupRowIndex++;
            }
        }
    }

    public static boolean isFieldGroupType(String propertyType) {
        return ((propertyType != null) && propertyType.endsWith(TYPE_SUFFIX));
    }
}