/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer;

import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.*;

/**
 * This class launches the renamer user interface.
 *
 * @author Peter Davies
 */
public class ImageRenamer extends JFrame {

    /**
     * Construct the application
     */
    public ImageRenamer() {
        super("Janelia Farm LSM Image Renamer");
        TabbedView tabbedView = new TabbedView();
        setContentPane(tabbedView.getContentPanel());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(tabbedView.getWindowListener());
        pack();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        JFrame frame = new ImageRenamer();

        //Center the window
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();

        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }

        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }

        frame.setLocation((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);

        frame.setVisible(true);
    }

}
