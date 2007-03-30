/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.config;

import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.janelia.it.ims.imagerenamer.field.VerifiedTextModel;
import org.janelia.it.ims.imagerenamer.field.ValidValue;
import org.janelia.it.ims.imagerenamer.field.ValidValueModel;
import org.janelia.it.ims.imagerenamer.field.SeparatorModel;
import org.janelia.it.ims.imagerenamer.field.RenameField;
import org.janelia.it.ims.imagerenamer.field.RunTimeModel;
import org.janelia.it.ims.imagerenamer.field.FileModificationTimeModel;
import org.janelia.it.ims.imagerenamer.field.VerifiedNumberModel;
import org.janelia.it.ims.imagerenamer.plugin.CopyCompleteListener;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRowValidator;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * This class encapsulates all application configuration information
 * and supports the loading of that information from an XML file.
 *
 * @author Eric Trautman
 */
public class RenameConfiguration {

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(RenameConfiguration.class);

    private RenamePattern renamePattern;
    private OutputDirectory outputDirectory;
    private PluginFactory pluginFactory;

    public RenameConfiguration() {
        this.renamePattern = new RenamePattern();
        this.outputDirectory = new OutputDirectory();
    }

    public RenameConfiguration(RenamePattern renamePattern,
                               OutputDirectory outputDirectory,
                               PluginFactory pluginFactory) {
        this.renamePattern = renamePattern;
        this.outputDirectory = outputDirectory;
        this.pluginFactory = pluginFactory;
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

    public Set<CopyCompleteListener> getCopyCompleteListeners() {
        Set<CopyCompleteListener> listeners;
        if (pluginFactory != null) {
            listeners = pluginFactory.getCopyCompleteListeners();
        } else {
            listeners = new HashSet<CopyCompleteListener>();
        }
        return listeners;
    }

    public Set<RenameFieldRowValidator> getRowValidators() {
        Set<RenameFieldRowValidator> validators;
        if (pluginFactory != null) {
            validators = pluginFactory.getRowValidators();
        } else {
            validators = new HashSet<RenameFieldRowValidator>();
        }
        return validators;
    }

    /**
     * Utility method to parse the specified config file.
     *
     * @param   resourceName   name of resource (file) containing xml
     *                         configuration.
     *
     * @throws ConfigurationException
     *             if an error occurs parsing the event config file.
     */
    public void load(String resourceName) throws ConfigurationException {

        InputStream stream = null;

        File configFile = new File(resourceName);
        if (! configFile.exists()) {
            String cpName = "/" + resourceName;
            stream = RenameConfiguration.class.getResourceAsStream(cpName);
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

        Digester digester = new Digester();
        digester.setValidating(false);

        digester.addObjectCreate("renameConfiguration",
                                 ArrayList.class);

        digester.addObjectCreate("renameConfiguration/outputDirectory",
                                 OutputDirectory.class);
        digester.addSetProperties("renameConfiguration/outputDirectory");
        digester.addSetNext("renameConfiguration/outputDirectory",
                            "add");

        digester.addObjectCreate("renameConfiguration/renamePattern",
                                 RenamePattern.class);
        digester.addSetNext("renameConfiguration/renamePattern",
                            "add");

        digester.addObjectCreate("renameConfiguration/renamePattern/number",
                                 VerifiedNumberModel.class);
        digester.addSetProperties("renameConfiguration/renamePattern/number");
        digester.addSetNext("renameConfiguration/renamePattern/number",
                            "add");

        digester.addObjectCreate("renameConfiguration/renamePattern/text",
                                 VerifiedTextModel.class);
        digester.addSetProperties("renameConfiguration/renamePattern/text");
        digester.addSetNext("renameConfiguration/renamePattern/text",
                            "add");

        digester.addObjectCreate("renameConfiguration/renamePattern/separator",
                                 SeparatorModel.class);
        digester.addSetProperties("renameConfiguration/renamePattern/separator");
        digester.addSetNext("renameConfiguration/renamePattern/separator",
                            "add");

        digester.addObjectCreate("renameConfiguration/renamePattern/runTime",
                                 RunTimeModel.class);
        digester.addSetProperties("renameConfiguration/renamePattern/runTime");
        digester.addSetNext("renameConfiguration/renamePattern/runTime",
                            "add");

        digester.addObjectCreate("renameConfiguration/renamePattern/fileModificationTime",
                                 FileModificationTimeModel.class);
        digester.addSetProperties("renameConfiguration/renamePattern/fileModificationTime");
        digester.addSetNext("renameConfiguration/renamePattern/fileModificationTime",
                            "add");

        digester.addObjectCreate("renameConfiguration/renamePattern/validValueList",
                                 ValidValueModel.class);
        digester.addSetProperties("renameConfiguration/renamePattern/validValueList");
        digester.addObjectCreate("renameConfiguration/renamePattern/validValueList/validValue",
                                 ValidValue.class);
        digester.addSetProperties("renameConfiguration/renamePattern/validValueList/validValue");
        digester.addSetNext("renameConfiguration/renamePattern/validValueList/validValue",
                            "addValidValue");
        digester.addSetNext("renameConfiguration/renamePattern/validValueList",
                            "add");

        digester.addObjectCreate("renameConfiguration/plugins",
                                 PluginFactory.class);
        digester.addSetProperties("renameConfiguration/plugins/copyCompleteListener");

        digester.addObjectCreate("renameConfiguration/plugins/copyCompleteListener",
                                 PluginConfiguration.class);
        digester.addSetProperties("renameConfiguration/plugins/copyCompleteListener");
        digester.addSetNext("renameConfiguration/plugins/copyCompleteListener",
                            "addCopyCompleteListenerPlugin");

        digester.addObjectCreate("renameConfiguration/plugins/rowValidator",
                                 PluginConfiguration.class);
        digester.addSetProperties("renameConfiguration/plugins/rowValidator");
        digester.addSetNext("renameConfiguration/plugins/rowValidator",
                            "addRowValidatorPlugin");

        digester.addSetNext("renameConfiguration/plugins",
                            "add");
        try {
            ArrayList parsedList = (ArrayList) digester.parse(stream);
            for (Object element : parsedList) {
                if (element instanceof OutputDirectory) {
                    outputDirectory = (OutputDirectory) element;
                    outputDirectory.verify();
                } else if (element instanceof RenamePattern) {
                    renamePattern = (RenamePattern) element;
                } else if (element instanceof PluginFactory) {
                    pluginFactory = (PluginFactory) element;
                    pluginFactory.constructInstances();
                }
            }
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Failed to access configuration file: " + resourceName, e);
        } catch (SAXException e) {
            throw new ConfigurationException(
                    "Failed parsing configuration file: " + resourceName, e);
        }

        LOG.info("loaded configuration file: " + resourceName);
    }

    public int getNumberOfEditableFields() {
        return renamePattern.getNumberOfEditableFields();
    }
}
