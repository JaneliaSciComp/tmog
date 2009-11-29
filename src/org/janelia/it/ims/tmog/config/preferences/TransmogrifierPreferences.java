/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
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

    private static TransmogrifierPreferences instance = null;

    public static TransmogrifierPreferences getInstance() {
        if (instance == null) {
            TransmogrifierPreferences prefs = new TransmogrifierPreferences();
            try {
                prefs.load();
            } catch (ConfigurationException e) {
                LOG.error("Preferences Error", e);
                prefs.loadError = e.getMessage();
            }
            instance = prefs;
        }
        return instance;
    }
    
    private File prefsFile;
    private Map<String, ProjectPreferences> projectNameToPreferencesMap;
    private String loadError;

    protected TransmogrifierPreferences() {
        this.projectNameToPreferencesMap =
                new LinkedHashMap<String, ProjectPreferences>();
        this.loadError = null;
    }

    public ProjectPreferences getPreferences(String projectName) {
        return projectNameToPreferencesMap.get(projectName);
    }
    
    public void addProjectPreferences(ProjectPreferences projectPreferences) {
        this.projectNameToPreferencesMap.put(
                projectPreferences.getName(),
                projectPreferences);
    }

    public String getLoadError() {
        return loadError;
    }

    // TODO: replace this with jaxb annotations whenever we can drop jdk1.5
    public String toXml() {
        String xml;
        if (projectNameToPreferencesMap.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("<transmogrifierPreferences>\n");
            for (ProjectPreferences projectPreferences :
                    projectNameToPreferencesMap.values()) {
                if (projectPreferences.getNumberOfFieldDefaultSets() > 0) {
                    sb.append(projectPreferences.toXml());
                }
            }
            sb.append("</transmogrifierPreferences>");
            xml = sb.toString();
        } else {
            xml = "";
        }
        return xml;
    }

    /**
     * Populates the preferences object model from the user's home directory.
     *
     * @throws ConfigurationException
     *   if an error occurs parsing the preferences file.
     */
    public void load() throws ConfigurationException {

        // TODO: consider load of common (not user specific) preferences - need to handle concurrent write issues

        final String userHomePath = System.getProperty("user.home");
        prefsFile = new File(userHomePath, FILE_NAME);
        final String prefsPath = prefsFile.getAbsolutePath();

        if (prefsFile.exists()) {

            if (prefsFile.canRead()) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(prefsFile);
                    load(fis);
                    LOG.info("Loaded preferences file " + prefsPath);
                } catch (Exception e) {
                    String msg = "Failed to load preferences file " +
                                 prefsPath;
                    throw new ConfigurationException(msg, e);
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            LOG.warn("After load, failed to close " +
                                     prefsPath);
                        }
                    }
                }

            } else {
                throw new ConfigurationException(
                        "You do not have access to read preference data from " +
                        prefsPath + ".");
            }

        } else {
            File prefsDir = prefsFile.getParentFile();
            if ((prefsDir == null) || (! prefsDir.canWrite())) {
                throw new ConfigurationException(
                        "You do not have access to save preference data to " +
                        prefsPath + ".");
            }
        }

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

            ArrayList parsedList = (ArrayList) digester.parse(stream);
            if (parsedList != null) {
                for (Object element : parsedList) {
                    if (element instanceof ProjectPreferences) {
                        this.addProjectPreferences((ProjectPreferences) element);
                    }
                }
            }
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Failed to access preferences.", e);
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Failed to parse preferences.", e);
        }
    }

    public boolean save() {

        boolean wasSaveSuccessful = false;

        if (canWrite()) {
            String prefsPath = prefsFile.getAbsolutePath();
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(prefsFile);
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

    public boolean canWrite() {
        boolean canWrite = false;
        if ((loadError == null) && (prefsFile != null)) {
            if (prefsFile.exists()) {
                canWrite = prefsFile.canWrite();
            } else {
                File prefsDir = prefsFile.getParentFile();
                canWrite = ((prefsDir != null) && prefsDir.canWrite());
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
}