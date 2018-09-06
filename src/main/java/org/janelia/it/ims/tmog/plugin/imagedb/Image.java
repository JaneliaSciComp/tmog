/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.plugin.PluginDataRow;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class encapsulates information collected for images.
 *
 * @author Eric Trautman
 */
public class Image {

    public static final String CREATED_BY_PROPERTY = "created_by";
    public static final String LINE_PROPERTY = "line";
    public static final String LAB_PROPERTY = "lab";
    
    private Integer id;
    private String relativePath;
    private String previousRelativePath;
    private Date captureDate;
    private String source;
    private String family;
    private boolean display;
    private String baseUrl;
    private String basePath;
    private Map<String, String> propertyTypeToValueMap;
    private boolean isBeingMoved;

    public Image() {
        this.id = null;
        this.propertyTypeToValueMap = new LinkedHashMap<String, String>();
        this.source = "JFRC";
        this.display = true;
        this.isBeingMoved = true;
    }

    public Image(PluginDataRow row,
                 List<ImagePropertySetter> propertySetters,
                 String relativePath,
                 String previousRelativePath,
                 boolean isBeingMoved) {
        this();
        this.relativePath = relativePath;
        this.previousRelativePath = previousRelativePath;
        this.isBeingMoved = isBeingMoved;
        for (ImagePropertySetter propertySetter : propertySetters) {
            propertySetter.setProperty(row, this);
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

    public String getPreviousRelativePath() {
        return previousRelativePath;
    }

    public void setRelativePaths(String relativePath,
                                 String previousRelativePath) {
        this.relativePath = relativePath;
        this.previousRelativePath = previousRelativePath;
    }

    public String getPath() {
        String path = null;
        if (basePath != null) {
            path = basePath + relativePath;
        }
        return path;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getUrl() {
        String url = null;
        if (baseUrl != null) {
            url = baseUrl + relativePath;
        }
        return url;
    }    

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Date getCaptureDate() {
        return captureDate;
    }

    public void setCaptureDate(Date captureDate) {
        this.captureDate = captureDate;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public boolean isDisplay() {
        return display;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    public Map<String, String> getPropertyTypeToValueMap() {
        return propertyTypeToValueMap;
    }

    public Map<String, String> getPropertyTypeToValueMapForSage() {
        Map<String, String> filteredMap =
                new HashMap<String, String>(propertyTypeToValueMap);
        for (String key : propertyTypeToValueMap.keySet()) {
            if (SAGE_FILTERED_PROPERTIES.contains(key)) {
                filteredMap.remove(key);
            }
        }
        return filteredMap;
    }

    public String getCreatedBy() {
        return propertyTypeToValueMap.get(CREATED_BY_PROPERTY);
    }

    public String getLineName() {
        return propertyTypeToValueMap.get(LINE_PROPERTY);
    }

    public String getLabName() {
        return propertyTypeToValueMap.get(LAB_PROPERTY);
    }

    public boolean isRepresentative() {
        return false;
    }

    public void addProperty(String type,
                            String value) {
        if ((value != null) && (value.length() > 0)) {
            propertyTypeToValueMap.put(type, value);
        }
    }

    public boolean isBeingMoved() {
        return isBeingMoved;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Image");
        sb.append("{captureDate=").append(captureDate);
        sb.append(", id=").append(id);
        sb.append(", relativePath='").append(relativePath).append('\'');
        sb.append(", previousRelativePath='").append(previousRelativePath).append('\'');
        sb.append(", url='").append(getUrl()).append('\'');
        sb.append(", path='").append(getPath()).append('\'');
        sb.append(", family='").append(family).append('\'');
        sb.append(", display=").append(display);
        sb.append(", source='").append(source).append('\'');
        sb.append(", isBeingMoved=").append(isBeingMoved);
        sb.append(", properties=").append(propertyTypeToValueMap);
        sb.append('}');
        return sb.toString();
    }

    private static final Set<String> SAGE_FILTERED_PROPERTIES;
    static {
        Set<String> set = new HashSet<String>();
        set.add(CREATED_BY_PROPERTY);
        set.add(LINE_PROPERTY);
        set.add(LAB_PROPERTY);
        SAGE_FILTERED_PROPERTIES = set;
    }
}