/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.view.component.ButtonPanel;
import org.janelia.it.ims.tmog.view.component.ButtonPanel.ButtonType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This class supports the rendering of a button cells
 * within the file table.
 *
 * @author Eric Trautman
 */
public class ButtonRenderer extends DefaultTableCellRenderer {

    private Map<ButtonType, ButtonPanel> typeToPanelMap;

    public ButtonRenderer() {
        this.typeToPanelMap = new HashMap<ButtonType, ButtonPanel>();
        for (ButtonType buttonType : ButtonType.values()) {
            this.typeToPanelMap.put(buttonType, new ButtonPanel(buttonType));
        }
    }

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        Component cellRenderer;
        if (value instanceof ButtonType) {
            ButtonType buttonType = (ButtonType) value;
            cellRenderer = typeToPanelMap.get(buttonType);
        } else {
            cellRenderer =
                    super.getTableCellRendererComponent(table,
                                                        value,
                                                        isSelected,
                                                        hasFocus,
                                                        row,
                                                        column);
        }
        return cellRenderer;
    }

}
