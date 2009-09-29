/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.Target;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This model supports capturing the file's base name
 * for processing.
 *
 * @author Eric Trautman
 */
public class FileNameModel implements DataField {

    private String displayName;
    private Integer displayWidth;
    private boolean markedForTask;
    private String patternString;
    private Pattern compiledPattern;
    private Integer patternGroupNumber;
    private boolean visible;
    private String fileName;

    public FileNameModel() {
        this.markedForTask = true;
        this.visible = true;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getDisplayWidth() {
        return displayWidth;
    }

    public void setDisplayWidth(Integer displayWidth) {
        this.displayWidth = displayWidth;
    }

    public boolean isEditable() {
        // NOTE: This will make the value visible in the data table,
        // but it is not really editable since it does not have a renderer.
        return visible;
    }

    public boolean isCopyable() {
        return false;
    }

    public boolean isMarkedForTask() {
        return markedForTask;
    }

    public void setMarkedForTask(boolean markedForTask) {
        this.markedForTask = markedForTask;
    }

    public String getPatternString() {
        return patternString;
    }

    public void setPatternString(String patternString) {
        this.patternString = patternString;
        this.compiledPattern = Pattern.compile(patternString);
    }

    public Integer getPatternGroupNumber() {
        return patternGroupNumber;
    }

    public void setPatternGroupNumber(Integer patternGroupNumber) {
        this.patternGroupNumber = patternGroupNumber;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public FileNameModel getNewInstance(boolean isCloneRequired) {
        FileNameModel instance = new FileNameModel();
        instance.setDisplayName(displayName);
        instance.setDisplayWidth(displayWidth);
        instance.setMarkedForTask(markedForTask);
        // do not set patternString, simply copy already compiled pattern
        instance.compiledPattern = compiledPattern;
        instance.setPatternGroupNumber(patternGroupNumber);
        instance.setVisible(visible);
        // do not copy fileName (must be derived when rename occurs)
        return instance;
    }

    public String getCoreValue() {
        return getFileNameValue();
    }

    public String getFileNameValue() {
        String value = fileName;
        if (value == null) {
            value = "";
        }
        return value;
    }

    public boolean verify() {
        return true;
    }

    public String getErrorMessage() {
        return null;
    }

    /**
     * Initializes this field's value based upon the specified target.
     *
     * @param  target  the target being processed.
     */
    public void initializeValue(Target target) {

        if (target instanceof FileTarget) {
            FileTarget fileTarget = (FileTarget) target;
            File file = fileTarget.getFile();

            if (file != null) {

                fileName = file.getName();

                if ((compiledPattern != null) && (patternGroupNumber != null)) {
                    Matcher matcher = compiledPattern.matcher(fileName);
                    if (matcher.matches()) {
                        if (matcher.groupCount() >= patternGroupNumber) {
                            fileName = matcher.group(patternGroupNumber);
                        }
                    } else {
                        fileName = "";                        
                    }
                }

            }
        }
    }

    @Override
    public String toString() {
        return getFileNameValue();
    }
}