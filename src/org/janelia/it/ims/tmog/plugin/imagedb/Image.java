/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.DatePatternField;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This class encapsulates information collected for images.
 */
public class Image {

    private Integer id;
    private String relativePath;
    private Date captureDate;
    private String family;
    private Map<String, String> propertyTypeToValueMap;

    public Image() {
        this.id = null;
        this.propertyTypeToValueMap = new HashMap<String, String>();
    }

    public Image(RenamePluginDataRow row,
                 Map<String, String> nameToTypeMap) {
        this();

        this.relativePath = row.getRelativePath();

        String type;
        for (String name : nameToTypeMap.keySet()) {
            type = nameToTypeMap.get(name);
            if ((type != null) && (type.length() > 0)) {
                if (CAPTURE_DATE_TYPE.equals(type)) {
                    setCaptureDate(row.getDataField(name));
                } else if (FAMILY_TYPE.equals(type)) {
                    setFamily(row.getDataField(name));
                } else if (CREATED_BY_TYPE.equals(type)) {
                    setCreatedBy();
                } else {
                    addProperty(type, row.getCoreValue(name));
                }
            }
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public Date getCaptureDate() {
        return captureDate;
    }

    public void setCaptureDate(Date captureDate) {
        this.captureDate = captureDate;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public Map<String, String> getPropertyTypeToValueMap() {
        return propertyTypeToValueMap;
    }

    public void addProperty(String type,
                            String value) {
        if ((value != null) && (value.length() > 0)) {
            propertyTypeToValueMap.put(type, value);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Image");
        sb.append("{captureDate=").append(captureDate);
        sb.append(", id=").append(id);
        sb.append(", relativePath='").append(relativePath).append('\'');
        sb.append(", family='").append(family).append('\'');
        sb.append(", properties=").append(propertyTypeToValueMap);
        sb.append('}');
        return sb.toString();
    }

    private void setCaptureDate(DataField field) {
        String value = null;
        if (field != null) {
            value = field.getCoreValue();
        }

        if ((value != null) && (value.length() > 0)) {
            if (field instanceof DatePatternField) {
                String pattern = ((DatePatternField) field).getDatePattern();
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                try {
                    captureDate = sdf.parse(value);
                } catch (ParseException e) {
                    LOG.warn("Unable to parse capture date for '" +
                             relativePath +
                             "'.  Continuing processing without the date.", e);
                }
            }
        }
    }

    private void setFamily(DataField field) {
        String value = null;
        if (field != null) {
            value = field.getCoreValue();
        }

        if ((value != null) && (value.length() > 0)) {
            setFamily(value);
        }
    }

    private void setCreatedBy() {
        this.propertyTypeToValueMap.put(CREATED_BY_TYPE,
                                        System.getProperty("user.name"));
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(Image.class);

    private static final String CAPTURE_DATE_TYPE = "capture_date";
    private static final String FAMILY_TYPE = "family";
    private static final String CREATED_BY_TYPE = "created_by";
}