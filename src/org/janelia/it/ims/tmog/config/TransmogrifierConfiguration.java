/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.config;

import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.output.OutputDirectoryConfiguration;
import org.janelia.it.ims.tmog.config.output.Path;
import org.janelia.it.ims.tmog.config.output.RenameFieldValue;
import org.janelia.it.ims.tmog.config.output.SourceFileModificationTime;
import org.janelia.it.ims.tmog.field.FileExtensionModel;
import org.janelia.it.ims.tmog.field.FileModificationTimeModel;
import org.janelia.it.ims.tmog.field.PluginDataModel;
import org.janelia.it.ims.tmog.field.RunTimeModel;
import org.janelia.it.ims.tmog.field.SourceFileDateDefaultValue;
import org.janelia.it.ims.tmog.field.SourceFileDefaultValue;
import org.janelia.it.ims.tmog.field.StaticDataModel;
import org.janelia.it.ims.tmog.field.StaticDefaultValue;
import org.janelia.it.ims.tmog.field.ValidValue;
import org.janelia.it.ims.tmog.field.ValidValueModel;
import org.janelia.it.ims.tmog.field.VerifiedDateModel;
import org.janelia.it.ims.tmog.field.VerifiedDecimalModel;
import org.janelia.it.ims.tmog.field.VerifiedIntegerModel;
import org.janelia.it.ims.tmog.field.VerifiedTextModel;
import org.janelia.it.ims.tmog.field.VerifiedWellModel;
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

    /**
     * The logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(TransmogrifierConfiguration.class);

    private List<ProjectConfiguration> projectList;

    public TransmogrifierConfiguration() {
        this.projectList = new ArrayList<ProjectConfiguration>();
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

        String fileName = System.getProperty("configFile");
        if (fileName != null) {
            String convertedFileName = PathUtil.convertPath(fileName);
            File configFile = new File(convertedFileName);
            InputStream stream;
            try {
                stream = new FileInputStream(configFile);
            } catch (FileNotFoundException e) {
                throw new ConfigurationException(
                        "Configuration file " + configFile.getAbsolutePath() +
                        " does not exist.  Please verify the configFile " +
                        "property which was specified as '" + fileName + "'.",
                        e);
            }
            String absoluteFileName = configFile.getAbsolutePath();
            LOG.info("attempting to load configuration from " +
                     absoluteFileName);
            try {
                load(stream);
            } catch (ConfigurationException e) {
                throw new ConfigurationException(
                        e.getMessage() +
                        "  Configuration information was read from " +
                        absoluteFileName + ".", e);
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

        digester.addObjectCreate("transmogrifierConfiguration/project",
                                 ProjectConfiguration.class);
        digester.addSetProperties("transmogrifierConfiguration/project");
        digester.addSetNext("transmogrifierConfiguration/project",
                            "add");

        digester.addObjectCreate("transmogrifierConfiguration/project/inputFileFilter",
                                 InputFileFilter.class);
        digester.addSetProperties("transmogrifierConfiguration/project/inputFileFilter");
        digester.addSetNext("transmogrifierConfiguration/project/inputFileFilter",
                            "setInputFileFilter");

        digester.addObjectCreate("transmogrifierConfiguration/project/inputFileSorter",
                                 InputFileSorter.class);
        digester.addSetProperties("transmogrifierConfiguration/project/inputFileSorter");
        digester.addSetNext("transmogrifierConfiguration/project/inputFileSorter",
                            "setInputFileSorter");

        digester.addObjectCreate("transmogrifierConfiguration/project/outputDirectory",
                                 OutputDirectoryConfiguration.class);
        digester.addSetProperties("transmogrifierConfiguration/project/outputDirectory");
        digester.addSetNext("transmogrifierConfiguration/project/outputDirectory",
                            "setOutputDirectory");

        digester.addObjectCreate("transmogrifierConfiguration/project/outputDirectory/path",
                                 Path.class);
        digester.addSetProperties("transmogrifierConfiguration/project/outputDirectory/path");
        digester.addSetNext("transmogrifierConfiguration/project/outputDirectory/path",
                            "addComponent");

        digester.addObjectCreate("transmogrifierConfiguration/project/outputDirectory/renameFieldValue",
                                 RenameFieldValue.class);
        digester.addSetProperties("transmogrifierConfiguration/project/outputDirectory/renameFieldValue");
        digester.addSetNext("transmogrifierConfiguration/project/outputDirectory/renameFieldValue",
                            "addComponent");

        digester.addObjectCreate("transmogrifierConfiguration/project/outputDirectory/sourceFileModificationTime",
                                 SourceFileModificationTime.class);
        digester.addSetProperties("transmogrifierConfiguration/project/outputDirectory/sourceFileModificationTime");
        digester.addSetNext("transmogrifierConfiguration/project/outputDirectory/sourceFileModificationTime",
                            "addComponent");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields",
                                 DataFields.class);
        digester.addSetNext("transmogrifierConfiguration/project/dataFields",
                            "setDataFields");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/number",
                                 VerifiedIntegerModel.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/number");
        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/number/sourceFileDefault",
                                 SourceFileDefaultValue.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/number/sourceFileDefault");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/number/sourceFileDefault",
                            "addDefaultValue");
        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/number/staticDefault",
                                 StaticDefaultValue.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/number/staticDefault");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/number/staticDefault",
                            "addDefaultValue");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/number",
                            "add");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/decimal",
                                 VerifiedDecimalModel.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/decimal");
        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/decimal/sourceFileDefault",
                                 SourceFileDefaultValue.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/decimal/sourceFileDefault");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/decimal/sourceFileDefault",
                            "addDefaultValue");
        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/decimal/staticDefault",
                                 StaticDefaultValue.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/decimal/staticDefault");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/decimal/staticDefault",
                            "addDefaultValue");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/decimal",
                            "add");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/text",
                                 VerifiedTextModel.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/text");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/text",
                            "add");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/text/sourceFileDefault",
                                 SourceFileDefaultValue.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/text/sourceFileDefault");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/text/sourceFileDefault",
                            "addDefaultValue");
        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/text/staticDefault",
                                 StaticDefaultValue.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/text/staticDefault");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/text/staticDefault",
                            "addDefaultValue");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/date",
                                 VerifiedDateModel.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/date");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/date",
                            "add");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/date/sourceFileDateDefault",
                                 SourceFileDateDefaultValue.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/date/sourceFileDateDefault");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/date/sourceFileDateDefault",
                            "addDefaultValue");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/well",
                                 VerifiedWellModel.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/well");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/well",
                            "add");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/separator",
                                 StaticDataModel.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/separator");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/separator",
                            "add");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/static",
                                 StaticDataModel.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/static");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/static",
                            "add");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/pluginData",
                                 PluginDataModel.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/pluginData");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/pluginData",
                            "add");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/runTime",
                                 RunTimeModel.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/runTime");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/runTime",
                            "add");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/fileModificationTime",
                                 FileModificationTimeModel.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/fileModificationTime");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/fileModificationTime",
                            "add");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/fileExtension",
                                 FileExtensionModel.class);
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/fileExtension",
                            "add");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/validValueList",
                                 ValidValueModel.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/validValueList");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/validValueList/sourceFileDefault",
                                 SourceFileDefaultValue.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/validValueList/sourceFileDefault");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/validValueList/sourceFileDefault",
                            "addDefaultValue");

        digester.addObjectCreate("transmogrifierConfiguration/project/dataFields/validValueList/validValue",
                                 ValidValue.class);
        digester.addSetProperties("transmogrifierConfiguration/project/dataFields/validValueList/validValue");
        digester.addSetNext("transmogrifierConfiguration/project/dataFields/validValueList/validValue",
                            "addValidValue");

        digester.addSetNext("transmogrifierConfiguration/project/dataFields/validValueList",
                            "add");

        digester.addObjectCreate("transmogrifierConfiguration/project/plugins",
                                 PluginFactory.class);
        digester.addSetProperties("transmogrifierConfiguration/project/plugins");

        addPlugin("rowListener", digester);
        addPlugin("rowValidator", digester);
        addPlugin("sessionListener", digester);

        digester.addSetNext("transmogrifierConfiguration/project/plugins",
                            "setPluginFactory");
        try {
            ArrayList parsedList = (ArrayList) digester.parse(stream);
            for (Object element : parsedList) {
                if (element instanceof ProjectConfiguration) {
                    ProjectConfiguration pConfig =
                            (ProjectConfiguration) element;
                    pConfig.initializeAndVerify();
                    this.addProjectConfiguration(pConfig);
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
}
