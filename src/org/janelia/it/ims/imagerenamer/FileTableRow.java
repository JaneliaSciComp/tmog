/*
 * Copyright ? 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer;

import org.janelia.it.ims.imagerenamer.field.RenameField;
import org.janelia.it.ims.imagerenamer.field.SourceFileField;

import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * This class encapsulates the model data for one row in the file table.
 *
 * @author Eric Trautman
 */
public class FileTableRow {

    private static final URL REMOVE_IMAGE =
            FileTableRow.class.getResource("/removeFile.png");
    private static final Icon REMOVE_ICON = new ImageIcon(REMOVE_IMAGE);
    private static final String REMOVE_TIP = "exclude file from renaming";

    private static final URL COPY_IMAGE =
            FileTableRow.class.getResource("/copyArrowSimple.png");
    private static final Icon COPY_ICON = new ImageIcon(COPY_IMAGE);
    private static final String COPY_TIP = "copy values from previous row";

    private JButton removeFileButton;
    private JButton copyButton;
    private File file;
    private RenameField[] fields;

    public FileTableRow(File file,
                        List<RenameField> renameFieldConfigs) {

        this.removeFileButton = new JButton(REMOVE_ICON);
        this.removeFileButton.setToolTipText(REMOVE_TIP);

        this.copyButton = new JButton(COPY_ICON);
        this.copyButton.setToolTipText(COPY_TIP);

        this.file = file;

        this.fields = new RenameField[renameFieldConfigs.size()];
        int fieldIndex = 0;
        for (RenameField renameFieldConfig : renameFieldConfigs) {
            RenameField newFieldInstance = renameFieldConfig.getNewInstance();
            this.fields[fieldIndex] = newFieldInstance;
            if (newFieldInstance instanceof SourceFileField) {
                SourceFileField sourceFileField =
                        (SourceFileField) newFieldInstance;
                sourceFileField.deriveSourceFileValues(file);
            }
            fieldIndex++;
        }
    }

    public JButton getRemoveFileButton() {
        return removeFileButton;
    }

    public JButton getCopyButton() {
        return copyButton;
    }

    public File getFile() {
        return file;
    }

    public RenameField[] getFields() {
        return fields;
    }

    public RenameField getField(int fieldIndex) {
        return fields[fieldIndex];
    }

    public void setField(int fieldIndex,
                         RenameField field) {
        fields[fieldIndex] = field;
    }

    public int getFieldCount() {
        return fields.length;
    }
}