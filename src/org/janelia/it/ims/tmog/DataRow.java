/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog;

import org.janelia.it.ims.tmog.field.DataField;

import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates the model data for a target.
 *
 * @author Eric Trautman
 */
public class DataRow {

    private Target target;
    private List<DataField> fields;

    public DataRow(Target target) {
        this.target = target;
        this.fields = new ArrayList<DataField>();
    }

    public Target getTarget() {
        return target;
    }

    public List<DataField> getFields() {
        return fields;
    }

    public DataField getField(int fieldIndex) {
        return fields.get(fieldIndex);
    }

    public void setField(int fieldIndex,
                         DataField field) {
        fields.set(fieldIndex, field);
    }

    public void addField(DataField field) {
        fields.add(field);
    }

    public void addAllFields(List<DataField> fieldList) {
        fields.addAll(fieldList);
    }

    public int getFieldCount() {
        return fields.size();
    }

    
}