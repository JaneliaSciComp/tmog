/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.target;

import org.apache.log4j.Logger;
import org.janelia.it.utils.BackgroundWorker;
import org.janelia.it.utils.StringUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * This task locates file targets for a selected directory.
 *
 * @author Eric Trautman
 */
public class FileTargetWorker
        extends BackgroundWorker<List<FileTarget>, String> {

    private File rootDirectory;
    private FileFilter filter;
    private boolean recursiveSearch;
    private Comparator<FileTarget> sortComparator;
    private FileTargetNamer namer;
    private boolean filterDuplicateTargets;
    private TargetDataFile targetDataFile;
    private String summary;
    private List<File> emptyFiles;

    /**
     * Constructs a new worker.
     *
     * @param  rootDirectory    directory in which targets are located.
     *
     * @param  filter           filter that identifies valid target names
     *                          (or null if all target names are valid).
     *
     * @param  recursiveSearch  if true, also look for targets in all
     *                          subdirectories of the root directory
     *                          that do not match the filter;
     *                          otherwise simply look for targets in
     *                          the root directory.
     *
     * @param  sortComparator   comparator for sorting the result target
     *                          list (or null if sorting is not needed).
     *
     * @param  namer            namer for converting file names to
     *                          desired target names (or null if
     *                          conversion is not needed).
     *
     * @param  filterDuplicateTargets  indicates whether targets with the
     *                                 same name should be filtered.
     *
     * @param  targetDataFile   if set (non null), parse rootDirectory (file)
     *                          to derive targets instead of directly accessing
     *                          file system.
     *
     */
    public FileTargetWorker(File rootDirectory,
                            FileFilter filter,
                            boolean recursiveSearch,
                            Comparator<FileTarget> sortComparator,
                            FileTargetNamer namer,
                            boolean filterDuplicateTargets,
                            TargetDataFile targetDataFile) {

        this.rootDirectory = rootDirectory;
        this.filter = filter;
        this.recursiveSearch = recursiveSearch;
        this.sortComparator = sortComparator;
        this.namer = namer;
        this.filterDuplicateTargets = filterDuplicateTargets;
        this.targetDataFile = targetDataFile;
        this.summary = null;
        this.emptyFiles = new ArrayList<File>();
    }

    /**
     * @return directory in which targets are located.
     */
    public File getRootDirectory() {
        return rootDirectory;
    }

    /**
     * @return true if a processing summary exists; otherwise false.
     */
    public boolean hasSummary() {
        return StringUtil.isDefined(summary);
    }

    /**
     * @return the processing summary or null if no summary exists.
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Executes the operation in a background thread.
     *
     * @throws Exception
     *   if any errors occur during processing.
     */
    protected List<FileTarget> executeBackgroundOperation() throws Exception {

        List<FileTarget> targets;

        if (targetDataFile == null) {
            targets = getTargetsFromFileSystem();
        } else {
            targets = getTargetsFromDataFile();
        }

        if (filterDuplicateTargets) {

            // A root wormtracker data directory can contain directories and zip
            // files for the same experiment.  We need to filter out duplicate
            // experiments here, giving zip files precedence over directories.

            // NOTE: This assumes experiment names are unique.
            // If the names are not unique, experiments with the same name
            // but in different paths will get filtered.

            HashMap<String, FileTarget> nameToTargetMap =
                    new HashMap<String, FileTarget>(targets.size());
            HashSet<FileTarget> duplicateTargets = new HashSet<FileTarget>();
            String name;
            FileTarget existingTarget;
            for (FileTarget target : targets) {
                name = target.getName();
                existingTarget = nameToTargetMap.get(name);
                if (existingTarget == null) {
                    nameToTargetMap.put(name, target);
                } else {
                    File targetFile = target.getFile();
                    if (targetFile.isFile()) {
                        // files take precedence over directories
                        duplicateTargets.add(existingTarget);
                        nameToTargetMap.put(name, target);
                    } else {
                        duplicateTargets.add(target);
                    }
                }
            }

            if (duplicateTargets.size() > 0) {
                for (Iterator<FileTarget> i = targets.iterator(); i.hasNext();) {
                    if (duplicateTargets.contains(i.next())) {
                        i.remove();
                    }
                }
            }
        }

        if (sortComparator != null) {
            Collections.sort(targets, sortComparator);
        }

        return targets;
    }

    private List<FileTarget> getTargetsFromFileSystem() {
        List<FileTarget> targets;
        if (recursiveSearch) {
            targets = getAllTargets();
        } else {
            final File[] children = rootDirectory.listFiles(filter);
            if (children != null) {
                targets = new ArrayList<FileTarget>(children.length);
                for (File child : children) {
                    if (child.length() == 0) {
                        emptyFiles.add(child);
                    } else {
                        targets.add(new FileTarget(child, rootDirectory, namer));
                    }
                }
            } else {
                targets = new ArrayList<FileTarget>(0);
            }
        }

        if (emptyFiles.size() > 0) {
            StringBuilder sb = new StringBuilder(1024);
            sb.append("The following zero length (empty) files ");
            sb.append("were excluded from processing:\n\n");
            for (File emptyFile : emptyFiles) {
                sb.append("   ");
                sb.append(emptyFile.getAbsolutePath());
                sb.append('\n');
            }
            summary = sb.toString();
        }

        return targets;
    }

    private List<FileTarget> getAllTargets() {
        List<FileTarget> targets = new ArrayList<FileTarget>(2048);
        targets = addTargets(rootDirectory,
                             targets);
        return targets;
    }

    private List<FileTarget> addTargets(File file,
                                        List<FileTarget> targets) {
        if (! isCancelled()) {
            if ((filter == null) || filter.accept(file)) {
                if (file.length() == 0) {
                    emptyFiles.add(file);
                } else {
                    targets.add(new FileTarget(file, rootDirectory, namer));
                }
            } else if (file.isDirectory()) {
                updateStatus("searching " + file.getName());
                final File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        targets = addTargets(child, targets);
                    }
                }
            }
        }
        return targets;
    }

    private void updateStatus(String message) {
        if (! isCancelled()) {
            List<String> messages = new ArrayList<String>();
            messages.add(message);
            process(messages);
        }
    }

    private List<FileTarget> getTargetsFromDataFile()
            throws IllegalArgumentException {
        FileInputStream stream;
        try {
            stream = new FileInputStream(rootDirectory);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to open " + rootDirectory.getAbsolutePath() + ".",
                    e);
        }

        updateStatus("parsing " + rootDirectory.getAbsolutePath());

        TargetList targetList;
        try {
            targetList = targetDataFile.getTargets(stream);
            summary = targetList.getSummary();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to parse " + rootDirectory.getAbsolutePath() + ".",
                    e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                LOG.warn("getTargetsFromDataFile: Failed to close stream for  " +
                         rootDirectory.getAbsolutePath(), e);
            }
        }

        return targetList.getList();
    }

    private static final Logger LOG =
            Logger.getLogger(FileTargetWorker.class);

}
