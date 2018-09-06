/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.filefilter;

import org.janelia.it.ims.tmog.target.FileTargetNamer;

import java.io.File;

/**
 * This filter accepts files that match a specified pattern and query.
 *
 * @author Eric Trautman
 */
public class FileNamePatternWithQueryFilter extends FileNamePatternFilter {

    private QueryFilter queryFilter;

    public FileNamePatternWithQueryFilter(String pattern,
                                          String queryUrl,
                                          boolean includeFilesMatchingQuery,
                                          FileTargetNamer targetNamer)
            throws IllegalArgumentException {
        
        super(pattern);
        this.queryFilter = new QueryFilter(queryUrl,
                                           includeFilesMatchingQuery,
                                           targetNamer);
    }

    public boolean accept(File pathname) {
        return super.accept(pathname) &&
               queryFilter.accept(pathname);
    }

}