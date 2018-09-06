/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;
import org.janelia.it.ims.tmog.view.component.DataTable;
import org.janelia.it.ims.tmog.view.component.DataTableKeyListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;

/**
 * This class supports the editing of a valid value cell
 * within the file table.
 * It uses <a href="http://www.glazedlists.com/">Glazed Lists</a>
 * auto complete support to simplify value entry
 * (see also:
 * <a href="http://publicobject.com/glazedlistsdeveloper/">
 *     tutorial screen casts
 * </a>).
 *
 * @author Eric Trautman
 */
public class AutoCompleteEditor extends DefaultCellEditor {

    private DataTable dataTable;
    private ValidValueModel model;
    private AutoCompleteSupport<ValidValue> autoCompleteSupport;

    public AutoCompleteEditor() {
        super(new JComboBox());
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {

        JComboBox editorComboBox = (JComboBox) editorComponent;

        if ((table instanceof DataTable) &&
            (value instanceof ValidValueModel)) {

            if (dataTable != table) {
                dataTable = (DataTable) table;                
                // remove any existing data table listeners
                // (leave ui listener alone or combo box key selection fails)
                for (KeyListener listener : editorComboBox.getKeyListeners()) {
                    if (listener instanceof DataTableKeyListener) {
                        editorComboBox.removeKeyListener(listener);
                    }
                }
                final KeyListener dtListener = dataTable.getKeyListener();
                editorComboBox.addKeyListener(dtListener);
            }

            // NOTE: We always need to rebuild auto complete support and filter values
            //       since the model can be shared between columns but the filters can differ.

            model = (ValidValueModel) value;

            if (autoCompleteSupport != null) {
                // must remove previous auto-complete support since the
                // same editor combo box is used for all cells
                autoCompleteSupport.uninstall();
            }

            model.filterValues(dataTable.getModel(), row);

            autoCompleteSupport =
                    AutoCompleteSupport.install(editorComboBox,
                                                model.getValidValues());

            autoCompleteSupport.setFilterMode(TextMatcherEditor.CONTAINS);

            // NOTE: do not use autoCompleteSupport.setStrict(true)
            // since it seems to disable contains filtering for
            // partially entered strings

            // transfer previous model selection to auto-complete combo box
            editorComboBox.setSelectedItem(model.getSelectedItem());

            if (model.isSharedForAllSessionFiles()) {
                TableRepainter tableRepainter = new TableRepainter(table);
                model.removeListDataListener(tableRepainter);
                model.addListDataListener(tableRepainter);
            }

        } else {
            editorComboBox = null;
            dataTable = null;
            model = null;
        }

        return editorComboBox;
    }

    public Object getCellEditorValue() {
        return model;
    }

    @Override
    public boolean stopCellEditing() {

        // transfer auto-complete combo box selection to model
        // note: if entered item doesn't exist in model, selection will be null
        final JComboBox comboBox = (JComboBox) editorComponent;
        model.setSelectedItem(comboBox.getSelectedItem());

        fireEditingStopped();

        return true;
    }

}
