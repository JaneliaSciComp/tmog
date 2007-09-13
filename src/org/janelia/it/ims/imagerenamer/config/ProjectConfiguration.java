/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.config;

import org.janelia.it.ims.imagerenamer.field.RenameField;
import org.janelia.it.ims.imagerenamer.plugin.CopyListener;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRowValidator;
import org.janelia.it.ims.imagerenamer.plugin.SessionListener;

import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates all application configuration information
 * for a renamer project.
 *
 * @author Eric Trautman
 */
public class ProjectConfiguration {

    private String name;
    private boolean isDefault;
    private RenamePattern renamePattern;
    private OutputDirectory outputDirectory;
    private PluginFactory pluginFactory;

    public ProjectConfiguration() {
        this.isDefault = false;
        this.renamePattern = new RenamePattern();
        this.outputDirectory = new OutputDirectory();
    }

    public String getName() {
        return name;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public List<RenameField> getFieldConfigurations() {
        return renamePattern.getFields();
    }

    public OutputDirectory getOutputDirectory() {
        return outputDirectory;
    }

    public boolean isOutputDirectoryManuallyChosen() {
        return outputDirectory.isManuallyChosen();
    }

    public List<CopyListener> getCopyListeners() {
        List<CopyListener> listeners;
        if (pluginFactory != null) {
            listeners = pluginFactory.getCopyListeners();
        } else {
            listeners = new ArrayList<CopyListener>();
        }
        return listeners;
    }

    public List<RenameFieldRowValidator> getRowValidators() {
        List<RenameFieldRowValidator> validators;
        if (pluginFactory != null) {
            validators = pluginFactory.getRowValidators();
        } else {
            validators = new ArrayList<RenameFieldRowValidator>();
        }
        return validators;
    }

    public List<SessionListener> getSessionListeners() {
        List<SessionListener> listeners;
        if (pluginFactory != null) {
            listeners = pluginFactory.getSessionListeners();
        } else {
            listeners = new ArrayList<SessionListener>();
        }
        return listeners;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }


    public void setRenamePattern(RenamePattern renamePattern) {
        this.renamePattern = renamePattern;
    }

    public void setOutputDirectory(OutputDirectory outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void setPluginFactory(PluginFactory pluginFactory) {
        this.pluginFactory = pluginFactory;
    }

    /**
     * Initializes and verifies the configured project.
     *
     * @throws ConfigurationException if any errors occur.
     */
    public void initializeAndVerify() throws ConfigurationException {
        if (outputDirectory == null) {
            throw new ConfigurationException(
                    "The output directory is not defined for the " +
                    name + " project.");
        }
        outputDirectory.verify(name);
        if (pluginFactory != null) {
            pluginFactory.constructInstances(name);
        }
    }

    public int getNumberOfEditableFields() {
        return renamePattern.getNumberOfEditableFields();
    }
}
