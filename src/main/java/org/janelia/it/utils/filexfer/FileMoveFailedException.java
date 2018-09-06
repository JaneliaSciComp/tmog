package org.janelia.it.utils.filexfer;

/**
 * This is exception is thrown when a file move fails.
 *
 * @author Peter Davies
 */
public class FileMoveFailedException extends Exception {

    public FileMoveFailedException() {
    }

    public FileMoveFailedException(String message) {
        super(message);
    }

    public FileMoveFailedException(String message,
                                   Throwable cause) {
        super(message, cause);
    }

    public FileMoveFailedException(Throwable cause) {
        super(cause);
    }
}
