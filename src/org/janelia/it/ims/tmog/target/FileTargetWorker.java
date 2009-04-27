/*
* Copyright 2009 Howard Hughes Medical Institute.
* All rights reserved.
* Use is subject to Janelia Farm Research Center Software Copyright 1.0
* license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
*/

package org.janelia.it.ims.tmog.target;

import org.janelia.it.utils.BackgroundWorker;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
     */
    public FileTargetWorker(File rootDirectory,
                            FileFilter filter,
                            boolean recursiveSearch,
                            Comparator<FileTarget> sortComparator) {

        this.rootDirectory = rootDirectory;
        this.filter = filter;
        this.recursiveSearch = recursiveSearch;
        this.sortComparator = sortComparator;
    }

    /**
     * @return directory in which targets are located.
     */
    public File getRootDirectory() {
        return rootDirectory;
    }

    /**
     * Executes the operation in a background thread.
     *
     * @throws Exception
     *   if any errors occur during processing.
     */
    protected List<FileTarget> executeBackgroundOperation() throws Exception {

        List<FileTarget> targets;

        if (recursiveSearch) {
            targets = getAllTargets();
        } else {
            final File[] children = rootDirectory.listFiles(filter);
            targets = new ArrayList<FileTarget>(children.length);
            for (File child : children) {
                targets.add(new FileTarget(child));
            }
        }

        if (sortComparator != null) {
            Collections.sort(targets, sortComparator);
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
                targets.add(new FileTarget(file));
            } else if (file.isDirectory()) {
                updateStatus("searching " + file.getName());
                final File[] children = file.listFiles();
                for (File child : children) {
                    targets = addTargets(child, targets);
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

}
