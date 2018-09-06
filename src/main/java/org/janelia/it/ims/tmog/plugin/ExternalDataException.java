/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin;

/**
 * This exception is thrown when a recoverable data error is discovered
 * during external processing.
 *
 * @author Eric Trautman
 */
public class ExternalDataException extends Exception {

    public ExternalDataException() {
        super();
    }

    public ExternalDataException(String message) {
        super(message);
    }

    public ExternalDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalDataException(Throwable cause) {
        super(cause);
    }
}
