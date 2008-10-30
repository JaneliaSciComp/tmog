/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.view.TabbedView;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * This class launches the transmogrifier user interface.
 *
 * @author Eric Trautman
 * @author Peter Davies
 */
public class JaneliaTransmogrifier extends JFrame {

    /** The logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(JaneliaTransmogrifier.class);

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

    private static final URL APP_IMAGE_URL =
            JaneliaTransmogrifier.class.getResource("/transmogrifier_icon.png");
    private static final ImageIcon APP_ICON =
            new ImageIcon(APP_IMAGE_URL, "Janelia Transmogrifier");

    /**
     * Construct the application
     *
     * @param  version  the application version number to be used in the title. 
     */
    public JaneliaTransmogrifier(String version) {
        super("Janelia Transmogrifier " + version);
        TabbedView tabbedView = new TabbedView();
        setContentPane(tabbedView.getContentPanel());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(tabbedView.getWindowListener());
        JMenuBar menuBar = tabbedView.getMenuBar();
        this.setJMenuBar(menuBar);

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
            String lookAndFeelClassName =
                    UIManager.getSystemLookAndFeelClassName();

            // work around Mac Leopard bug with combo boxes
            // see http://www.randelshofer.ch/quaqua/ for another option
            String javaVersion = System.getProperty("java.runtime.version");
            if ((javaVersion != null) && (javaVersion.startsWith("1.5"))) {
                String osName = System.getProperty("os.name");
                if ((osName != null) && osName.startsWith("Mac")) {
                    String osVersion = System.getProperty("os.version");
                    if ((osVersion != null) && osVersion.startsWith("10")) {
                        LOG.info("use Metal look and feel for java " + 
                                 javaVersion + " on " + osName +
                                 " (" + osVersion + ")");
                        lookAndFeelClassName = MetalLookAndFeel.class.getName();
                    }
                }
            }

            UIManager.setLookAndFeel(lookAndFeelClassName);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        Package pkg = JaneliaTransmogrifier.class.getPackage();
        String version = pkg.getSpecificationVersion();
        if (version == null) {
            version = "?";
        }
        String revision = pkg.getImplementationVersion();
        if (revision == null) {
            revision = "?";
        }
        LOG.info("starting Janelia Transmogrifier version " + version +
                 ", revision " + revision);

        JFrame frame = new JaneliaTransmogrifier(version);
        frame.setIconImage(APP_ICON.getImage());

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
