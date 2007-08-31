/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import org.janelia.it.ims.imagerenamer.FileTableModel;
import org.janelia.it.ims.imagerenamer.PreviewImageFrame;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class supports the editing of button cells
 * within the file table.
 *
 * @author Eric Trautman
 */
public class ButtonEditor extends AbstractCellEditor
        implements TableCellEditor, ActionListener {

    private JTable fileTable;
    private JButton button;
    private int row;
    private int column;
    private static final int previewImageSize = 400;

    public ButtonEditor() {
    }

    public Object getCellEditorValue() {
        return button;
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        fileTable = table;
        this.row = row;
        this.column = column;
        button = (JButton) value;
        ActionListener[] listeners = this.button.getActionListeners();
        for (ActionListener listener : listeners) {
            button.removeActionListener(listener);
        }
        button.addActionListener(this);

        return button;
    }

    public void actionPerformed(ActionEvent e) {

        FileTableModel model = (FileTableModel) fileTable.getModel();
        final boolean isRemoveFileButton = (column == 0);
        final boolean isCopyButton = (column == 1);
        final boolean isPreviewButton = (column == 2);

        fireEditingStopped(); //Make the renderer reappear.

        if (isPreviewButton) {
            PreviewImageFrame previewImageFrame = new PreviewImageFrame(model.getRows().get(row).getFile(), previewImageSize);

        }
        if (isRemoveFileButton) {
            model.removeRow(row);
        } else if (isCopyButton) {
            model.copyRow((row - 1), row);
        }

        if (isCopyButton) {
            final int firstFieldCol = FileTableModel.getFirstFieldColumn();
            fileTable.changeSelection(row, firstFieldCol, false, false);
            fileTable.editCellAt(row, firstFieldCol);
        }

    }
}
