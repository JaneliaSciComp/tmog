/*
 * Copyright 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.view.component.DataTable;
import org.janelia.it.ims.tmog.view.component.DataTableKeyListener;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
 * This class supports the editing of a verified field cell
 * within the file table.
 *
 * @author Eric Trautman
 */
public class VerifiedFieldEditor extends AbstractCellEditor
        implements TableCellEditor, ActionListener, ItemListener {

        private DataTable dataTable;
        private JTextField textField;
        private VerifiedFieldModel verifiedFieldModel;

        public VerifiedFieldEditor() {
            this.textField = new JTextField();
            this.textField.addActionListener(this);
        }

        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            if ((table instanceof DataTable) &&
                (value instanceof VerifiedFieldModel)) {

                if (dataTable != table) {
                    dataTable = (DataTable) table;
                    // remove any existing data table listeners
                    for (KeyListener listener : textField.getKeyListeners()) {
                        if (listener instanceof DataTableKeyListener) {
                            textField.removeKeyListener(listener);
                        }
                    }
                    final KeyListener dtListener = dataTable.getKeyListener();
                    textField.addKeyListener(dtListener);
                }

                verifiedFieldModel = (VerifiedFieldModel) value;
                textField.setDocument(verifiedFieldModel);
                textField.setBorder(new LineBorder(Color.gray));
                if (isSelected) {
                    textField.selectAll();
                }
            }
            return textField;
        }

        public void setValue(Object value) {
            textField.setText((value != null) ? value.toString() : "");
        }

        public Object getCellEditorValue() {
            return textField.getDocument();
        }

        public boolean stopCellEditing() {

            // We need to record what event caused editing to stop
            // immediately before any other events (e.g. mouse click
            // in the confirmation dialog) get placed on the queue.
            final boolean isStopCausedByKeyEdit =
                    (EventQueue.getCurrentEvent() instanceof KeyEvent);

            boolean isEditingStopped = true;

            String coreValue = verifiedFieldModel.getCoreValue();
            if ((dataTable != null) &&
                (coreValue.length() > 0) &&
                (! verifiedFieldModel.verify())) {

                int selection =
                        dataTable.showInvalidEntryConfimDialog(
                                verifiedFieldModel);

                if (selection == JOptionPane.YES_OPTION)  {
                    textField.setBorder(new LineBorder(Color.red));
                    textField.selectAll();
                    textField.requestFocusInWindow();
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
                if (verifiedFieldModel.isSharedForAllSessionFiles() &&
                    (dataTable != null)) {
                    dataTable.repaint();
                }
            }

            return isEditingStopped;
		}

        /**
         * Returns true if <code>anEvent</code> is <b>not</b> a
         * <code>MouseEvent</code>.  Otherwise, it returns true
         * if the necessary number of clicks have occurred, and
         * returns false otherwise.
         *
         * @param   anEvent         the event
         * @return  true  if cell is ready for editing, false otherwise
         * @see #shouldSelectCell
         */
        public boolean isCellEditable(EventObject anEvent) {
            return !(anEvent instanceof MouseEvent) ||
                    ((MouseEvent) anEvent).getClickCount() >= 1;
        }

        /**
         * @param  anEvent  the event
         *
         * @return true to indicate that editing has begun.
         */
        @SuppressWarnings({"UnusedDeclaration"})
        public boolean startCellEditing(EventObject anEvent) {
            return true;
        }

        /**
         * Cancels editing.  This method calls <code>fireEditingCanceled</code>.
         */
        public void cancelCellEditing() {
            fireEditingCanceled();
        }

        /**
         * When an action is performed, editing is ended.
         * @param e the action event
         * @see #stopCellEditing
         */
        public void actionPerformed(ActionEvent e) {
            this.stopCellEditing();
        }

        /**
         * When an item's state changes, editing is ended.
         * @param e the action event
         * @see #stopCellEditing
         */
        public void itemStateChanged(ItemEvent e) {
            this.stopCellEditing();
        }
}
