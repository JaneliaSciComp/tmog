/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.utils.db;

/**
 * This exception is thrown when a database configuration error occurs.
 *
 * @author Eric Trautman
 */
public class DbConfigException extends Exception {

    public DbConfigException() {
        super();
    }

    public DbConfigException(String message) {
        super(message);
    }

    public DbConfigException(String message,
                             Throwable cause) {
        super(message, cause);
    }

    public DbConfigException(Throwable cause) {
        super(cause);
    }
}
