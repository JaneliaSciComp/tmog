package org.janelia.it.utils.filexfer;

/**
 * This is exception is thrown when a file copy fails.
 *
 * @author Peter Davies
 */
public class FileCopyFailedException extends Exception {

    public FileCopyFailedException() {
    }

    public FileCopyFailedException(String message) {
        super(message);
    }

    public FileCopyFailedException(String message,
                                   Throwable cause) {
        super(message, cause);
    }

    public FileCopyFailedException(Throwable cause) {
        super(cause);
    }
}
