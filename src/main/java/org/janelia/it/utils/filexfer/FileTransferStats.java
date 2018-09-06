/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.utils.filexfer;

/**
 * Contains basic statistics for a file transfer.
 * 
 * @author Eric Trautman
 */
public class FileTransferStats {

    private long bytesProcessed;
    private long duration;

    public FileTransferStats(long bytesProcessed,
                             long duration) {
        this.bytesProcessed = bytesProcessed;
        this.duration = duration;
    }

    public double getDurationSeconds() {
        return duration / 1000.0;
    }
    
    public long getBytesProcessed() {
        return bytesProcessed;
    }

    @Override
    public String toString() {
        return "FileTransferStats{" +
               "bytesProcessed=" + bytesProcessed +
               ", duration=" + duration +
               '}';
    }
}