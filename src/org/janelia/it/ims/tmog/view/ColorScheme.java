/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.preferences.TransmogrifierPreferences;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Allows users to toggle between a light (default) and dark color scheme.
 *
 * @author Eric Trautman
 */
public class ColorScheme {

    private LookAndFeel lightLookAndFeel;
    private Map<String, Color> lightColorScheme;

    private LookAndFeel darkLookAndFeel;
    private Map<String, Color> darkColorScheme;

    private boolean isDark;
    private Set<Component> schemeComponents;

    public ColorScheme() {

        this.lightLookAndFeel = UIManager.getLookAndFeel();

        if (this.lightLookAndFeel instanceof MetalLookAndFeel) {
            this.darkLookAndFeel = this.lightLookAndFeel;
        } else {
            this.darkLookAndFeel = new MetalLookAndFeel();
        }

        this.lightColorScheme = new HashMap<String, Color>();
        this.darkColorScheme = new HashMap<String, Color>();

        for (String key : FOREGROUND_KEYS) {
            this.lightColorScheme.put(key, UIManager.getColor(key));
            this.darkColorScheme.put(key, WHITE);
        }

        for (String key : BACKGROUND_KEYS) {
            this.lightColorScheme.put(key, UIManager.getColor(key));
            this.darkColorScheme.put(key, BLACK);
        }

        for (String key : ALTERNATE_BACKGROUND_KEYS) {
            this.lightColorScheme.put(key, UIManager.getColor(key));
            this.darkColorScheme.put(key, DARK_GRAY);
        }

        this.isDark = false;
        this.schemeComponents = new HashSet<Component>();
    }

    /**
     * Add the specified parent/container component to the list of components
     * to be updated when the color scheme is changed.
     *
     * @param  c  parent/container component to be updated upon scheme changes.
     */
    public void addSchemeComponent(Component c) {
        schemeComponents.add(c);
    }

    /**
     * Toggle between the light and dark color schemes.
     */
    public void toggle() {

        LookAndFeel lookAndFeel;
        Map<String, Color> scheme;
        if (isDark) {
            isDark = false;
            lookAndFeel = lightLookAndFeel;
            scheme = lightColorScheme;
        } else {
            isDark = true;
            lookAndFeel = darkLookAndFeel;
            scheme = darkColorScheme;
        }

        for (String key : scheme.keySet()) {
            UIManager.put(key, scheme.get(key));
        }

        if (lightLookAndFeel != darkLookAndFeel) {
            try {
                UIManager.setLookAndFeel(lookAndFeel);
            } catch (UnsupportedLookAndFeelException e) {
                LOG.warn("failed to set look and feel", e);
            }
        }

        for (Component c : schemeComponents) {
            SwingUtilities.updateComponentTreeUI(c);
            c.repaint();
        }

        TransmogrifierPreferences tmogPreferences =
                TransmogrifierPreferences.getInstance();
        tmogPreferences.setColorScheme(isDark);
        tmogPreferences.save();
    }

    private static final Logger LOG = Logger.getLogger(ColorScheme.class);

    // NOTE: colors must be UI resources to allow for run-time changes

    private static final ColorUIResource WHITE = new ColorUIResource(Color.WHITE);
    private static final ColorUIResource BLACK = new ColorUIResource(Color.BLACK);
    private static final ColorUIResource DARK_GRAY = new ColorUIResource(Color.DARK_GRAY);

    // NOTE: menu bar keys are commented out so that the bar remains the same for both schemes

    private static final String[] FOREGROUND_KEYS =
            {"CheckBox.foreground", "CheckBoxMenuItem.foreground", "ColorChooser.foreground",
             "ComboBox.foreground", "EditorPane.foreground", "FormattedTextField.foreground",
             "Label.foreground", "List.foreground",
//             "Menu.foreground", "MenuBar.foreground", "MenuItem.foreground",
             "OptionPane.foreground", "OptionPane.messageForeground",
             "Panel.foreground", "PasswordField.foreground",
//             "PopupMenu.foreground",
             "ProgressBar.foreground", "RadioButton.foreground",
             "RadioButtonMenuItem.foreground", "ScrollBar.foreground", "ScrollPane.foreground",
             "Separator.foreground", "Slider.foreground", "Spinner.foreground",
             "Table.foreground", "TableHeader.foreground", "TextArea.foreground",
             "TextField.foreground", "TextPane.foreground", "ToggleButton.foreground",
             "ToolBar.foreground", "Tree.foreground", "Viewport.foreground",
             "controlText", "infoText", "menuText",
             "text", "textText", "windowText"};

    private static final String[] BACKGROUND_KEYS =
            {"CheckBox.background", "CheckBoxMenuItem.background", "ColorChooser.background",
             "ComboBox.background", "Desktop.background", "EditorPane.background",
             "FormattedTextField.background", "InternalFrame.background", "Label.background",
             "List.background", // file chooser
//             "Menu.background", "MenuBar.background", "MenuItem.background",
             "OptionPane.background", "Panel.background", "PasswordField.background",
//             "PopupMenu.background",
             "ProgressBar.background", "RadioButton.background",
             "RadioButtonMenuItem.background", "Slider.background", "Spinner.background",
             "SplitPane.background", "Table.background", "TextArea.background",
             "TextField.background", "TextPane.background", "ToggleButton.background",
             "ToolBar.background", "Tree.background",
             "Viewport.background"}; // data table

    private static final String[] ALTERNATE_BACKGROUND_KEYS =
            {"ScrollBar.background", "ScrollPane.background", "TableHeader.background"};

}
