/*
 * Copyright © 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.simpsonlab;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.imagerenamer.field.DatePatternField;
import org.janelia.it.ims.imagerenamer.field.RenameField;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRow;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class encapsulates information collected for Simpson Lab images.
 */
public class Image {

    private Integer id;
    private String relativePath;
    private Date captureDate;
    private String family;
    private ArrayList<ImageProperty> properties;

    public Image() {
        init();
    }

    public Image(Line line,
                 RenameFieldRow row) {
        this.relativePath = row.getRelativePath();
        this.setCaptureDate(row.getRenameField(CAPTURE_DATE_NAME));
        this.setFamily(row.getRenameField(FAMILY_NAME));

        init();

        this.properties.add(new ImageProperty(ImageProperty.LINE_NAME,
                                              line.getFullName()));
        String value;
        for (String propertyName : ImageProperty.NAMES) {
            value = row.getCoreValue(propertyName);
            if ((value != null) && (value.length() > 0)) {
                this.properties.add(new ImageProperty(propertyName, value));
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

    public void setCaptureDate(RenameField field) {
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

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public void setFamily(RenameField field) {
        String value = null;
        if (field != null) {
            value = field.getCoreValue();
        }

        if ((value != null) && (value.length() > 0)) {
            setFamily(value);
        }
    }

    public List<ImageProperty> getProperties() {
        return properties;
    }

    public void addProperty(ImageProperty property) {
        this.properties.add(property);
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Image");
        sb.append("{captureDate=").append(captureDate);
        sb.append(", id=").append(id);
        sb.append(", relativePath='").append(relativePath).append('\'');
        sb.append(", family='").append(family).append('\'');
        sb.append(", properties=").append(properties);
        sb.append('}');
        return sb.toString();
    }

    private void init() {
        this.id = null;
        this.properties =
                new ArrayList<ImageProperty>(ImageProperty.NAMES.size());
        this.properties.add(new ImageProperty(ImageProperty.CREATED_BY_NAME,
                                              getUserName()));
    }

    private String getUserName() {
        return System.getProperty("user.name");
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(Image.class);

    private static final String CAPTURE_DATE_NAME = "Capture Date";
    private static final String FAMILY_NAME = "Family";

}