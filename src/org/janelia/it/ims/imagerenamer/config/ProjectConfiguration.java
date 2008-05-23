/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.config;

import org.janelia.it.ims.imagerenamer.config.output.OutputDirectoryConfiguration;
import org.janelia.it.ims.imagerenamer.field.DataField;
import org.janelia.it.ims.imagerenamer.plugin.RowListener;
import org.janelia.it.ims.imagerenamer.plugin.RowValidator;
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
    private String taskName;
    private RenamePattern renamePattern;
    private InputFileFilter inputFileFilter;
    private InputFileSorter inputFileSorter;
    private OutputDirectoryConfiguration outputDirectoryConfiguration;
    private PluginFactory pluginFactory;

    public ProjectConfiguration() {
        this.isDefault = false;
        this.renamePattern = new RenamePattern();
        this.inputFileFilter = new InputFileFilter();
        this.inputFileSorter = new InputFileSorter();
        this.outputDirectoryConfiguration = new OutputDirectoryConfiguration();
    }

    public String getName() {
        return name;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public String getTaskName() {
        return taskName;
    }

    public List<DataField> getFieldConfigurations() {
        return renamePattern.getFields();
    }

    public InputFileFilter getInputFileFilter() {
        return inputFileFilter;
    }

    public InputFileSorter getInputFileSorter() {
        return inputFileSorter;
    }

    public OutputDirectoryConfiguration getOutputDirectory() {
        return outputDirectoryConfiguration;
    }

    public List<RowListener> getRowListeners() {
        List<RowListener> listeners;
        if (pluginFactory != null) {
            listeners = pluginFactory.getRowListeners();
        } else {
            listeners = new ArrayList<RowListener>();
        }
        return listeners;
    }

    public List<RowValidator> getRowValidators() {
        List<RowValidator> validators;
        if (pluginFactory != null) {
            validators = pluginFactory.getRowValidators();
        } else {
            validators = new ArrayList<RowValidator>();
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

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public void setRenamePattern(RenamePattern renamePattern) {
        this.renamePattern = renamePattern;
    }

    public void setInputFileFilter(InputFileFilter inputFileFilter) {
        this.inputFileFilter = inputFileFilter;
    }

    public void setInputFileSorter(InputFileSorter inputFileSorter) {
        this.inputFileSorter = inputFileSorter;
    }

    public void setOutputDirectory(OutputDirectoryConfiguration outputDirectoryConfiguration) {
        this.outputDirectoryConfiguration = outputDirectoryConfiguration;
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
        if (outputDirectoryConfiguration == null) {
            throw new ConfigurationException(
                    "The output directory is not defined for the " +
                    name + " project.");
        }
        outputDirectoryConfiguration.verify(name, renamePattern.getFields());
        if (pluginFactory != null) {
            pluginFactory.constructInstances(name);
        }
    }

    public int getNumberOfEditableFields() {
        return renamePattern.getNumberOfEditableFields();
    }
}
