/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.config;

import org.janelia.it.ims.tmog.field.DataField;

import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates configuration information about the
 * application data fields.
 *
 * @author Eric Trautman
 */
public class DataFields {
    private ArrayList<DataField> fields;

    public DataFields() {
        fields = new ArrayList<DataField>();
    }

    public List<DataField> getFields() {
        return fields;
    }

    public boolean add(DataField field) {
        return fields.add(field);
    }

    public int getNumberOfVisibleFields() {
        int count = 0;
        for (DataField field : fields) {
            if (field.isVisible()) {
                count++;
            }
        }
        return count;
    }
}
