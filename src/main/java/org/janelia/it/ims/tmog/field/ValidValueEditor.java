/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.view.component.DataTable;
import org.janelia.it.ims.tmog.view.component.DataTableKeyListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * This class supports the editing of a valid value cell
 * within the file table.
 *
 * @author Eric Trautman
 */
public class ValidValueEditor extends DefaultCellEditor {

    private DataTable dataTable;
    private ValidValueModel model;

    public ValidValueEditor() {
        super(new EditorComboBox());
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {

        EditorComboBox editorComboBox = (EditorComboBox) editorComponent;

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

            model = (ValidValueModel) value;

            // NOTE: We always need to rebuild filter values since the model
            // can be shared between columns but the filters can differ.

            model.filterValues(dataTable.getModel(), row);

            editorComboBox.setModel(model);
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

        boolean isEditingStopped = false;

        // We need to record what event caused editing to stop
        // immediately before any other events (e.g. mouse click
        // in the confirmation dialog) get placed on the queue.
        final AWTEvent event = EventQueue.getCurrentEvent();
        final boolean isStopCausedByKeyEdit = (event instanceof KeyEvent);
        final EditorComboBox comboBox = (EditorComboBox) editorComponent;

        // If a keyboard event triggered selection of an item in the drop down
        // menu, simply hide the menu and allow the editor to remain
        // (and retain focus).  Otherwise, allow editing to completely stop
        // by notifying all listeners.

        // NOTE: This works around a bug where valid value fields are
        //       configured in both the data table and at least one nested
        //       data table.  For some reason in these cases, focus is
        //       properly handled for the field selected initially but
        //       focus is lost for other fields.  For example, selecting
        //       a data table field first causes focus to be lost whenever
        //       selecting a nested data table field.  This work around
        //       fixes the focus problem for keyboard selections (when it
        //       matters).  You can still observe the problem with mouse
        //       selections.
        if (isStopCausedByKeyEdit && comboBox.isPopupVisible()) {
            comboBox.hidePopup();
        } else {
            fireEditingStopped();
            isEditingStopped = true;
        }

        return isEditingStopped;
    }

    static class EditorComboBox extends JComboBox<ValidValue> {
        public EditorComboBox() {
            setKeySelectionManager(new MyKeySelectionManager());
        }

        @Override
        public void processKeyEvent(KeyEvent e) {
            super.processKeyEvent(e);

            // Hitting the tab key normally hides the combo box popup
            // (see the JComboBox implementation of this method).
            // We override that action here because this combo box editor
            // is potentially used for both a previously edited cell and
            // the current cell being edited.  The loss of focus on the
            // previous cell will hide its popup and the focus on the
            // current cell will show its popup (see processFocusEvent below).
            // The tab keyboard event then hides the current cell popup.
            // This simply restores it once again.
            //
            // NOTE: The combo box isShowing() check is needed to make
            //       sure we have tabbed into another valid value cell.       
            final int keyCode = e.getKeyCode();
            if ((keyCode == KeyEvent.VK_TAB) && isShowing()) {
                showPopup();
            }
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
