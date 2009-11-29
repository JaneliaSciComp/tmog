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
public class ButtonPanel extends JPanel {

    public enum ButtonType {
        EXCLUDE_TARGET("/removeTarget.png", "exclude target"),
        ROW_MENU("/16-em-pencil.png", "show editing short cuts"),
        FIELD_GROUP_ROW_MENU("/16-em-pencil.png", "field group options");

        private String imagePath;
        private String toolTip;

        private ButtonType(String imagePath,
                           String toolTip) {
            this.imagePath = imagePath;
            this.toolTip = toolTip;
        }

        public String getImagePath() {
            return imagePath;
        }

        public String getToolTip() {
            return toolTip;
        }
    }

    /** The width of all data table buttons. */
    protected static final int BUTTON_WIDTH = 20;

    /** The height of all data table buttons. */
    protected static final int BUTTON_HEIGHT = 20;

    protected static final Dimension DEFAULT_SIZE =
            new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);

    private JButton button;

    public ButtonPanel(ButtonType buttonType) {
        super(new GridBagLayout());

        URL imageUrl = ButtonPanel.class.getResource(buttonType.getImagePath());
        this.button = getButton(imageUrl, DEFAULT_SIZE);
        final GridBagConstraints centerWithFixedSize = new GridBagConstraints();
        add(this.button, centerWithFixedSize);
        setBackground(Color.WHITE);
        setToolTipText(buttonType.getToolTip());
    }

    public JButton getButton() {
        return button;
    }

    private static JButton getButton(URL imageUrl,
                                     Dimension size) {
        final Icon icon = new ImageIcon(imageUrl);
        JButton b = new JButton(icon);
        b.setPreferredSize(size);
        b.setSize(size);
        b.setMaximumSize(size);
        return b;
    }

}
