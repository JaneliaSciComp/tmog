/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config.preferences;

import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.ConfigurationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * All application preference settings.
 *
 * @author Eric Trautman
 */
public class TransmogrifierPreferences {

    private static TransmogrifierPreferences instance =
            new TransmogrifierPreferences();

    public static TransmogrifierPreferences getInstance() {
        return instance;
    }

    /**
     * @param  projectName  project name.
     *
     * @return current user preferences for the specified project or
     *         null if user preferences cannot be managed.
     */
    public static synchronized ProjectPreferences getProjectPreferences(String projectName) {

        ProjectPreferences projectPreferences = null;

        if (instance.areLoaded()) {
            projectPreferences = instance.getPreferences(projectName);

            if (projectPreferences == null) {
                projectPreferences = new ProjectPreferences();
                projectPreferences.setName(projectName);
                instance.addProjectPreferences(projectPreferences);
            }
        }

        return projectPreferences;
    }

    /**
     * @param  projectName  project name.
     *
     * @return the user view preferences for the specified project or
     *         null if they cannot be managed.
     */
    public static synchronized ViewDefault getProjectViewPreferences(String projectName) {
        ViewDefault viewDefault = null;
        ProjectPreferences projectPreferences =
                getProjectPreferences(projectName);
        if (projectPreferences != null) {
            viewDefault = projectPreferences.getViewDefault(ViewDefault.CURRENT,
                                                            true);
        }
        return viewDefault;
    }

    /**
     * Overwrites or adds the specified view preferences to the
     * user preferences for the specified project.
     *
     * @param  projectName  project name.
     * @param  viewDefault  view preferences to update.
     */
    public static void updateProjectViewPreferences(String projectName,
                                                    ViewDefault viewDefault) {
        ProjectPreferences projectPreferences =
                getProjectPreferences(projectName);
        if (projectPreferences != null) {
            projectPreferences.addViewDefault(viewDefault);
        }
    }

    private File preferencesFile;
    private FieldDefaultSet globalPreferences;
    private Map<String, ProjectPreferences> projectNameToPreferencesMap;
    private boolean loaded;

    /**
     * Constructs an unloaded empty instance.
     */
    protected TransmogrifierPreferences() {
        this.globalPreferences = new FieldDefaultSet();
        this.globalPreferences.setName(GLOBAL_PREFERENCES_NAME);
        this.projectNameToPreferencesMap =
                new LinkedHashMap<String, ProjectPreferences>();
        this.loaded = false;
    }

    /**
     * @return the global preference for the specified name or null if it does not exist.
     */
    public String getGlobalPreference(String name) {
        String value = null;
        FieldDefault fieldDefault = globalPreferences.getFieldDefault(name);
        if (fieldDefault != null) {
            value = fieldDefault.getValue();
        }
        return value;
    }

    /**
     * @return true if the global preferences identify the dark color scheme; otherwise false.
     */
    public boolean isDarkColorScheme() {
        return Boolean.parseBoolean(getGlobalPreference(DARK_COLOR_SCHEME));
    }

    /**
     * Sets the specified global preference.
     *
     * @param  name   preference name.
     * @param  value  preference value.
     */
    public void setGlobalPreference(String name,
                                    String value) {
        FieldDefault fieldDefault = new FieldDefault();
        fieldDefault.setName(name);
        fieldDefault.setValue(value);
        globalPreferences.addFieldDefault(fieldDefault);
    }

    /**
     * Sets the global color scheme preference.
     *
     * @param  isDark  if true, indicates the dark color scheme is preferred.
     */
    public void setColorScheme(boolean isDark) {
        setGlobalPreference(DARK_COLOR_SCHEME, String.valueOf(isDark));
    }

    /**
     * @param  projectName  name of project to retrieve.
     *
     * @return the preferences for the specified project or null if none exist.
     */
    public ProjectPreferences getPreferences(String projectName) {
        return projectNameToPreferencesMap.get(projectName);
    }

    /**
     * Adds the specified project preferences to the set managed for
     * the application.
     *
     * @param projectPreferences  project preferences to add.
     */
    public void addProjectPreferences(ProjectPreferences projectPreferences) {
        this.projectNameToPreferencesMap.put(
                projectPreferences.getName(),
                projectPreferences);
    }

    /**
     * @return true if the application preferences have been loaded from disk;
     *         otherwise false.
     */
    public boolean areLoaded() {
        return loaded;
    }

    /**
     * @return the absolute path of the preferences file or null if none exists.
     */
    public String getAbsolutePath() {
        String path = null;
        if (preferencesFile != null) {
            path = preferencesFile.getAbsolutePath();
        }
        return path;
    }

    /**
     * @return an xml representation of this object.
     */
    // TODO: replace this with jaxb annotations whenever we can drop jdk1.5
    public String toXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<transmogrifierPreferences>\n");

        ProjectPreferences globalPreferencesWrapper = new ProjectPreferences();
        globalPreferencesWrapper.setName(GLOBAL_PREFERENCES_NAME);
        globalPreferencesWrapper.addFieldDefaultSet(globalPreferences);
        sb.append(globalPreferencesWrapper.toXml());

        for (ProjectPreferences projectPreferences :
                projectNameToPreferencesMap.values()) {
            sb.append(projectPreferences.toXml());
        }
        sb.append("</transmogrifierPreferences>\n");
        return sb.toString();
    }

    /**
     * Populates the preferences object model from the user's home directory.
     *
     * @throws ConfigurationException
     *   if an error occurs parsing the preferences file.
     */
    public synchronized void load() throws ConfigurationException {

        this.loaded = false;

        final String userHomePath = System.getProperty("user.home");
        preferencesFile = new File(userHomePath, FILE_NAME);
        final String preferencesPath = preferencesFile.getAbsolutePath();

        if (preferencesFile.exists()) {

            if (preferencesFile.canRead()) {

                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(preferencesFile);
                    load(fis);
                    LOG.info("Loaded preferences file " + preferencesPath);
                } catch (Exception e) {
                    String msg = "Failed to load preferences file " +
                                 preferencesPath;
                    throw new ConfigurationException(msg, e);
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            LOG.warn("After load, failed to close " +
                                     preferencesPath);
                        }
                    }
                }

            } else {
                throw new ConfigurationException(
                        "You do not have access to read preference data from " +
                        preferencesPath + ".");
            }

        } else {
            File prefsDir = preferencesFile.getParentFile();
            if ((prefsDir == null) || (! prefsDir.canWrite())) {
                throw new ConfigurationException(
                        "You do not have access to save preference data to " +
                        preferencesPath + ".");
            }
        }

        this.loaded = true;
    }

    // TODO: replace this with jaxb annotations whenever we can drop jdk1.5
    /**
     * Utility method to parse xml preferences from the input stream.
     *
     * @param  stream  input stream for for xml.
     *
     * @throws ConfigurationException
     *   if an error occurs while parsing the preference data.
     */
    public void load(InputStream stream) throws ConfigurationException {

        try {
            Digester digester = new Digester();
            digester.setValidating(false);

            digester.addObjectCreate("transmogrifierPreferences",
                                     ArrayList.class);

            createSetAndAdd("*/projectPreferences",
                            ProjectPreferences.class,
                            "add",
                            digester);

            createSetAndAdd("*/fieldDefaultSet",
                            FieldDefaultSet.class,
                            "addFieldDefaultSet",
                            digester);

            final String fieldDefaultElements = "*/fieldDefault";
            createSetAndAdd(fieldDefaultElements,
                            FieldDefault.class,
                            "addFieldDefault",
                            digester);
            digester.addCallMethod(fieldDefaultElements, "setValue", 0);

            createSetAndAdd("*/viewDefault",
                            ViewDefault.class,
                            "addViewDefault",
                            digester);

            final String pathDefaultElements = "*/pathDefault";
            createSetAndAdd(pathDefaultElements,
                            PathDefault.class,
                            "addPathDefault",
                            digester);
            digester.addCallMethod(pathDefaultElements, "setValue", 0);

            createSetAndAdd("*/columnDefault",
                            ColumnDefault.class,
                            "addColumnDefault",
                            digester);
            
            ArrayList parsedList = (ArrayList) digester.parse(stream);
            if (parsedList != null) {
                ProjectPreferences projectPreferences;
                for (Object element : parsedList) {
                    if (element instanceof ProjectPreferences) {
                        projectPreferences = (ProjectPreferences) element;
                        if (GLOBAL_PREFERENCES_NAME.equals(projectPreferences.getName())) {
                            this.globalPreferences =
                                    projectPreferences.getFieldDefaultSet(GLOBAL_PREFERENCES_NAME);
                        } else {
                            this.addProjectPreferences(projectPreferences);
                        }
                    }
                }
            }

            if (this.globalPreferences == null) {
                this.globalPreferences = new FieldDefaultSet();
            }

        } catch (IOException e) {
            throw new ConfigurationException(
                    "Failed to access preferences.", e);
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Failed to parse preferences.", e);
        }
    }

    /**
     * Overwrites the preferences file with an xml representation of this
     * object.
     *
     * @return true if the save completed successfully; otherwise false.
     */
    public boolean save() {

        boolean wasSaveSuccessful = false;

        if (canWrite()) {
            String prefsPath = preferencesFile.getAbsolutePath();
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(preferencesFile);
                final String xml = toXml();
                fos.write(xml.getBytes());
                LOG.info("Saved preferences file " + prefsPath);
                wasSaveSuccessful = true;
            } catch (IOException e) {
                String msg = "Failed to save preferences file " +
                             prefsPath;
                LOG.error(msg, e);
                throw new IllegalStateException(msg, e);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        LOG.warn("After save, failed to close " + prefsPath);
                    }
                }
            }
        }

        return wasSaveSuccessful;
    }

    /**
     * @return true if the preferences file can be written.
     */
    public boolean canWrite() {
        boolean canWrite = false;
        if (loaded && (preferencesFile != null)) {
            if (preferencesFile.exists()) {
                canWrite = preferencesFile.canWrite();
            } else {
                File preferencesDirectory = preferencesFile.getParentFile();
                canWrite = ((preferencesDirectory != null) &&
                            preferencesDirectory.canWrite());
            }
        }
        return canWrite;
    }

    private void createSetAndAdd(String path,
                                 Class fieldClass,
                                 String setNextMethodName,
                                 Digester digester) {
        digester.addObjectCreate(path, fieldClass);
        digester.addSetProperties(path);
        digester.addSetNext(path, setNextMethodName);
    }

    private static final Logger LOG =
            Logger.getLogger(TransmogrifierPreferences.class);

    private static final String FILE_NAME = ".tmog-preferences.xml";
    private static final String GLOBAL_PREFERENCES_NAME = "tmog-global-preferences";
    private static final String DARK_COLOR_SCHEME = "dark-color-scheme";
}