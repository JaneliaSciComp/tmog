/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.utils.ant;

/**
 * Utility class to parse the latest revision number from the output of the
 * <a href="http://svnbook.red-bean.com/en/1.1/re57.html">svnversion</a>
 * command.
 *
 * @author Eric Trautman
 */
public class GetLatestSvnRevision {

    public static void main(String[] args) {
        String version = "?";
        if (args.length > 0) {
            String[] arr = args[0].split(":");
            if (arr.length == 1) {
                version = arr[0];
            } else if (arr.length > 1) {
                version = arr[1];
            }
        }
        System.out.println(version);
    }

}
