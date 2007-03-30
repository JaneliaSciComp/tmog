/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import javax.swing.DefaultCellEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import java.awt.event.FocusEvent;
import java.awt.KeyboardFocusManager;
import java.awt.Component;

/**
 * This class supports the editing of a valid value cell
 * within the file table.
 *
 * @author Eric Trautman
 */
public class ValidValueEditor extends DefaultCellEditor {

    private ComboBoxModel comboBoxModel;

    public ValidValueEditor() {
        super(new JComboBox() {
            public void processFocusEvent(FocusEvent fe) {

                super.processFocusEvent(fe);

                KeyboardFocusManager focusManager =
                        KeyboardFocusManager.getCurrentKeyboardFocusManager();
                Component focusOwner = focusManager.getFocusOwner();

                if (isDisplayable() &&
                        (fe.getID() == FocusEvent.FOCUS_GAINED) &&
                        (focusOwner == this) &&
                        (! isPopupVisible())) {
                    showPopup();
                }
            }
        });
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {

        Component component = super.getTableCellEditorComponent(table,
                                                                value,
                                                                isSelected,
                                                                row,
                                                                column);

        if (value instanceof ComboBoxModel && component instanceof JComboBox) {
            JComboBox comboBox = (JComboBox) component;
            comboBoxModel = (ComboBoxModel) value;
            comboBox.setModel(comboBoxModel);
        }

        return component;
    }

    public Object getCellEditorValue() {
        return comboBoxModel;
    }
}
