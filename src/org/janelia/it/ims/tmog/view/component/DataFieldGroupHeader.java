/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.view.component;

import org.janelia.it.ims.tmog.field.DataFieldGroupModel;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseEvent;


/**
 * This component renders and handles mouse events for
 * data field group columns.
 *
 * @author Eric Trautman
 */
public class DataFieldGroupHeader extends JPanel implements MouseInputListener {

    private DataTable dataTable;
    private JTableHeader fieldNameTableHeader;

    /**
     * Constructs a data field group header.
     *
     * @param  groupModel  the model copy for the header.
     * @param  dataTable   the parent data table that contains this header.
     */
    public DataFieldGroupHeader(DataFieldGroupModel groupModel,
                                DataTable dataTable) {
        super(new BorderLayout());

        this.dataTable = dataTable;

        final JLabel groupNameLabel =
                new JLabel(groupModel.getDisplayName());
        groupNameLabel.setHorizontalAlignment(JLabel.CENTER);
        groupNameLabel.setFont(this.getFont());

        final DataTable mainDataTable = dataTable;
        DataTable fieldNameTable = new DataTable() {
            // use original JTable implementation to correct sizing
            // (not sure why I had to do this ...)
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return !(autoResizeMode == AUTO_RESIZE_OFF);
            }            

            // repaint the main data table associated with this header
            // (so that header resize events get propagated)
            @Override
            protected void repaintTableForHeaderClick() {
                super.repaintTableForHeaderClick();
                mainDataTable.repaint();
            }
        };

        fieldNameTable.setModel(groupModel);

        JScrollPane tablePane =
                new JScrollPane(fieldNameTable,
                                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        fieldNameTableHeader = fieldNameTable.getTableHeader();
        fieldNameTableHeader.setResizingAllowed(true);
        fieldNameTableHeader.addMouseListener(this);
        fieldNameTableHeader.addMouseMotionListener(this);

        final Dimension tableHeaderSize =
                fieldNameTableHeader.getPreferredSize();
        final Dimension paneSize = new Dimension(tableHeaderSize.width,
                                                 tableHeaderSize.height + 2);
        tablePane.setPreferredSize(paneSize);
        add(groupNameLabel, BorderLayout.NORTH);
        add(tablePane, BorderLayout.CENTER);

        // don't overlap header cell border with this panel
        add(Box.createHorizontalStrut(2), BorderLayout.EAST);

        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
    }

    /**
     * @param  columnIndex  index of the desired column (field).
     *
     * @return the current width of the specified column in this header.
     */
    public int getColumnWidth(int columnIndex) {
        final TableColumnModel columnModel =
                fieldNameTableHeader.getColumnModel();
        final TableColumn column = columnModel.getColumn(columnIndex);
        return column.getWidth();
    }

    // MouseInputListener implementations

    public void mouseDragged(MouseEvent e) {
        TableColumn column = fieldNameTableHeader.getResizingColumn();
        if (column != null) {
            // repainting the table forces field group cells to resize
            // their width to the widths defined by this header
            dataTable.repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        // TODO: look into how to resize editor cell without stopping edit
        if (dataTable.isEditing()) {
            // stop edit to ensure all cells get resized
            dataTable.getCellEditor().stopCellEditing();
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }
}