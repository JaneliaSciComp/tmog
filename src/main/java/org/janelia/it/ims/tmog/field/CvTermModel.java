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

        // <cv>
        //     <name>age_remap</name>
        //     <displayName>Age</displayName>
        //     <definition>Age remapping</definition>
        //     <termSet>
        //         <term>
        //             <name>A</name>
        //             <displayName>Adult</displayName>
        //             <dataType>text</dataType>
        //             <definition>Adult</definition>
        //         </term>
        //         ...
        //     </termSet>
        //     ...
        // </cv>

        setValueCreationPath("*/term");
        setRelativeActualValuePath("name");
        setRelativeValueDisplayNamePath("displayName");
    }
}
