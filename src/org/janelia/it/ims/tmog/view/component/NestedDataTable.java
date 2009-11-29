/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.view.component;

import org.janelia.it.ims.tmog.TransmogrifierTableModel;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * This component supports the display of a data table nested within
 * another data table.
 *
 * @author Eric Trautman
 */
public class NestedDataTable extends DataTable {

    private DataTable parentTable;
    private Integer parentColumnIndex;


    /**
     * Constructs a data table associated with the specified parent.
     *
     * @param  parentTable  parent table that contains this table.
     */
    public NestedDataTable(DataTable parentTable) {
        super(false);

        if (parentTable == null) {
            throw new IllegalArgumentException(
                    "nested data tables must have a defined parent table");
        }

        this.parentTable = parentTable;
        this.parentColumnIndex = null;

        // reuse parent table default renderers and editors
        TableCellRenderer renderer;
        TableCellEditor editor;
        for (Class clazz : columnClasses) {
            renderer = parentTable.getDefaultRenderer(clazz);
            if (renderer != null) {
                setDefaultRenderer(clazz, renderer);
            }
            editor = parentTable.getDefaultEditor(clazz);
            if (editor != null) {
                setDefaultEditor(clazz, editor);
            }
        }
    }

    /**
     * Sets the data model for this table and then resizes all columns
     * and rows.  The parent column index needs to be specified so that
     * the nested table's column widths can be synchronized with the
     * widths defined in the parent column's header.
     *
     * @param  dataModel          table data model.
     * @param  parentColumnIndex  the number of the parent table column
     *                            in which this table is nested.
     */
    public void setModel(TableModel dataModel,
                         int parentColumnIndex) {
        this.parentColumnIndex = parentColumnIndex;
        super.setModel(dataModel);
    }

    /**
     * @return the data panel parent for dialogs (so that centering doesn't
     *         get messed up by wide tables with horizontal scroll bars).
     */
    @Override
    public Container getDialogParent() {
        Container parent = null;
        if (parentTable != null) {
            parent = parentTable.getDialogParent();
        }
        return parent;
    }

    /**
     * Resizes this table's column widths based upon the corresponding
     * widths of its parent column header.
     *
     * @param  tmogModel  the table's data model.
     */
    protected void resizeAllColumnWidths(TransmogrifierTableModel tmogModel) {
        if (parentTable == null) {
            super.resizeAllColumnWidths(tmogModel);
        } else {
            // column sizes should be synchronized with the column header
            final TableColumnModel parentColumnModel =
                    parentTable.getColumnModel();
            final TableColumn parentColumn =
                    parentColumnModel.getColumn(parentColumnIndex);
            final Object parentHeaderValue = parentColumn.getHeaderValue();
            if (parentHeaderValue instanceof DataFieldGroupHeader) {
                DataFieldGroupHeader groupHeader =
                        (DataFieldGroupHeader) parentHeaderValue;
                TableColumnModel colModel = getColumnModel();
                colModel.setColumnMargin(CELL_MARGIN);
                TableColumn tableColumn;
                int headerColumnWidth;
                final int numColumns = tmogModel.getColumnCount();
                for (int columnIndex = 0; columnIndex < numColumns; columnIndex++) {
                    tableColumn = colModel.getColumn(columnIndex);
                    headerColumnWidth = groupHeader.getColumnWidth(columnIndex);
                    tableColumn.setPreferredWidth(headerColumnWidth);
                }
            }
        }
    }

    /**
     * A nested table propagates boundary selections
     * (select next from the last row/column and
     * select previous from the first row/column)
     * to its parent table rather than wrapping.
     *
     * @param  isNext  true for select next events;
     *                 false for select previous events.
     *
     * @return true if the event was propagated (handled).
     */
    @Override
    protected boolean propagateSelection(boolean isNext) {
        boolean handled = false;
        if (parentTable != null) {
            if (isNext) {
                parentTable.selectAndEditNextCell();
            } else {
                parentTable.selectAndEditPreviousCell();
            }
            handled = true;
        }
        return handled;
    }
}