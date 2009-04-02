/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.view.component.DataTable;
import org.janelia.it.ims.tmog.view.component.NarrowOptionPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

/**
 * This class supports the editing of a valid value cell
 * within the file table.
 *
 * @author Eric Trautman
 */
public class ValidValueEditor extends DefaultCellEditor {

    private DataTable dataTable;
    private EditorComboBox editorComboBox;
    private ValidValueModel model;

   public ValidValueEditor(DataTable table) {
       super(new EditorComboBox());
       this.dataTable = table;
       this.editorComboBox = (EditorComboBox) editorComponent;
       this.editorComboBox.addKeyListener(table.getKeyListener());
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        if (value instanceof ValidValueModel) {
            model = (ValidValueModel) value;
            editorComboBox.setModel(model);
            if (model.isSharedForAllSessionFiles()) {
                TableRepainter tableRepainter = new TableRepainter(table);
                model.removeListDataListener(tableRepainter);
                model.addListDataListener(tableRepainter);
            }
        }

        return editorComboBox;
    }

    public Object getCellEditorValue() {
        return model;
    }

    @Override
    public boolean stopCellEditing() {

        // We need to record what event caused editing to stop
        // immediately before any other events (e.g. mouse click
        // in the confirmation dialog) get placed on the queue.
        final boolean isStopCausedByKeyEdit =
                (EventQueue.getCurrentEvent() instanceof KeyEvent);

        boolean isEditingStopped = true;

        if (! model.verify()) {
            String dialogMsg = model.getErrorMessage() +
                    "  Would you like to correct the field now?";

            int selection =
                    NarrowOptionPane.showConfirmDialog(
                            dataTable,
                            dialogMsg,
                            "Invalid Entry", // title
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);

            if (selection == JOptionPane.YES_OPTION)  {
                editorComboBox.requestFocusInWindow();
                isEditingStopped = false;
            } else {
                // If a key edit (e.g. tab) caused editing to stop
                // and the user has decided not to fix the current
                // invalid cell, we need to "schedule" a request focus
                // event for the newly selected cell.
                // Otherwise (e.g. if a mouse click caused editing
                // to stop), nothing needs to be done.
                if (isStopCausedByKeyEdit) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            dataTable.editAndRequestFocusForSelectedCell();
                        }
                    });
                }
            }
        }

        if (isEditingStopped) {
            fireEditingStopped();
        }

        return isEditingStopped;
    }

    static class EditorComboBox extends JComboBox {
        public EditorComboBox() {
            setKeySelectionManager(new MyKeySelectionManager());
        }

        @Override
        protected void processFocusEvent(FocusEvent e) {
            super.processFocusEvent(e);

            // show drop down menu if this editor just gained focus
            KeyboardFocusManager focusManager =
                    KeyboardFocusManager.getCurrentKeyboardFocusManager();
            Component focusOwner = focusManager.getFocusOwner();

            if (isDisplayable() &&
                    (e.getID() == FocusEvent.FOCUS_GAINED) &&
                    (focusOwner == this) &&
                    (!isPopupVisible())) {
                showPopup();
            }
        }
    }

    /**
     * This class will buffer key characters for 3 seconds or until they
     * identify a unique value in the ComboBoxModel.  This overrides the
     * default behavior of selecting the first matching value.
     */
    static class MyKeySelectionManager implements JComboBox.KeySelectionManager {
        static long TIMEOUT = 3000;  //milliseconds
        long firstKeyTime = System.currentTimeMillis();
        StringBuffer buffer = new StringBuffer();

        public int selectionForKey(char aKey,
                                   ComboBoxModel aModel) {

            if (!Character.isLetterOrDigit(aKey)) {
                return -1; //don't process anything other then letters or digits
            }

            if (buffer.length() > 0) {
                if ((firstKeyTime + TIMEOUT) <
                    System.currentTimeMillis()) {
                    //time expired on buffer, clear it and reset
                    buffer.setLength(0);
                }
            }

            buffer.append(aKey);

            if (buffer.length() == 1) {
                firstKeyTime = System.currentTimeMillis();
            }

            int matchingElement = -1;
            final String bufferString = buffer.toString().toLowerCase();
            String elementString;
            for (int i = 0; i < aModel.getSize(); i++) {
                elementString = String.valueOf(aModel.getElementAt(i)).toLowerCase();
                if (elementString.startsWith(bufferString)) { //match found
                    if (matchingElement > -1) {
                        return -1; //more than one match
                    } else {
                        matchingElement = i;
                    }
                }
            }

            if (matchingElement > -1) {
                buffer.setLength(0);  //clear buffer if we found one
            }

            return matchingElement;
        }
    }
}
