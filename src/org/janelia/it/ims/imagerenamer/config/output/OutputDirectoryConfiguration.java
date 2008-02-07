/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.config.output;

import org.apache.log4j.Logger;
import org.janelia.it.ims.imagerenamer.config.ConfigurationException;
import org.janelia.it.ims.imagerenamer.field.RenameField;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This class encapsulates configuration information about the
 * application output directory.
 *
 * @author Eric Trautman
 */
public class OutputDirectoryConfiguration {

    private ArrayList<OutputDirectoryComponent> components;
    private boolean derivedFromEarliestModifiedFile;

    /**
     * Empty constructor.
     */
    public OutputDirectoryConfiguration() {
        this.components = new ArrayList<OutputDirectoryComponent>();
        this.derivedFromEarliestModifiedFile = false;
    }

    /**
     * @return true if the output directory's last path component is derived
     *         from the earliest modified source file in the session;
     *         otherwise false.
     */
    public boolean isDerivedFromEarliestModifiedFile() {
        return derivedFromEarliestModifiedFile;
    }

    /**
     * Sets whether the output directory's last path component should be
     * derived from the earliest modified source file in the session.
     *
     * @param  derivedFromEarliestModifiedFile  flag value.
     */
    public void setDerivedFromEarliestModifiedFile(boolean derivedFromEarliestModifiedFile) {
        this.derivedFromEarliestModifiedFile = derivedFromEarliestModifiedFile;
        SourceFileModificationTime modTime = new SourceFileModificationTime();
        modTime.setDatePattern("'" + FILE_SEP + "'yyyyMMdd");
        this.components.add(modTime);
    }

    /**
     * Adds the specified component to this configuration's list.
     *
     * @param  component  directory path component to add.
     */
    public void addComponent(OutputDirectoryComponent component) {
        components.add(component);
    }

    /**
     * @return true if the output directory should be manually chosen;
     *         otherwise false.
     */
    public boolean isManuallyChosen() {
        return (! derivedFromEarliestModifiedFile) && (components.size() == 0);
    }

    /**
     * @return true if the output directory is derived once for each session
     *         (and is not different for each rename file); otherwise false.
     */
    public boolean isDerivedForSession() {
        return derivedFromEarliestModifiedFile || isManuallyChosen();
    }

    /**
     * @return the base path for the output directory.
     */
    public String getBasePath() {
        StringBuilder basePath = new StringBuilder();
        for (OutputDirectoryComponent component : components) {
            if (component instanceof Path) {
                basePath.append(((Path) component).getPath());
            } else {
                break;
            }
        }
        return basePath.toString();
    }

    /**
     * Sets the base path for the output directory.
     *
     * @param  basePath  path name to set.
     */
    public void setBasePath(String basePath) {
        Path path = new Path(basePath);
        components.add(0, path);
    }

    /**
     * Verifies as much of the output directory configuration as possible
     * and normalizes the directory's base path so that it is absolute
     * and not relative.
     *
     * @param  projectName   the name of the current project
     *                       (for error messages).
     * @param  renameFields  the rename field configuration
     *                       (to check references).
     *
     * @throws ConfigurationException
     *   if the output directory configuration is not valid.
     */
    public void verify(String projectName,
                       List<RenameField> renameFields)
            throws ConfigurationException {

        if (! isManuallyChosen()) {

            String basePath = getBasePath();
            File baseDirectory = new File(basePath);
            // ensure output directory will be absolute - fully qualified
            if (! baseDirectory.isAbsolute()) {
                baseDirectory = baseDirectory.getAbsoluteFile();
                Path absolutePath = new Path(baseDirectory.getAbsolutePath());

                // ! manuallyChosen == components.size() > 0
                OutputDirectoryComponent firstComponent =
                        components.get(0);
                if (firstComponent instanceof Path) {
                    components.set(0, absolutePath);
                } else {
                    absolutePath.setPath(absolutePath.getPath() + FILE_SEP);
                    components.add(0, absolutePath);
                }
            }

            validateBaseDirectory(projectName, baseDirectory);
            validateRenameFieldValues(projectName, renameFields);
        }
    }

    /**
     * Builds the derived path for the output directory based on the
     * specified data.
     *
     * @param  sourceFile    the source file being renamed.
     * @param  renameFields  the rename fields supplied by the user.
     *
     * @return the derived output directory path.
     */
    public String getDerivedPath(File sourceFile,
                                 RenameField[] renameFields) {
        StringBuilder derivedPath = new StringBuilder(128);
        String componentValue;
        for (OutputDirectoryComponent component : components) {
            componentValue = component.getValue(sourceFile, renameFields);
            if (componentValue != null) {
                derivedPath.append(componentValue);
            }
        }
        return derivedPath.toString();
    }

    /**
     * Identifies the specifeid file with the earliest last modification
     * date and then derives the output directory path fragment for
     * that file.
     *
     * @param  sourceDirectory  directory containing the session's source files.
     * @param  sourceFiles      the source files being renamed.
     *
     * @return a path fragment derived from the earliest source file.
     */
    public String getDerivedPathForEarliestFile(File sourceDirectory,
                                                File[] sourceFiles) {

        File earliestFile = sourceDirectory;
        if (sourceDirectory.isDirectory()) {
            long earliestMod = sourceDirectory.lastModified();
            for (File sourceFile : sourceFiles) {
                long fileMod = sourceFile.lastModified();
                if (fileMod < earliestMod) {
                    earliestMod = fileMod;
                    earliestFile = sourceFile;
                }
            }
        }

        return getDerivedPath(earliestFile, new RenameField[0]);
    }

    /**
     * @return a description of this output directory path for display.
     */
    public String getDescription() {
        StringBuilder description = new StringBuilder(128);
        File basePath = new File("");
        if (components.size() > 0) {
            String componentDescription;
            for (OutputDirectoryComponent component : components) {
                componentDescription = component.getDescription();
                if (componentDescription != null) {
                    description.append(componentDescription);
                }
            }
        } else {
            description.append(basePath.getAbsolutePath());
        }
        return description.toString();        
    }

    /**
     * Utility method to create the specified directory and identify
     * an errors that occur during creation.
     *
     * @param  outputDirectory  directory to create.
     *
     * @return an error message if creation fails; otherwise null.
     */
    public static String createAndValidateDirectory(File outputDirectory) {
        String outputFailureMsg = null;

        if (!outputDirectory.exists()) {
            try {
                outputDirectory.mkdir();
            } catch (Exception e1) {
                outputFailureMsg =
                        "Failed to create output directory " +
                        outputDirectory.getAbsolutePath() + ".";
                LOG.error(outputFailureMsg, e1);
            }
        }

        if (!outputDirectory.isDirectory()) {
            outputFailureMsg =
                    "The output directory must be set to a valid directory.";
        }
        return outputFailureMsg;
    }

    private void validateBaseDirectory(String projectName,
                                       File baseDirectory)
            throws ConfigurationException {

        if (! baseDirectory.exists()) {
            throw new ConfigurationException(
                    "The output directory base path (" +
                    baseDirectory.getAbsolutePath() +
                    ") for the " + projectName +
                    " project does not exist.");
        }
        if (! baseDirectory.isDirectory()) {
            throw new ConfigurationException(
                    "The output directory base path (" +
                    baseDirectory.getAbsolutePath() +
                    ") for the " + projectName +
                    " project is not a directory.");
        }
        if (! baseDirectory.canWrite()) {
            throw new ConfigurationException(
                    "The output directory base path (" +
                    baseDirectory.getAbsolutePath() +
                    ") for the " + projectName +
                    " project is not writable.");
        }
    }

    private void validateRenameFieldValues(String projectName,
                                           List<RenameField> renameFields)
            throws ConfigurationException {

        HashSet<String> fieldDisplayNames =
                new HashSet<String>(renameFields.size());
        String availableDisplayName;
        for (RenameField field : renameFields) {
            availableDisplayName = field.getDisplayName();
            if (availableDisplayName != null) {
                fieldDisplayNames.add(availableDisplayName);
            }
        }

        String fieldDisplayName;
        for (OutputDirectoryComponent component : components) {
            if (component instanceof RenameFieldValue) {
                fieldDisplayName =
                        ((RenameFieldValue) component).getFieldDisplayName();
                if (! fieldDisplayNames.contains((fieldDisplayName))) {
                    throw new ConfigurationException(
                            "The output directory configuration for the " +
                            projectName +
                            " project references the field display name '" +
                            fieldDisplayName + "' which does not exist.  " +
                            "Please review the configured renameFieldValue " +
                            "and renamerPattern elements for this project.");
                }
            }
        }
    }


    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(OutputDirectoryConfiguration.class);

    private static final String FILE_SEP = System.getProperty("file.separator");
}
