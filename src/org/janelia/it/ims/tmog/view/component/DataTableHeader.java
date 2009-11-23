/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.view.component;

import org.janelia.it.ims.tmog.TransmogrifierTableModel;
import org.janelia.it.ims.tmog.field.DataFieldGroupModel;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;


/**
 * This customized header supports rendering and mouse event handling for
 * field group column headers in a data table.
 *
 * @author Eric Trautman
 */
public class DataTableHeader
        extends JTableHeader {

    /** Maps field group column indexes to their header components. */
    private Map<Integer, DataFieldGroupHeader> columnToGroupHeader;

    /** Common renderer to use for field group columns. */
    private TableCellRenderer groupHeaderRenderer;

    /**
     * Constructs a table header with the specified column model.
     *
     * @param  columnModel  column model for the table.
     */
    public DataTableHeader(TableColumnModel columnModel) {
        super(columnModel);
        setReorderingAllowed(false);
        setResizingAllowed(true);
        columnToGroupHeader = new HashMap<Integer, DataFieldGroupHeader>();
        groupHeaderRenderer = new TableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                return columnToGroupHeader.get(column);
            }
        };
    }

    @Override
    public void updateUI(){
        // override to use custom UI implementation
        setUI(new DataTableHeaderUI(this));
        resizeAndRepaint();
        invalidate();
    }

    /**
     * Updates this header based upon the specified model.
     *
     * @param  tableModel  new model for this header's table.
     */
    public void updateModel(TransmogrifierTableModel tableModel) {

        // detach old group headers from this component
        for (DataFieldGroupHeader groupHeader : columnToGroupHeader.values()) {
            remove(groupHeader);
        }

        columnToGroupHeader = new HashMap<Integer, DataFieldGroupHeader>();

        final int numColumns = tableModel.getColumnCount();
        if (tableModel.getRowCount() > 0) {
            for (int columnIndex = 0; columnIndex < numColumns; columnIndex++) {
                Object cellValue = tableModel.getValueAt(0, columnIndex);
                if (cellValue instanceof DataFieldGroupModel) {
                    DataFieldGroupModel groupModel =
                            ((DataFieldGroupModel) cellValue).getNewInstance(false);

                    DataFieldGroupHeader groupHeader =
                            new DataFieldGroupHeader(groupModel,
                                                     (DataTable) getTable());

                    columnToGroupHeader.put(columnIndex, groupHeader);

                    TableColumn column = columnModel.getColumn(columnIndex);
                    column.setHeaderValue(groupHeader);
                    column.setHeaderRenderer(groupHeaderRenderer);
                }
            }
        }        
    }

    /**
     * Restores the relationship between this header's field group header
     * components and the header itself.  This needs to be called whenever
     * the data table header is painted because the field group headers also
     * serve as renderers.  During the rendering process, renderers get
     * attached to the table's renderer pane and detached from any previous
     * parent.  This method restores the original parent relationship allowing
     * events (like mouse moved, dragged, ...) to be properly forwarded to
     * the field group header component.
     */
    public void restoreFieldGroupHeaders() {
        DataFieldGroupHeader groupHeader;
        for (Integer columnIndex : columnToGroupHeader.keySet()) {
            groupHeader = columnToGroupHeader.get(columnIndex);
            groupHeader.setBounds(getHeaderRect(columnIndex));
            add(groupHeader);
            groupHeader.validate();
        }
    }

}