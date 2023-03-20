/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

/**
 * This model supports selecting a controlled vocabulary term from a
 * predefined set of terms.  Terms are retrieved at start-up.
 *
 * @author Eric Trautman
 */
public class CvTermModel
        extends HttpValidValueModel {

    public CvTermModel() {

        // https://sage-responder.int.janelia.org/cvterms?cv=age_remap&_columns=cv_term,display_name
        // {
        //   "cvterm_data": [
        //     {
        //       "cv_term": "A",
        //       "display_name": "Adult"
        //     },
        //     {
        //       "cv_term": "A00",
        //       "display_name": "Adult (unspecified)"
        //     }, ...
        //   ]
        // }

        setValueCreationPath("$.cvterm_data[*]"); // see https://github.com/json-path/JsonPath
        setRelativeActualValuePath("cv_term");
        setRelativeValueDisplayNamePath("display_name");
        setResponseContentType("application/json");
    }
}
