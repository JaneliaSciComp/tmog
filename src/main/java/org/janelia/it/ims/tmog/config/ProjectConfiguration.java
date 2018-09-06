/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config;

import org.janelia.it.ims.tmog.config.output.OutputDirectoryConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.DataFieldGroupModel;
import org.janelia.it.ims.tmog.field.DefaultValue;
import org.janelia.it.ims.tmog.field.DefaultValueList;
import org.janelia.it.ims.tmog.field.DefaultValueModel;
import org.janelia.it.ims.tmog.field.HttpValidValueModel;
import org.janelia.it.ims.tmog.field.PluginDefaultValue;
import org.janelia.it.ims.tmog.field.TargetNameModel;
import org.janelia.it.ims.tmog.plugin.RowListener;
import org.janelia.it.ims.tmog.plugin.RowUpdater;
import org.janelia.it.ims.tmog.plugin.RowValidator;
import org.janelia.it.ims.tmog.plugin.SessionListener;
import org.janelia.it.ims.tmog.view.CollectorView;

import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates all application configuration information
 * for a transmogrifier project.
 *
 * @author Eric Trautman
 */
public class ProjectConfiguration {

    private String name;
    private boolean isDefault;
    private String taskName;
    private boolean copyPreviousButtonVisible;
    private String imageFamilyName;
    private DataFields dataFields;
    private String targetDisplayName;
    private InputFileFilter inputFileFilter;
    private InputFileSorter inputFileSorter;
    private OutputDirectoryConfiguration outputDirectoryConfiguration;
    private FileTransferConfiguration fileTransferConfiguration;
    private PluginFactory pluginFactory;
    private ConfigurationLoader loader;

    public ProjectConfiguration() {
        this.isDefault = false;
        this.copyPreviousButtonVisible = true;
        this.imageFamilyName = null;
        this.dataFields = new DataFields();
        this.inputFileFilter = new InputFileFilter();
        this.inputFileSorter = new InputFileSorter();
        this.outputDirectoryConfiguration = new OutputDirectoryConfiguration();
        this.fileTransferConfiguration = new FileTransferConfiguration();
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

    public boolean isCopyPreviousButtonVisible() {
        return copyPreviousButtonVisible;
    }

    public String getImageFamilyName() {
        return imageFamilyName;
    }

    /**
     * @return a cloned (deep) copy of this project's field configurations.
     */
    public List<DataField> getFieldConfigurations() {
        List<DataField> fields = dataFields.getFields();
        List<DataField> fieldConfigurations = new ArrayList<>(fields.size());
        for (DataField field : fields) {
            fieldConfigurations.add(field.getNewInstance(true));
        }
        return fieldConfigurations;
    }

    public String getTargetDisplayName() {
        return targetDisplayName;
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

    public FileTransferConfiguration getFileTransfer() {
        return fileTransferConfiguration;
    }

    public boolean hasRowUpdaters() {
        final List<RowUpdater> updaters = getRowUpdaters();
        return updaters.size() > 0;
    }

    public List<RowUpdater> getRowUpdaters() {
        List<RowUpdater> updaters;
        if (pluginFactory != null) {
            updaters = pluginFactory.getRowUpdaters();
        } else {
            updaters = new ArrayList<>();
        }
        return updaters;
    }

    public List<RowListener> getRowListeners() {
        List<RowListener> listeners;
        if (pluginFactory != null) {
            listeners = pluginFactory.getRowListeners();
        } else {
            listeners = new ArrayList<>();
        }
        return listeners;
    }

    public List<RowValidator> getRowValidators() {
        List<RowValidator> validators;
        if (pluginFactory != null) {
            validators = pluginFactory.getRowValidators();
        } else {
            validators = new ArrayList<>();
        }
        return validators;
    }

    public List<SessionListener> getSessionListeners() {
        List<SessionListener> listeners;
        if (pluginFactory != null) {
            listeners = pluginFactory.getSessionListeners();
        } else {
            listeners = new ArrayList<>();
        }
        return listeners;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setName(String name) {
        this.name = name;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setCopyPreviousButtonVisible(boolean copyPreviousButtonVisible) {
        this.copyPreviousButtonVisible = copyPreviousButtonVisible;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setImageFamilyName(String imageFamilyName) {
        this.imageFamilyName = imageFamilyName;
    }

    public void setDataFields(DataFields dataFields) {
        this.dataFields = dataFields;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setInputFileFilter(InputFileFilter inputFileFilter) {
        this.inputFileFilter = inputFileFilter;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setInputFileSorter(InputFileSorter inputFileSorter) {
        this.inputFileSorter = inputFileSorter;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setOutputDirectory(OutputDirectoryConfiguration outputDirectoryConfiguration) {
        this.outputDirectoryConfiguration = outputDirectoryConfiguration;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setFileTransfer(FileTransferConfiguration fileTransferConfiguration) {
        this.fileTransferConfiguration = fileTransferConfiguration;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setPluginFactory(PluginFactory pluginFactory) {
        this.pluginFactory = pluginFactory;
    }

    public void setLoader(ConfigurationLoader loader) {
        this.loader = loader;
        pluginFactory.setLoader(loader);
    }

    /**
     * Initializes and verifies the configured project.
     *
     * @throws ConfigurationException if any errors occur.
     */
    public void initializeAndVerify() throws ConfigurationException {

        for (DataField field : dataFields.getFields()) {
            initializeAndVerifyField(field);
        }

        if (targetDisplayName == null) {
            targetDisplayName = "File Name";
        }

        inputFileFilter.verify();
        
        if (outputDirectoryConfiguration == null) {
            throw new ConfigurationException(
                    "The output directory is not defined for the " +
                    name + " project.");
        }
        outputDirectoryConfiguration.verify(name, dataFields.getFields());

        fileTransferConfiguration.verify();

        if (CollectorView.SAGE_TASK_NAME.equals(taskName) && (imageFamilyName == null)) {
            throw new ConfigurationException(
                    "The " + name + " project is a " + CollectorView.SAGE_TASK_NAME +
                    " task but does not have imageFamilyName defined.");
        }

        if (pluginFactory != null) {
            pluginFactory.constructInstances(name);
        }
    }

    private void initializeAndVerifyField(DataField field)
            throws ConfigurationException {

        if (field instanceof DataFieldGroupModel) {
            DataFieldGroupModel group = (DataFieldGroupModel) field;
            for (DataField f : group.getFirstRow()) {
                initializeAndVerifyField(f);
            }
        }

        if (field instanceof DefaultValueModel) {
            DefaultValueModel defaultField = (DefaultValueModel) field;
            constructDefaultValuePluginInstances(
                    defaultField.getDefaultValueList());
        }

        if (field instanceof TargetNameModel) {
            targetDisplayName = field.getDisplayName();
        }

        if (field instanceof HttpValidValueModel) {
            HttpValidValueModel model = (HttpValidValueModel) field;
            try {
                loader.publishMessage("  loading values from " + model.getServiceUrl());
                model.retrieveAndSetValidValues();
            } catch (Exception e) {
                throw new ConfigurationException(e.getMessage(), e);
            }
        }
    }

    public int getNumberOfVisibleFields() {
        return dataFields.getNumberOfVisibleFields();
    }

    private void constructDefaultValuePluginInstances(DefaultValueList defaultList)
            throws ConfigurationException {

        DefaultValue value;
        PluginDefaultValue pluginValue;
        PluginDefaultValueConfiguration config;
        for (int i = 0; i < defaultList.size(); i++) {
            value = defaultList.get(i);
            if (value instanceof PluginDefaultValueConfiguration) {
                config = (PluginDefaultValueConfiguration) value;
                String className = config.getClassName();
                Object newInstance =
                        PluginFactory.constructInstance(className,
                                                        name);
                if (newInstance instanceof PluginDefaultValue) {
                    pluginValue = (PluginDefaultValue) newInstance;
                    loader.publishMessage("  initializing " + className.substring(className.lastIndexOf('.')+1));
                    pluginValue.init(config.getProperties());
                } else {
                    throw new ConfigurationException(
                            "The configured default value plugin class (" +
                            className + ") for the " + name +
                            " project does not implement " +
                            PluginDefaultValue.class.getName() + ".");
                }
                defaultList.set(i, pluginValue);
            }
        }
        
    }

}
