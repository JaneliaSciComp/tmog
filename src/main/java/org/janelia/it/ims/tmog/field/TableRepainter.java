/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * This class supports refreshing a table's entire view for components
 * that are rendered in more than one cell.
 *
 * @author Eric Trautman
 */
public class TableRepainter implements ListDataListener {

    private JTable table;

    public TableRepainter(JTable table) {
        if (table == null) {
            throw new IllegalArgumentException("null table specified");
        }
        this.table = table;
    }

    public boolean equals(Object o) {
        boolean isEqual = false;
        if (o instanceof TableRepainter) {
            isEqual = (table == ((TableRepainter) o).table);
        }
        return isEqual;
    }

    public int hashCode() {
        return table.hashCode();
    }

    // ----- ListDataListener methods 
    public void intervalAdded(ListDataEvent e) {
    }

    public void intervalRemoved(ListDataEvent e) {
    }

    public void contentsChanged(ListDataEvent e) {
        table.repaint();
    }
}