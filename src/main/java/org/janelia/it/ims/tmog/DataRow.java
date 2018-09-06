/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog;

import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.target.Target;

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
    private DataTableModel dataTableModel;

    /**
     * Creates a data row for the specified target without any model reference.
     *
     * @param  target  target for the data being collected.
     */
    public DataRow(Target target) {
        this.target = target;
        this.fields = new ArrayList<DataField>();
        this.dataTableModel = null;
    }

    /**
     * Creates a data row for the specified target with a reference to the
     * model for the entire session.
     *
     * @param  target          target for the data being collected.
     * @param  dataTableModel  model for current session.
     */
    public DataRow(Target target,
                   DataTableModel dataTableModel) {
        this(target);
        this.dataTableModel = dataTableModel;
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

    public int getFieldCount() {
        return fields.size();
    }

    public DataTableModel getDataTableModel() {
        return dataTableModel;
    }
}