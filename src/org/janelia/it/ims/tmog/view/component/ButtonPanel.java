package org.janelia.it.ims.tmog.view.component;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * This class provides the common cell buttons used in data tables.
 * These buttons are "wrapped" in a panel so that they don't get resized
 * to the data table's cell size.
 *
 * @author Eric Trautman
 */
public class ButtonPanel {

    /** The width of all data table buttons. */
    protected static final int BUTTON_WIDTH = 20;

    /** The height of all data table buttons. */
    protected static final int BUTTON_HEIGHT = 20;

    private static final Dimension DEFAULT_SIZE =
            new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);

    public static final ButtonPanel ADD_ROW = new ButtonPanel(
            getButton(ButtonPanel.class.getResource("/addRow.png"),
                      DEFAULT_SIZE),
            "add new row");

    public static final ButtonPanel COPY_PREVIOUS_ROW = new ButtonPanel(
            getButton(ButtonPanel.class.getResource("/copyArrowSimple.png"),
                      DEFAULT_SIZE),
            "copy values from previous row");

    public static final ButtonPanel DELETE_ROW = new ButtonPanel(
            getButton(ButtonPanel.class.getResource("/deleteRow.png"),
                      DEFAULT_SIZE),
            "delete row");

    public static final ButtonPanel EXCLUDE_TARGET = new ButtonPanel(
            getButton(ButtonPanel.class.getResource("/removeTarget.png"),
                      DEFAULT_SIZE),
            "exclude target");

    private JPanel panel;
    private JButton button;

    private ButtonPanel(JButton button,
                        String tip) {

        this.button = button;
        this.panel = new JPanel(new GridBagLayout());
        final GridBagConstraints centerWithFixedSize = new GridBagConstraints();
        this.panel.add(this.button, centerWithFixedSize);

        this.panel.setBackground(Color.WHITE);
        this.panel.setToolTipText(tip);
    }

    public JPanel getPanel() {
        return panel;
    }

    public JButton getButton() {
        return button;
    }

    public boolean isAddRow() {
        return this == ADD_ROW;
    }

    public boolean isCopyPreviousRow() {
        return this == COPY_PREVIOUS_ROW;
    }

    public boolean isDeleteRow() {
        return (this == EXCLUDE_TARGET) || (this == DELETE_ROW);
    }

    private static JButton getButton(URL imageUrl,
                                     Dimension size) {
        final Icon icon = new ImageIcon(imageUrl);
        JButton b = new JButton(icon);
        b.setPreferredSize(size);
        b.setSize(size);
        b.setMaximumSize(size);
        b.setBorderPainted(false);
        return b;
    }

}
