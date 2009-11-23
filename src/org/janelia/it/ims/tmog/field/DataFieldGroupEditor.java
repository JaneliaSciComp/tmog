/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.view.component.DataTable;
import org.janelia.it.ims.tmog.view.component.NestedDataTable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

/**
 * This class supports the editing of a field group cell
 * within the file table.
 *
 * @author Eric Trautman
 */
public class DataFieldGroupEditor
        extends AbstractCellEditor
        implements TableCellEditor {

    private DataTable parentTable;
    private NestedDataTable nestedTable;
    private DataFieldGroupModel model;

    public DataFieldGroupEditor(DataTable parentTable) {
        this.parentTable = parentTable;
        this.nestedTable = null;
        this.model = null;
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        Component component = null;

        if (value instanceof DataFieldGroupModel) {
            model = (DataFieldGroupModel) value;

            if (nestedTable == null) {
                // NOTE: need to create nested table here instead of
                //       in editor constructor to prevent infinite
                //       recursion problem when the nested table sets
                //       up its editors
                nestedTable = new NestedDataTable(parentTable);
            }
            nestedTable.setModel(model, column);
            component = nestedTable;
        }

        return component;
    }

    public Object getCellEditorValue() {
        return model;
    }

    @Override
    public boolean stopCellEditing() {
        boolean isEditingStopped = true;
        if (nestedTable.isEditing()) {
            isEditingStopped = nestedTable.getCellEditor().stopCellEditing();
        }
        if (isEditingStopped) {
            fireEditingStopped();
        }

        return isEditingStopped;
    }

    @Override
    public void cancelCellEditing() {
        if (nestedTable.isEditing()) {
            nestedTable.getCellEditor().cancelCellEditing();
        }
        fireEditingCanceled();
    }

    /**
     * Selects the first (if isNextAction is true) or last (if isNextAction
     * is false) selectable cell in the field group table.
     *
     * @param  isNextAction  indicates whether the selection is for the next
     */
    public void selectDefaultCell(boolean isNextAction) {
        nestedTable.clearSelection();
        if (isNextAction) {
            nestedTable.selectAndEditNextCell();
        } else {
            nestedTable.selectAndEditPreviousCell();
        }
    }
}