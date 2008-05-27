/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.chacrm;

/**
 * This class is thrown when a transformant can not be found in the repository.
 *
 * @author Eric Trautman
 */
public class TransformantNotFoundException extends Exception {

    public TransformantNotFoundException() {
    }

    public TransformantNotFoundException(String message) {
        super(message);
    }

    public TransformantNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransformantNotFoundException(Throwable cause) {
        super(cause);
    }

    public TransformantNotFoundException(Transformant transformant) {
        super(getStandardMessage(transformant));
    }

    public static String getStandardMessage(String transformantID) {
        return "Transformant with ID '" + transformantID +  "' does not exist.";
    }

    public static String getStandardMessage(Transformant transformant) {
        String transformantID = null;
        if (transformant != null) {
            transformantID = transformant.getTransformantID();
        }
        return getStandardMessage(transformantID);
    }
}
