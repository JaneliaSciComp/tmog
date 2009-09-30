/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.GlobalConfiguration;
import org.janelia.it.ims.tmog.config.TransmogrifierConfiguration;
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
     * The current version of this application.
     * This needs to be kept in sync with the version in build.properties
     * until I find a better way to dynamically populate this when the
     * app is run from within an IDE (package information can be used
     * when the app is run from a jar file). 
     */
    public static final String VERSION = "2.2.9";
    
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

    private Integer frameSizePercentage;

    /**
     * Construct the application
     */
    public JaneliaTransmogrifier() {
        super("Janelia Transmogrifier " + VERSION);
        TabbedView tabbedView = new TabbedView();
        setContentPane(tabbedView.getContentPanel());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(tabbedView.getWindowListener());
        JMenuBar menuBar = tabbedView.getMenuBar();
        this.setJMenuBar(menuBar);
        pack();

        TransmogrifierConfiguration config = tabbedView.getTmogConfig();
        GlobalConfiguration globalConfig = config.getGlobalConfiguration();
        this.frameSizePercentage = globalConfig.getFrameSizePercentage();
    }

    public Integer getFrameSizePercentage() {
        return frameSizePercentage;
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

            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");

            boolean isMacLeopard1_5 = false;
            String javaVersion = System.getProperty("java.runtime.version");
            if ((javaVersion != null) && (javaVersion.startsWith("1.5"))) {
                if (osName != null) {
                    isMacLeopard1_5 =
                            osName.startsWith("Mac") &&
                            (osVersion != null) && osVersion.startsWith("10");
                }
            }

            // work around Mac Leopard bug with combo boxes
            // see http://www.randelshofer.ch/quaqua/ for another option

            // work around Ubuntu 9.04 bug with tables
            //     Exception in thread "AWT-EventQueue-0" 
            //     java.lang.NullPointerException at
            //     javax.swing.plaf.synth.SynthTableUI.paintCell
            //     (SynthTableUI.java:623)

            if (isMacLeopard1_5 || osName.equals("Linux")) {
                LOG.info("use Metal look and feel for java " +
                         javaVersion + " on " + osName +
                         " (" + osVersion + ")");
                lookAndFeelClassName = MetalLookAndFeel.class.getName();
            }

            UIManager.setLookAndFeel(lookAndFeelClassName);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        Package pkg = JaneliaTransmogrifier.class.getPackage();
        String revision = pkg.getImplementationVersion();
        if (revision == null) {
            revision = "?";
        }
        LOG.info("starting Janelia Transmogrifier version " + VERSION +
                 ", revision " + revision);

        JaneliaTransmogrifier frame = new JaneliaTransmogrifier();
        frame.setIconImage(APP_ICON.getImage());

        // size the window
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();

        Integer frameSizePct = frame.getFrameSizePercentage();
        final int minPct = 40;
        final int defaultPct = 80;
        final int maxPct = 99;
        if (frameSizePct == null) {
            frameSizePct = defaultPct;
        } else if (frameSizePct < minPct) {
            frameSizePct = minPct;
        } else if (frameSizePct > maxPct) {
            frameSizePct = maxPct;
        }

        @SuppressWarnings({"RedundantCast"})
        final double frameSizeFactor = (double) frameSizePct / 100;
        frameSize.height = (int) (screenSize.height * frameSizeFactor);
        frameSize.width = (int) (screenSize.width * frameSizeFactor);

        // hack for dual screens
        final int maxWidth = (int) (1920 * frameSizeFactor);
        if (frameSize.width > maxWidth) {
            frameSize.width = maxWidth;
        }

        frame.setSize(frameSize.width, frameSize.height);
        frame.setPreferredSize(frameSize);

        frame.setVisible(true);
    }

}
