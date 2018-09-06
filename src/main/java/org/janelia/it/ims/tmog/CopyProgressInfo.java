/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog;

import java.io.File;

/**
 * This class encapsulates copy progress information.
 *
 * @author Eric Trautman
 */
public class CopyProgressInfo {

    private File fromFile;
    private File toFile;
    private long percentComplete;
    private int fileNumber;
    private int totalNumberOfFiles;

    public CopyProgressInfo(File fromFile,
                            File toFile,
                            long percentComplete,
                            int fileNumber,
                            int totalNumberOfFiles) {
        this.fromFile = fromFile;
        this.toFile = toFile;
        this.percentComplete = percentComplete;
        this.fileNumber = fileNumber;
        this.totalNumberOfFiles = totalNumberOfFiles;
    }

    public File getFromFile() {
        return fromFile;
    }

    public File getToFile() {
        return toFile;
    }

    public long getPercentComplete() {
        return percentComplete;
    }

    public int getFileNumber() {
        return fileNumber;
    }

    public int getTotalNumberOfFiles() {
        return totalNumberOfFiles;
    }

    public int getProgress() {
        return (int) percentComplete;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("copying file ");
        sb.append((fileNumber + 1));
        sb.append(" of ");
        sb.append(totalNumberOfFiles);
        sb.append(": ");
        sb.append(fromFile.getName());
        sb.append(" -> ");
        sb.append(toFile.getName());
        return sb.toString();
    }
}
