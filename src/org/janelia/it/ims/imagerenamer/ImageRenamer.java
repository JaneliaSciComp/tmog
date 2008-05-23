/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer;

import org.janelia.it.ims.imagerenamer.view.TabbedView;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * This class launches the renamer user interface.
 *
 * @author Eric Trautman
 * @author Peter Davies
 */
public class ImageRenamer extends JFrame {

    /**
     * Set up a thread pool to limit the number of concurrent
     * session tasks running at any given time.
     *
     * This pool was introduced to work around issues with large
     * numbers of concurrent transfers to Samba file shares.
     * These transfers would timeout and litter the file system with
     * partially transferred files.
     * The thread pool allows a user to queue up as many sessions
     * as they like, but will only execute 4 sessions at any given
     * time. 
     */
    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR =
            (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

    /**
     * Construct the application
     */
    public ImageRenamer() {
        super("Janelia Transmogrifier");
        TabbedView tabbedView = new TabbedView();
        setContentPane(tabbedView.getContentPanel());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(tabbedView.getWindowListener());
        pack();
    }

    /**
     * Submits the specified task to the application thread pool.
     *
     * @param  task  task to execute.
     */
    public static void submitTask(Runnable task) {
        THREAD_POOL_EXECUTOR.submit(task);
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
