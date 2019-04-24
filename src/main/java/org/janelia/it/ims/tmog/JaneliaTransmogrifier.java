/*
 * Copyright (c) 2018 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.ConfigurationException;
import org.janelia.it.ims.tmog.config.ConfigurationLoadCompletionHandler;
import org.janelia.it.ims.tmog.config.ConfigurationLoader;
import org.janelia.it.ims.tmog.config.GlobalConfiguration;
import org.janelia.it.ims.tmog.config.TransmogrifierConfiguration;
import org.janelia.it.ims.tmog.config.preferences.TransmogrifierPreferences;
import org.janelia.it.ims.tmog.view.ColorScheme;
import org.janelia.it.ims.tmog.view.TabbedView;
import org.janelia.it.ims.tmog.view.component.NarrowOptionPane;

/**
 * This class launches the transmogrifier user interface.
 *
 * @author Eric Trautman
 * @author Peter Davies
 */
public class JaneliaTransmogrifier extends JFrame implements ConfigurationLoadCompletionHandler {

    /** The logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(JaneliaTransmogrifier.class);

    private static String version = JaneliaTransmogrifier.class.getPackage().getImplementationVersion();

    /**
     * @return the current version of this application.
     */
    public static String getVersion() {
        return version;
    }
    
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
    private JaneliaTransmogrifier(String configResource) {
        super("Janelia Transmogrifier " + getVersion());

        // attempt to load preferences
        TransmogrifierPreferences tmogPreferences =
                TransmogrifierPreferences.getInstance();
        try {
            tmogPreferences.load();
        } catch (ConfigurationException e) {
            LOG.error("Preferences Error", e);

            NarrowOptionPane.showMessageDialog(
                    this,
                    e.getMessage() + "  Consequently, all preferences related features will be disabled.",
                    "Preferences Features Disabled",
                    JOptionPane.WARNING_MESSAGE);
        }

        try {
            final URL configUrl = ConfigurationLoader.getConfigUrl(configResource);
            final ConfigurationLoader loader = new ConfigurationLoader(configUrl, this);
            loader.execute();
        } catch (Exception e) {
            LOG.error("Configuration Error", e);
            ConfigurationLoader.showConfigurationErrorDialog(this, e);
            System.exit(1);
        }

    }

    @Override
    public void handleConfigurationLoadSuccess(TransmogrifierConfiguration config) {
        TransmogrifierPreferences tmogPreferences =
                TransmogrifierPreferences.getInstance();

        ColorScheme colorScheme = new ColorScheme();
        if (tmogPreferences.isDarkColorScheme()) {
            colorScheme.toggle();
        }
        colorScheme.addSchemeComponent(this);

        TabbedView tabbedView = new TabbedView(colorScheme, config);
        setContentPane(tabbedView.getContentPanel());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(tabbedView.getWindowListener());
        JMenuBar menuBar = tabbedView.getMenuBar();
        this.setJMenuBar(menuBar);
        colorScheme.addSchemeComponent(menuBar);
        pack();

        // size the window
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = getSize();

        final GlobalConfiguration globalConfig = config.getGlobalConfiguration();
        Integer frameSizePct = globalConfig.getFrameSizePercentage();
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

        final double frameSizeFactor = (double) frameSizePct / 100;
        frameSize.height = (int) (screenSize.height * frameSizeFactor);
        frameSize.width = (int) (screenSize.width * frameSizeFactor);

        // hack for dual screens
        final int maxWidth = (int) (1920 * frameSizeFactor);
        if (frameSize.width > maxWidth) {
            frameSize.width = maxWidth;
        }

        setSize(frameSize.width, frameSize.height);
        setPreferredSize(frameSize);

        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    @Override
    public void handleConfigurationLoadFailure(Exception failure) {
        System.exit(1);
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

            final String userName = System.getProperty("user.name");
            final String userDir = System.getProperty("user.dir");
            LOG.info("connected as user '" + userName +
                     "' with working directory " + userDir );

            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            String javaVersion = System.getProperty("java.runtime.version");

            // work around Mac Leopard bug with combo boxes
            // see http://www.randelshofer.ch/quaqua/ for another option

            // work around Ubuntu 9.04 bug with tables
            //     Exception in thread "AWT-EventQueue-0" 
            //     java.lang.NullPointerException at
            //     javax.swing.plaf.synth.SynthTableUI.paintCell
            //     (SynthTableUI.java:623)

            if (osName.startsWith("Mac") || osName.equals("Linux")) {
                LOG.info("use Metal look and feel for java " +
                         javaVersion + " on " + osName +
                         " (" + osVersion + ")");
                lookAndFeelClassName = MetalLookAndFeel.class.getName();
            } else {
                LOG.info("use Native look and feel for java " +
                         javaVersion + " on " + osName +
                         " (" + osVersion + ")");
            }

            UIManager.setLookAndFeel(lookAndFeelClassName);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        LOG.info("starting Janelia Transmogrifier version " + getVersion());

        String configPath = null;
        if (args.length > 0) {
            configPath = args[0];
            if ("select_config".equals(configPath)) {
                configPath = null;
            }
        }
        JaneliaTransmogrifier frame = new JaneliaTransmogrifier(configPath);

        final java.util.List<Image> iconList = new ArrayList<>();

        final int[] iconSizes = { 16, 24, 32, 48, 64, 96, 128, 256, 512 };
        for (final int iconSize : iconSizes) {
            final String name = String.format("/images/tmog_%dx%d.png", iconSize, iconSize);
            final URL imageUrl = JaneliaTransmogrifier.class.getResource(name);
            iconList.add(new ImageIcon(imageUrl, "Janelia Transmogrifier").getImage());
        }
        frame.setIconImages(iconList);

    }

}
