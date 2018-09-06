/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin;

/**
 * This exception is thrown when a non-recoverable system error occurs
 * during external processing.
 */
public class ExternalSystemException extends Exception {

    public ExternalSystemException() {
        super();
    }

    public ExternalSystemException(String message) {
        super(message);
    }

    public ExternalSystemException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalSystemException(Throwable cause) {
        super(cause);
    }
}
