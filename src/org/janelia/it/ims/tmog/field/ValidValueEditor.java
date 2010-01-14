/*
 * Copyright 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.view.component.DataTable;

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
                // remove any existing listeners
                for (KeyListener listener : editorComboBox.getKeyListeners()) {
                    editorComboBox.removeKeyListener(listener);
                }
                editorComboBox.addKeyListener(dataTable.getKeyListener());
            }

            model = (ValidValueModel) value;
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

        // We need to record what event caused editing to stop
        // immediately before any other events (e.g. mouse click
        // in the confirmation dialog) get placed on the queue.
        final AWTEvent event = EventQueue.getCurrentEvent();
        final boolean isStopCausedByKeyEdit = (event instanceof KeyEvent);

        // Always stop editing since during editing,
        // we only validate populated entries and any populated entry
        // must be a valid selection from the value list.
        fireEditingStopped();

        // If a key edit (e.g. tab) caused editing to stop,
        // we need to "schedule" a request focus event for the
        // selected cell.  This prevents the drop down menu
        // from losing focus (a problem with nested tables).
        if (isStopCausedByKeyEdit && (dataTable != null)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    dataTable.editAndRequestFocusForSelectedCell();
                }
            });
        }

        return true;
    }

    static class EditorComboBox extends JComboBox {
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
