/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config;

import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.JaneliaTransmogrifier;
import org.janelia.it.ims.tmog.config.output.OutputDirectoryConfiguration;
import org.janelia.it.ims.tmog.config.output.Path;
import org.janelia.it.ims.tmog.config.output.RenameFieldValue;
import org.janelia.it.ims.tmog.config.output.SourceFileModificationTime;
import org.janelia.it.ims.tmog.field.CvTermModel;
import org.janelia.it.ims.tmog.field.DataFieldGroupModel;
import org.janelia.it.ims.tmog.field.FileExtensionModel;
import org.janelia.it.ims.tmog.field.FileModificationTimeModel;
import org.janelia.it.ims.tmog.field.FileNameModel;
import org.janelia.it.ims.tmog.field.FileRelativePathModel;
import org.janelia.it.ims.tmog.field.MappedValue;
import org.janelia.it.ims.tmog.field.PluginDataModel;
import org.janelia.it.ims.tmog.field.RunTimeModel;
import org.janelia.it.ims.tmog.field.SourceFileDateDefaultValue;
import org.janelia.it.ims.tmog.field.SourceFileDefaultValue;
import org.janelia.it.ims.tmog.field.SourceFileMappedDefaultValue;
import org.janelia.it.ims.tmog.field.StaticDataModel;
import org.janelia.it.ims.tmog.field.StaticDefaultValue;
import org.janelia.it.ims.tmog.field.TargetNameModel;
import org.janelia.it.ims.tmog.field.TargetPropertyDefaultValue;
import org.janelia.it.ims.tmog.field.ValidValue;
import org.janelia.it.ims.tmog.field.ValidValueModel;
import org.janelia.it.ims.tmog.field.VerifiedDateModel;
import org.janelia.it.ims.tmog.field.VerifiedDecimalModel;
import org.janelia.it.ims.tmog.field.VerifiedIntegerModel;
import org.janelia.it.ims.tmog.field.VerifiedTextModel;
import org.janelia.it.ims.tmog.field.VerifiedWellModel;
import org.janelia.it.ims.tmog.target.XmlTargetDataFile;
import org.janelia.it.utils.PathUtil;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates all application configuration information
 * and supports the loading of that information from an XML file.
 *
 * @author Eric Trautman
 */
public class TransmogrifierConfiguration {

    private static String configFileName = null;

    /**
     * @return the configuration file name.
     */
    public static String getConfigFileName() {
        if (configFileName == null) {
            String fileName = System.getProperty(CONFIG_FILE_PROPERTY_NAME);
            if (fileName != null) {
                String convertedFileName = PathUtil.convertPath(fileName);
                File configFile = new File(convertedFileName);
                configFileName = configFile.getAbsolutePath();
            }
        }
        return configFileName;
    }

    private GlobalConfiguration globalConfiguration;
    private List<ProjectConfiguration> projectList;

    public TransmogrifierConfiguration() {
        this.projectList = new ArrayList<ProjectConfiguration>();
    }

    public GlobalConfiguration getGlobalConfiguration() {
        return globalConfiguration;
    }

    public void addProjectConfiguration(ProjectConfiguration projectConfig) {
        this.projectList.add(projectConfig);
    }

    public List<ProjectConfiguration> getProjectList() {
        return projectList;
    }

    public ProjectConfiguration getDefaultProjectConfiguration() {
        ProjectConfiguration defaultProject = null;
        for (ProjectConfiguration project : projectList) {
            if (project.isDefault()) {
                defaultProject = project;
                break;
            }
        }
        return defaultProject;
    }

    /**
     * Populates the configuration object model from the file identified
     * by the 'configFile' system property.
     *
     * @throws ConfigurationException
     *   if an error occurs locating or parsing the configuration file.
     */
    public void load() throws ConfigurationException {

        String convertedFileName = getConfigFileName();
        if (convertedFileName != null) {
            File configFile = new File(convertedFileName);
            InputStream stream;
            try {
                stream = new FileInputStream(configFile);
            } catch (FileNotFoundException e) {
                throw new ConfigurationException(
                        "Configuration file " + configFileName +
                        " does not exist.  Please verify the configFile " +
                        "property which was specified as '" +
                        System.getProperty(CONFIG_FILE_PROPERTY_NAME) + "'.",
                        e);
            }

            LOG.info("attempting to load configuration from " +
                     configFileName);
            try {
                load(stream);
            } catch (ConfigurationException e) {
                throw new ConfigurationException(
                        e.getMessage() +
                        "  Configuration information was read from " +
                        configFileName + ".", e);
            }
        } else {
            throw new ConfigurationException(
                    "The configFile property has not been specified.  " +
                    "Please use the '-DconfigFile=<path_to_file>' " +
                    "command line option to identify the configuration " +
                    "file location.");
        }
    }

    /**
     * Utility method to parse the specified configuration input stream.
     *
     * @param stream       input stream for configuration.
     * 
     * @throws ConfigurationException
     *   if an error occurs while parsing the configuration data.
     */
    public void load(InputStream stream) throws ConfigurationException {
        Digester digester = new Digester();
        digester.setValidating(false);

        digester.addObjectCreate("transmogrifierConfiguration",
                                 ArrayList.class);

        createSetAndAdd("*/global",
                        GlobalConfiguration.class, digester);
        createSetAndAdd("*/project",
                        ProjectConfiguration.class, digester);

        createSetAndAdd("*/inputFileFilter",
                        InputFileFilter.class,
                        "setInputFileFilter", digester);
        final String xmlPropertyFilePath = "*/inputFileFilter/xmlPropertyFile";
        createSetAndAdd(xmlPropertyFilePath,
                        XmlTargetDataFile.class,
                        "setTargetDataFile", digester);
        final String groupPropertyPath = xmlPropertyFilePath + "/groupProperty";
        digester.addCallMethod(groupPropertyPath,
                               "addRelativeGroupPropertyPath",
                               1);
        digester.addCallParam(groupPropertyPath, 0, "relativePath");
        final String targetPropertyPath =
                xmlPropertyFilePath + "/targetProperty";
        digester.addCallMethod(targetPropertyPath,
                               "addRelativeTargetPropertyPath",
                               1);
        digester.addCallParam(targetPropertyPath, 0, "relativePath");
                
        createSetAndAdd("*/inputFileSorter",
                        InputFileSorter.class,
                        "setInputFileSorter", digester);
        createSetAndAdd("*/outputDirectory",
                        OutputDirectoryConfiguration.class,
                        "setOutputDirectory", digester);
        createSetAndAdd("*/fileTransfer",
                        FileTransferConfiguration.class,
                        "setFileTransfer", digester);
        createSetAndAdd("*/outputDirectory/path",
                        Path.class,
                        "addComponent", digester);
        createSetAndAdd("*/outputDirectory/renameFieldValue",
                        RenameFieldValue.class,
                        "addComponent", digester);
        createSetAndAdd("*/outputDirectory/sourceFileModificationTime",
                        SourceFileModificationTime.class,
                        "addComponent", digester);

        digester.addObjectCreate("*/dataFields", DataFields.class);
        digester.addSetNext("*/dataFields", "setDataFields");

        createSetAndAdd("*/cvTermList",
                        CvTermModel.class, digester);
        createSetAndAdd("*/date",
                        VerifiedDateModel.class, digester);
        createSetAndAdd("*/decimal",
                        VerifiedDecimalModel.class, digester);
        createSetAndAdd("*/fieldGroup",
                        DataFieldGroupModel.class, digester);
        createSetAndAdd("*/fileExtension",
                        FileExtensionModel.class, digester);
        createSetAndAdd("*/fileModificationTime",
                        FileModificationTimeModel.class, digester);
        createSetAndAdd("*/fileName",
                        FileNameModel.class, digester);
        createSetAndAdd("*/fileRelativePath",
                        FileRelativePathModel.class, digester);
        createSetAndAdd("*/number",
                        VerifiedIntegerModel.class, digester);
        createSetAndAdd("*/pluginData",
                        PluginDataModel.class, digester);
        createSetAndAdd("*/runTime",
                        RunTimeModel.class, digester);
        createSetAndAdd("*/separator",
                        StaticDataModel.class, digester);
        createSetAndAdd("*/static",
                        StaticDataModel.class, digester);
        createSetAndAdd("*/targetName",
                        TargetNameModel.class, digester);
        createSetAndAdd("*/text",
                        VerifiedTextModel.class, digester);
        createSetAndAdd("*/validValue",
                        ValidValue.class,
                        "addValidValue", digester);
        createSetAndAdd("*/validValueList",
                        ValidValueModel.class, digester);
        createSetAndAdd("*/well",
                        VerifiedWellModel.class, digester);
        createSetAndAdd("*/mappedValue",
                        MappedValue.class,
                        "addMappedValue", digester);

        createSetAndAddDefault("*/sourceFileDefault",
                               SourceFileDefaultValue.class,
                               digester);
        createSetAndAddDefault("*/staticDefault",
                               StaticDefaultValue.class,
                               digester);
        createSetAndAddDefault("*/sourceFileDateDefault",
                               SourceFileDateDefaultValue.class,
                               digester);
        createSetAndAddDefault("*/sourceFileMappedDefault",
                               SourceFileMappedDefaultValue.class,
                               digester);
        createSetAndAddDefault("*/targetPropertyDefault",
                               TargetPropertyDefaultValue.class,
                               digester);

        final String pluginDefaultPath = "*/pluginDefault";
        createSetAndAddDefault(pluginDefaultPath,
                                      PluginDefaultValueConfiguration.class,
                                      digester);
        final String pluginDefaultPropertyPath =
                pluginDefaultPath + "/property";
        digester.addCallMethod(pluginDefaultPropertyPath, "setProperty", 2);
        digester.addCallParam(pluginDefaultPropertyPath, 0, "name");
        digester.addCallParam(pluginDefaultPropertyPath, 1, "value");

        createSetAndAdd("*/plugins",
                        PluginFactory.class,
                        "setPluginFactory", digester);

        addPlugin("rowListener", digester);
        addPlugin("rowValidator", digester);
        addPlugin("sessionListener", digester);

        try {
            ArrayList parsedList = (ArrayList) digester.parse(stream);
            for (Object element : parsedList) {
                if (element instanceof ProjectConfiguration) {
                    ProjectConfiguration pConfig =
                            (ProjectConfiguration) element;
                    pConfig.initializeAndVerify();
                    this.addProjectConfiguration(pConfig);
                } else if (element instanceof GlobalConfiguration) {
                    globalConfiguration = (GlobalConfiguration) element;
                    globalConfiguration.verify(JaneliaTransmogrifier.VERSION);
                }
            }
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Failed to access configuration information.", e);
        } catch (SAXException e) {
            throw new ConfigurationException(
                    "Failed to parse configuration information.", e);
        }
    }

    private void addPlugin(String pluginName,
                           Digester digester) {

        final String pluginRoot =
                "transmogrifierConfiguration/project/plugins/" + pluginName;
        final String propertyRoot = pluginRoot + "/property";

        digester.addObjectCreate(pluginRoot,
                                 PluginConfiguration.class);
        digester.addSetProperties(pluginRoot);

        digester.addCallMethod(propertyRoot, "setProperty", 2);
        digester.addCallParam(propertyRoot, 0, "name");
        digester.addCallParam(propertyRoot, 1, "value");

        StringBuilder addMethodName = new StringBuilder();
        addMethodName.append("add");
        addMethodName.append(Character.toUpperCase(pluginName.charAt(0)));
        addMethodName.append(pluginName.substring(1));
        addMethodName.append("Plugin");
        digester.addSetNext(pluginRoot,
                            addMethodName.toString());
    }

    private void createSetAndAdd(String path,
                                 Class fieldClass,
                                 String setNextMethodName,
                                 Digester digester) {
        digester.addObjectCreate(path, fieldClass);
        digester.addSetProperties(path);
        digester.addSetNext(path, setNextMethodName);
    }

    private void createSetAndAdd(String path,
                                 Class fieldClass,
                                 Digester digester) {
        createSetAndAdd(path, fieldClass, "add", digester);
    }

    private void createSetAndAddDefault(String path,
                                        Class defaultClass,
                                        Digester digester) {
        createSetAndAdd(path, defaultClass, "addDefaultValue", digester);
    }

    private static final Logger LOG =
            Logger.getLogger(TransmogrifierConfiguration.class);

    private static final String CONFIG_FILE_PROPERTY_NAME = "configFile";    
}
