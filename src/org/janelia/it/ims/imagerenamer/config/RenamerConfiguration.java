/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.config;

import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.janelia.it.ims.imagerenamer.field.FileExtensionModel;
import org.janelia.it.ims.imagerenamer.field.FileModificationTimeModel;
import org.janelia.it.ims.imagerenamer.field.PluginDataModel;
import org.janelia.it.ims.imagerenamer.field.RunTimeModel;
import org.janelia.it.ims.imagerenamer.field.SeparatorModel;
import org.janelia.it.ims.imagerenamer.field.SourceFileDefaultValue;
import org.janelia.it.ims.imagerenamer.field.ValidValue;
import org.janelia.it.ims.imagerenamer.field.ValidValueModel;
import org.janelia.it.ims.imagerenamer.field.VerifiedNumberModel;
import org.janelia.it.ims.imagerenamer.field.VerifiedTextModel;
import org.janelia.it.ims.imagerenamer.field.VerifiedWellModel;
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
public class RenamerConfiguration {

    /**
     * The logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(RenamerConfiguration.class);

    private List<ProjectConfiguration> projectList;

    public RenamerConfiguration() {
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
     * Utility method to parse the specified config file.
     *
     * @param resourceName name of resource (file) containing xml
     *                     configuration.
     * @throws org.janelia.it.ims.imagerenamer.config.ConfigurationException
     *          if an error occurs parsing the event config file.
     */
    public void load(String resourceName) throws ConfigurationException {

        InputStream stream = null;

        File configFile = new File(resourceName);
        if (!configFile.exists()) {
            String cpName = "/" + resourceName;
            stream = RenamerConfiguration.class.getResourceAsStream(cpName);
            LOG.warn("Loaded configuration from jar file: " + resourceName);
        }

        if (stream == null) {
            try {
                stream = new FileInputStream(configFile);
            } catch (FileNotFoundException e) {
                throw new ConfigurationException(
                        "Unable to find configuration file: " +
                        configFile.getAbsolutePath(), e);
            }
        }

        load(stream, resourceName);

        LOG.info("loaded configuration file: " + resourceName);
    }

    /**
     * Utility method to parse the specified configuration input stream.
     *
     * @param stream       input stream for configuration.
     * @param resourceName name of the input stream source (for logging).
     * @throws ConfigurationException if an error occurs while parsing the configuration data.
     */
    public void load(InputStream stream,
                     String resourceName) throws ConfigurationException {
        Digester digester = new Digester();
        digester.setValidating(false);

        digester.addObjectCreate("renameConfiguration",
                                 ArrayList.class);

        digester.addObjectCreate("renameConfiguration/project",
                                 ProjectConfiguration.class);
        digester.addSetProperties("renameConfiguration/project");
        digester.addSetNext("renameConfiguration/project",
                            "add");

        digester.addObjectCreate("renameConfiguration/project/inputFileFilter",
                                 InputFileFilter.class);
        digester.addSetProperties("renameConfiguration/project/inputFileFilter");
        digester.addSetNext("renameConfiguration/project/inputFileFilter",
                            "setInputFileFilter");

        digester.addObjectCreate("renameConfiguration/project/outputDirectory",
                                 OutputDirectory.class);
        digester.addSetProperties("renameConfiguration/project/outputDirectory");
        digester.addSetNext("renameConfiguration/project/outputDirectory",
                            "setOutputDirectory");

        digester.addObjectCreate("renameConfiguration/project/renamePattern",
                                 RenamePattern.class);
        digester.addSetNext("renameConfiguration/project/renamePattern",
                            "setRenamePattern");

        digester.addObjectCreate("renameConfiguration/project/renamePattern/number",
                                 VerifiedNumberModel.class);
        digester.addSetProperties("renameConfiguration/project/renamePattern/number");
        digester.addSetNext("renameConfiguration/project/renamePattern/number",
                            "add");

        digester.addObjectCreate("renameConfiguration/project/renamePattern/text",
                                 VerifiedTextModel.class);
        digester.addSetProperties("renameConfiguration/project/renamePattern/text");
        digester.addSetNext("renameConfiguration/project/renamePattern/text",
                            "add");

        digester.addObjectCreate("renameConfiguration/project/renamePattern/text/sourceFileDefault",
                                 SourceFileDefaultValue.class);
        digester.addSetProperties("renameConfiguration/project/renamePattern/text/sourceFileDefault");
        digester.addSetNext("renameConfiguration/project/renamePattern/text/sourceFileDefault",
                            "addDefaultValue");

        digester.addObjectCreate("renameConfiguration/project/renamePattern/well",
                                 VerifiedWellModel.class);
        digester.addSetProperties("renameConfiguration/project/renamePattern/well");
        digester.addSetNext("renameConfiguration/project/renamePattern/well",
                            "add");

        digester.addObjectCreate("renameConfiguration/project/renamePattern/separator",
                                 SeparatorModel.class);
        digester.addSetProperties("renameConfiguration/project/renamePattern/separator");
        digester.addSetNext("renameConfiguration/project/renamePattern/separator",
                            "add");

        digester.addObjectCreate("renameConfiguration/project/renamePattern/pluginData",
                                 PluginDataModel.class);
        digester.addSetProperties("renameConfiguration/project/renamePattern/pluginData");
        digester.addSetNext("renameConfiguration/project/renamePattern/pluginData",
                            "add");

        digester.addObjectCreate("renameConfiguration/project/renamePattern/runTime",
                                 RunTimeModel.class);
        digester.addSetProperties("renameConfiguration/project/renamePattern/runTime");
        digester.addSetNext("renameConfiguration/project/renamePattern/runTime",
                            "add");

        digester.addObjectCreate("renameConfiguration/project/renamePattern/fileModificationTime",
                                 FileModificationTimeModel.class);
        digester.addSetProperties("renameConfiguration/project/renamePattern/fileModificationTime");
        digester.addSetNext("renameConfiguration/project/renamePattern/fileModificationTime",
                            "add");

        digester.addObjectCreate("renameConfiguration/project/renamePattern/fileExtension",
                                 FileExtensionModel.class);
        digester.addSetNext("renameConfiguration/project/renamePattern/fileExtension",
                            "add");

        digester.addObjectCreate("renameConfiguration/project/renamePattern/validValueList",
                                 ValidValueModel.class);
        digester.addSetProperties("renameConfiguration/project/renamePattern/validValueList");
        digester.addObjectCreate("renameConfiguration/project/renamePattern/validValueList/validValue",
                                 ValidValue.class);
        digester.addSetProperties("renameConfiguration/project/renamePattern/validValueList/validValue");
        digester.addSetNext("renameConfiguration/project/renamePattern/validValueList/validValue",
                            "addValidValue");
        digester.addSetNext("renameConfiguration/project/renamePattern/validValueList",
                            "add");

        digester.addObjectCreate("renameConfiguration/project/plugins",
                                 PluginFactory.class);
        digester.addSetProperties("renameConfiguration/project/plugins");

        addPlugin("copyListener", digester);
        addPlugin("rowValidator", digester);
        addPlugin("sessionListener", digester);

        digester.addSetNext("renameConfiguration/project/plugins",
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
                    "Failed to access configuration file: " + resourceName, e);
        } catch (SAXException e) {
            throw new ConfigurationException(
                    "Failed parsing configuration file: " + resourceName, e);
        }
    }

    private void addPlugin(String pluginName,
                           Digester digester) {

        final String pluginRoot =
                "renameConfiguration/project/plugins/" + pluginName;
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
