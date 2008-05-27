/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;

/**
 * This class supports the editing of a valid value cell
 * within the file table.
 *
 * @author Eric Trautman
 */
public class ValidValueEditor extends DefaultCellEditor {

    private ComboBoxModel comboBoxModel;
    private static JComboBox myComboBox = new MyJComboBox();

    static {
        myComboBox.setKeySelectionManager(new MyKeySelectionManager());
    }

    public ValidValueEditor() {
        super(myComboBox);
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

    static class MyJComboBox extends JComboBox {
        public void processFocusEvent(FocusEvent fe) {

            super.processFocusEvent(fe);

            KeyboardFocusManager focusManager =
                    KeyboardFocusManager.getCurrentKeyboardFocusManager();
            Component focusOwner = focusManager.getFocusOwner();

            if (isDisplayable() &&
                    (fe.getID() == FocusEvent.FOCUS_GAINED) &&
                    (focusOwner == this) &&
                    (!isPopupVisible())) {
                showPopup();
            }
        }
    }

    /**
     * This class will buffer key characters for 3 seconds or until they identify a unique value in the
     * ComboBoxModel.  This overrides the default behavior of selecting the first matching value.
     */
    static class MyKeySelectionManager implements JComboBox.KeySelectionManager {
        static long TIMEOUT = 3000;  //milliseconds
        long firstKeyTime=System.currentTimeMillis();
        StringBuffer buffer = new StringBuffer();

        public int selectionForKey(char aKey, ComboBoxModel aModel) {
            if (!Character.isLetterOrDigit(aKey)) return -1; //don't process anything other then letters or digits
            if (buffer.length() > 0) {
                if ((firstKeyTime + TIMEOUT) < System.currentTimeMillis()) {  //time expired on buffer, clear it and reset
                    buffer.setLength(0);
                  }
            }
            buffer.append(aKey);
            if (buffer.length()==1) firstKeyTime = System.currentTimeMillis();
            int matchingElement = -1;
            for (int i = 0; i < aModel.getSize(); i++) {
                if (aModel.getElementAt(i).toString().toUpperCase().startsWith(buffer.toString().toUpperCase())) { //match found
                    if (matchingElement > -1) return -1; //more than one match
                    else matchingElement = i;
                }
            }
            if (matchingElement>-1) {
                buffer.setLength(0);  //clear buffer if we found one
            }

            return matchingElement;
        }
    }
}
