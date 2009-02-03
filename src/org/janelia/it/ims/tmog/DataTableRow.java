/*
 * Copyright ? 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog;

import org.janelia.it.ims.tmog.field.DataField;

import javax.swing.*;
import java.net.URL;
import java.util.List;

/**
 * This class encapsulates the model data for one row in the data table.
 *
 * @author Eric Trautman
 */
public class DataTableRow {

    private static final URL REMOVE_IMAGE =
            DataTableRow.class.getResource("/removeTarget.png");
    private static final Icon REMOVE_ICON = new ImageIcon(REMOVE_IMAGE);
    private static final String REMOVE_TIP = "exclude target";

    private static final URL COPY_IMAGE =
            DataTableRow.class.getResource("/copyArrowSimple.png");
    private static final Icon COPY_ICON = new ImageIcon(COPY_IMAGE);
    private static final String COPY_TIP = "copy values from previous row";

    private JButton removeButton;
    private JButton copyButton;
    private DataRow dataRow;

    public DataTableRow(Target target,
                        List<DataField> dataFieldConfigs) {

        this.removeButton = new JButton(REMOVE_ICON);
        this.removeButton.setToolTipText(REMOVE_TIP);

        this.copyButton = new JButton(COPY_ICON);
        this.copyButton.setToolTipText(COPY_TIP);

        this.dataRow = new DataRow(target);
        for (DataField dataFieldConfig : dataFieldConfigs) {
            DataField newFieldInstance =
                    dataFieldConfig.getNewInstance(false);
            this.dataRow.addField(newFieldInstance);
            newFieldInstance.initializeValue(target);
        }
    }

    public JButton getRemoveButton() {
        return removeButton;
    }

    public JButton getCopyButton() {
        return copyButton;
    }

    public DataRow getDataRow() {
        return dataRow;
    }

    public Target getTarget() {
        return dataRow.getTarget();
    }

    public List<DataField> getFields() {
        return dataRow.getFields();
    }

    public DataField getField(int fieldIndex) {
        return dataRow.getField(fieldIndex);
    }

    public void setField(int fieldIndex,
                         DataField field) {
        dataRow.setField(fieldIndex, field);
    }

    public int getFieldCount() {
        return dataRow.getFieldCount();
    }
}