/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
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
import org.janelia.it.ims.tmog.config.preferences.TransmogrifierPreferences;
import org.janelia.it.ims.tmog.field.CvTermModel;
import org.janelia.it.ims.tmog.field.DataFieldGroupModel;
import org.janelia.it.ims.tmog.field.FileExtensionModel;
import org.janelia.it.ims.tmog.field.FileModificationTimeModel;
import org.janelia.it.ims.tmog.field.FileNameModel;
import org.janelia.it.ims.tmog.field.FileRelativePathModel;
import org.janelia.it.ims.tmog.field.HttpValidValueModel;
import org.janelia.it.ims.tmog.field.LsmDefaultValue;
import org.janelia.it.ims.tmog.field.MappedValue;
import org.janelia.it.ims.tmog.field.PluginDataModel;
import org.janelia.it.ims.tmog.field.RunTimeModel;
import org.janelia.it.ims.tmog.field.SourceFileDateDefaultValue;
import org.janelia.it.ims.tmog.field.SourceFileDefaultValue;
import org.janelia.it.ims.tmog.field.SourceFileMappedDefaultValue;
import org.janelia.it.ims.tmog.field.SourceFileSlideLocationDefaultValue;
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
import org.janelia.it.ims.tmog.filefilter.FileNamePatternFilter;
import org.janelia.it.ims.tmog.target.XmlTargetDataFile;
import org.janelia.it.ims.tmog.view.component.NarrowOptionPane;
import org.janelia.it.ims.tmog.view.component.ProgressPanel;
import org.janelia.it.utils.PathUtil;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads configuration data in a background thread while
 * displaying load progress (and any errors) on the EDT.
 *
 * @author Eric Trautman
 */
public class ConfigurationLoader
        extends SwingWorker<TransmogrifierConfiguration, String> {

    private URL configUrl;
    private TransmogrifierConfiguration config;
    private ConfigurationLoadCompletionHandler completionHandler;

    private Exception loadFailure;

    private JFrame progressFrame;
    private ProgressPanel progressPanel;

    /**
     * Sets up the loader for processing.
     *
     * @param  configUrl          URL for configuration data.
     * @param  completionHandler  (optional) handler to notify when load has completed (or failed).
     */
    public ConfigurationLoader(URL configUrl,
                               ConfigurationLoadCompletionHandler completionHandler) {

        this.configUrl = configUrl;
        this.config = new TransmogrifierConfiguration();
        this.completionHandler = completionHandler;

        progressFrame = new JFrame("Loading Configuration");
        progressFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        progressPanel = new ProgressPanel();
        progressPanel.setOpaque(true);
        progressFrame.setContentPane(progressPanel);
        progressFrame.pack();
        progressFrame.setVisible(true);

        this.addPropertyChangeListener(progressPanel);
    }

    /**
     * Loads configuration data on a background thread.
     *
     * @return the loaded configuration
     *
     * @throws Exception
     *   if the load fails.
     */
    @Override
    protected TransmogrifierConfiguration doInBackground()
            throws Exception {

        InputStream configStream = null;
        try {
            publish("parsing " + configUrl);
            configStream = configUrl.openStream();
            load(configStream);
        } catch (Exception e) {
            LOG.error("doInBackground: load failed", e);
            loadFailure = e;
            throw e;
        } finally {
            if (configStream != null) {
                try {
                    configStream.close();
                } catch (IOException e) {
                    LOG.warn("doInBackground: failed to close configuration stream, ignoring error", e);
                }
            }
        }

        return config;
    }

    @Override
    protected void process(List<String> chunks) {
        for (String message : chunks) {
            progressPanel.addTaskOutput(message);
        }
    }

    @Override
    protected void done() {
        if (loadFailure != null) {
            showConfigurationErrorDialog(progressFrame, loadFailure);
        }

        if (completionHandler != null) {
            if (loadFailure == null) {
                completionHandler.handleConfigurationLoadSuccess(config);
            } else {
                completionHandler.handleConfigurationLoadFailure(loadFailure);
            }
        }

        progressFrame.dispose();
    }

    /**
     * Publishes the specified message for display.
     *
     * @param  message  message to add to progress frame.
     */
    void publishMessage(String message) {
        publish(message);
    }

    /**
     * Utility method to parse the specified configuration input stream.
     *
     * @param stream       input stream for configuration.
     *
     * @throws ConfigurationException
     *   if an error occurs while parsing the configuration data.
     */
    private void load(InputStream stream) throws ConfigurationException {
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
        createSetAndAdd("*/webServiceList",
                        HttpValidValueModel.class, digester);
        createSetAndAdd("*/well",
                        VerifiedWellModel.class, digester);
        createSetAndAdd("*/mappedValue",
                        MappedValue.class,
                        "addMappedValue", digester);

        createSetAndAddDefault("*/lsmDefault",
                               LsmDefaultValue.class,
                               digester);
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
        createSetAndAddDefault("*/sourceFileSlideLocationDefault",
                               SourceFileSlideLocationDefaultValue.class,
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

        addPlugin("rowUpdater", digester);
        addPlugin("rowListener", digester);
        addPlugin("rowValidator", digester);
        addPlugin("sessionListener", digester);

        try {
            ArrayList parsedList = (ArrayList) digester.parse(stream);
            final int totalElementCount = parsedList.size();
            int elementCount = 0;
            double percentComplete;
            for (Object element : parsedList) {
                elementCount++;
                if (element instanceof ProjectConfiguration) {
                    ProjectConfiguration pConfig = (ProjectConfiguration) element;
                    publish("loading '" + pConfig.getName() + "' configuration");
                    pConfig.setLoader(this);
                    pConfig.initializeAndVerify();
                    config.addProjectConfiguration(pConfig);
                } else if (element instanceof GlobalConfiguration) {
                    publish("loading global configuration");
                    GlobalConfiguration globalConfiguration = (GlobalConfiguration) element;
                    globalConfiguration.verify(JaneliaTransmogrifier.getVersion());
                    config.setGlobalConfiguration(globalConfiguration);
                }
                percentComplete = ((double) elementCount * 100) / (double) totalElementCount;
                setProgress((int) percentComplete);
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

        @SuppressWarnings("StringBufferReplaceableByString")
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

    /**
     * @return the configuration data URL.
     */
    public static URL getConfigUrl(String configResource) throws ConfigurationException {
        URL configUrl;
        LOG.info("getConfigUrl: configResource is '" + configResource + "'");

        try {
            if (configResource == null) {
                final File file = selectConfigFile();
                configResource = file.getAbsolutePath();
                configUrl = file.toURI().toURL();
            } else if (configResource.startsWith("http")) {
                configUrl = new URL(configResource);
            } else {
                String convertedFileName = PathUtil.convertPath(configResource);
                File configFile = new File(convertedFileName);

                if (! configFile.exists()) {

                    if (PathUtil.ON_WINDOWS) {

                        final Pattern removalPattern = Pattern.compile("\\.hhmi\\.org");
                        final Matcher m = removalPattern.matcher(convertedFileName);
                        final String alternateFileName = m.replaceFirst("");
                        final File alternateConfigFile = new File(alternateFileName);

                        if (alternateConfigFile.exists()) {
                            final String fullDriveSuffix = ".hhmi.org";
                            final int driveEnd = convertedFileName.indexOf(fullDriveSuffix) + fullDriveSuffix.length();
                            final String expectedDriveName = convertedFileName.substring(0, driveEnd);
                            throw new ConfigurationException(
                                    "The configuration file is expected to be loaded from " +
                                    configFile.getAbsolutePath() +
                                    " but was found instead at " + alternateConfigFile.getAbsolutePath() +
                                    ".  Please map the network drive using the fully qualified name '" +
                                    expectedDriveName + "' to ensure that all paths declared within the " +
                                    "configuration file are properly resolved.");
                        }

                    }

                    final String message = "The default configuration file\n\n" + configFile.getAbsolutePath() +
                                           "\n\ncould not be found.\n\n";
                    NarrowOptionPane.showMessageDialog(null,
                                                       message,
                                                       "Default Configuration Missing",
                                                       JOptionPane.WARNING_MESSAGE);
                    final File file = selectConfigFile();
                    configResource = file.getAbsolutePath();
                    configUrl = file.toURI().toURL();

                } else {
                    configUrl = new File(configFile.getAbsolutePath()).toURI().toURL();
                }

            }
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Failed to load configuration from " +
                                             configResource + ".", e);
        }

        return configUrl;
    }

    public static void showConfigurationErrorDialog(Component parentWindow,
                                                    Exception failure) {
        NarrowOptionPane.showMessageDialog(parentWindow,
                                           failure.getMessage(),
                                           "Configuration Error",
                                           JOptionPane.ERROR_MESSAGE);
    }

    private static File selectConfigFile()
            throws ConfigurationException {

        final String chooserDirectoryName = "configFileChooserDirectory";
        TransmogrifierPreferences tmogPreferences =
                TransmogrifierPreferences.getInstance();

        File chooserDirectory = null;
        if (tmogPreferences.areLoaded()) {
            String preferredDirectory =
                    tmogPreferences.getGlobalPreference(chooserDirectoryName);
            if (preferredDirectory != null) {
                chooserDirectory =
                        new File(PathUtil.convertPath(preferredDirectory));
            }
        }

        final FileNamePatternFilter xmlFileFilter =
                new FileNamePatternFilter(".*\\.xml");

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(xmlFileFilter);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setCurrentDirectory(chooserDirectory);
        fileChooser.setPreferredSize(new Dimension(900, 600));

        fileChooser.showDialog(null, "Select Configuration File");

        final File selectedFile = fileChooser.getSelectedFile();

        if (selectedFile == null) {
            throw new ConfigurationException(
                    "A configuration file must be selected.");
        }
        if (tmogPreferences.areLoaded()) {
            chooserDirectory = fileChooser.getCurrentDirectory();
            tmogPreferences.setGlobalPreference(
                    chooserDirectoryName,
                    chooserDirectory.getAbsolutePath());
            tmogPreferences.save();
        }

        return selectedFile;
    }

    private static final Logger LOG = Logger.getLogger(ConfigurationLoader.class);
}
