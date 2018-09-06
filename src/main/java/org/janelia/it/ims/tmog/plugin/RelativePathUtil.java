/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import java.io.File;
import java.util.Stack;

/**
 * Utility to derive relative path for file.
 *
 * @author Eric Trautman
 */
public class RelativePathUtil {

    public static String getRelativePath(File file) {
        return getRelativePath(file, 1);
    }

    /**
     * @param  file                      file for which relative path
     *                                   is to be derived.
     * @param  maximumParentDirectories  maximum number of parent directories
     *                                   to be included in the relative path.
     *
     * @return a relative path for the specified file or null if no
     *         file is specified.
     */
    public static String getRelativePath(File file,
                                         int maximumParentDirectories) {
        String relativePath = null;

        if (file != null) {

            Stack<String> nameStack = new Stack<String>();
            nameStack.push(file.getName());

            File parent = file.getParentFile();
            String name;
            while (nameStack.size() <= maximumParentDirectories) {

                if ((parent == null)) {
                    break;
                }

                name = parent.getName();

                if (name.equals("..")) {
                    parent = parent.getParentFile();
                } else if ((name.length() > 0) && (! name.equals("."))) {
                    nameStack.push(name);
                }

                if (parent != null) {
                    parent = parent.getParentFile();
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append(nameStack.pop());
            for (int i = nameStack.size(); i > 0; i--) {
                sb.append('/');
                sb.append(nameStack.pop());
            }

            relativePath = sb.toString();
        }

        return relativePath;
    }

}