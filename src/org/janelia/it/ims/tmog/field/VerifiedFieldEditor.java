/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.view.component.NarrowOptionPane;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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

        private Component dialogParent;
        private JTextField textField;
        private VerifiedFieldModel verifiedFieldModel;
        boolean isNextCellSelectValid;
        private JTable table;

        public VerifiedFieldEditor(Component dialogParent) {
            this.dialogParent = dialogParent;
            this.textField = new JTextField();
            this.textField.addActionListener(this);
            this.isNextCellSelectValid = true;
        }

        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            if (value instanceof VerifiedFieldModel) {
                verifiedFieldModel = (VerifiedFieldModel) value;
                textField.setDocument(verifiedFieldModel);
                textField.setBorder(new LineBorder(Color.gray));
                if (isSelected) {
                    textField.selectAll();
                }
                this.table = table;
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
            boolean isEditingStopped = true;

            if (! verifiedFieldModel.verify()) {

                String dialogMsg = verifiedFieldModel.getErrorMessage() +
                        "  Would you like to correct the field now?";

                int selection = NarrowOptionPane.showConfirmDialog(
                        dialogParent,
                        dialogMsg,
                        "Invalid Entry", // title
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (selection == JOptionPane.YES_OPTION)  {
                    textField.setBorder(new LineBorder(Color.red));
                    textField.selectAll();
                    textField.requestFocusInWindow();
                    isEditingStopped = false;
                    isNextCellSelectValid = false;
                } else {
                    // TODO: determine how to restore key events
                }
            }

            if (isEditingStopped) {
                fireEditingStopped();
                if (verifiedFieldModel.isSharedForAllSessionFiles()) {
                    table.repaint();
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
         * Returns true to indicate that the editing cell may
         * be selected.
         *
         * @param   anEvent         the event
         * @return  true
         * @see #isCellEditable
         */
         public boolean shouldSelectCell(EventObject anEvent) {
            boolean isCurrentSelectValid = isNextCellSelectValid;
            isNextCellSelectValid = true;
            return isCurrentSelectValid;
         }

        /**
         * @param  anEvent  the event
         *
         * @return true to indicate that editing has begun.
         */
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
