/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view.component;

import org.janelia.it.ims.tmog.config.preferences.ColumnDefault;
import org.janelia.it.ims.tmog.config.preferences.ColumnDefaultSet;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.DataFieldGroupModel;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;


/**
 * This component renders and handles mouse events for
 * data field group columns.
 *
 * @author Eric Trautman
 */
public class DataFieldGroupHeader extends JPanel implements MouseInputListener {

    private String groupName;
    private DataTable dataTable;
    private DataTable fieldNameTable;
    private JTableHeader fieldNameTableHeader;
    private ColumnDefault columnDefault;

    /**
     * Constructs a data field group header.
     *
     * @param  groupModel  the model copy for the header.
     * @param  dataTable   the parent data table that contains this header.
     */
    public DataFieldGroupHeader(DataFieldGroupModel groupModel,
                                DataTable dataTable) {
        super(new BorderLayout());

        this.groupName = groupModel.getDisplayName();
        this.dataTable = dataTable;
        this.columnDefault = null;

        JLabel groupNameLabel;
        final List<DataField> firstRow = groupModel.getFirstRow();
        if (firstRow.size() > 1) {
            groupNameLabel = new JLabel(groupName);
        } else {
            groupNameLabel = new JLabel(" ");
        }
        groupNameLabel.setHorizontalAlignment(JLabel.CENTER);
        groupNameLabel.setFont(this.getFont());

        final DataTable mainDataTable = dataTable;
        this.fieldNameTable = new DataTable(false) {
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

        ColumnDefaultSet parentColumnDefaults = dataTable.getColumnDefaults();
        if (parentColumnDefaults != null) {
            this.columnDefault =
                    parentColumnDefaults.getColumnDefault(groupName);
            if (this.columnDefault != null) {
                ColumnDefaultSet nestedColumnDefaults =
                        this.columnDefault.getNestedColumnDefaults();
                fieldNameTable.setColumnDefaults(nestedColumnDefaults,
                                                 false);
            }
        }

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
     * @return the (column) name for this field group.
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * @return the default information for this group's nested columns.
     */
    public ColumnDefault getColumnDefault() {
        return columnDefault;
    }

    /**
     * Sets the default information for this group's nested columns.
     *
     * @param  columnDefault  the default for this field group column
     *                        (which should contain nested defaults) or
     *                        null if the default should be cleared.
     *
     * @param  updateDisplay  true if the header should be refreshed
     *                        immediately; false if it will be refreshed
     *                        by another process.
     */
    public void setColumnDefault(ColumnDefault columnDefault,
                                 boolean updateDisplay) {
        this.columnDefault = columnDefault;
        ColumnDefaultSet nestedDefaults;
        if (columnDefault == null) {
            nestedDefaults = null;
        } else {
            nestedDefaults = columnDefault.getNestedColumnDefaults();
        }
        fieldNameTable.setColumnDefaults(nestedDefaults, updateDisplay);
    }

    /**
     * Sets this group's nested column defaults based upon the current
     * view state.
     */
    public void setColumnDefaultsToCurrent() {
        fieldNameTable.setColumnDefaultsToCurrent();
        columnDefault = new ColumnDefault(groupName);
        columnDefault.addAllColumnDefaults(fieldNameTable.getColumnDefaults());
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