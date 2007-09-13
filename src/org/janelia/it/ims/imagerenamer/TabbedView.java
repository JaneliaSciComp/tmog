/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer;

import org.apache.log4j.Logger;
import org.janelia.it.ims.imagerenamer.config.ConfigurationException;
import org.janelia.it.ims.imagerenamer.config.ProjectConfiguration;
import org.janelia.it.ims.imagerenamer.config.RenamerConfiguration;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class manages the tabbed view of rename sessions.
 *
 * @author Eric Trautman
 */
public class TabbedView implements ActionListener {

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(TabbedView.class);

    private JTabbedPane tabbedPane;
    private JMenuBar menuBar;
    private JPanel contentPanel;
    private Map<JMenuItem, ProjectConfiguration> addRenamerItems;
    private JMenuItem removeRenamerItem;
    private JMenuItem exitItem;
    private RenamerConfiguration renamerConfig;

    private HashMap<String, MainView> sessionList;
    private int sessionCount;

    public TabbedView() {
        this.sessionList = new HashMap<String, MainView>();
        this.sessionCount = 0;

        LoggingUtils.setLoggingContext();
        Runnable setContextInDispatchThread = new Runnable() {
            public void run() { LoggingUtils.setLoggingContext(); }
        };
        SwingUtilities.invokeLater(setContextInDispatchThread);

        LOG.info("starting renamer");

        String configFileName = "renamer_config.xml";
        renamerConfig = new RenamerConfiguration();
        try {
            renamerConfig.load(configFileName);
        } catch (ConfigurationException e) {
            LOG.error("Configuration Error", e);
            JOptionPane.showMessageDialog(contentPanel,
                                          e.getMessage(),
                                          "Configuration Error",
                                          JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        createMenuBar();
        ProjectConfiguration defaultProject =
                renamerConfig.getDefaultProjectConfiguration();
        if (defaultProject != null) {
            addSession(defaultProject);
        }
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public WindowListener getWindowListener() {
        return new WindowAdapter() {
            public void windowClosing(WindowEvent ev) {
                exitApplicationSafely();
            }
        };
    }

    private void exitApplicationSafely() {
        if (! hasActiveSessions(sessionList.keySet())) {
            System.exit(0);
        }        
    }

    private boolean hasActiveSessions(Collection<String> sessionNames) {
        ArrayList<String> sessionsInProgress = new ArrayList<String>();
        for (String sessionName : sessionNames) {
            MainView session = sessionList.get(sessionName);
            if (session.isRenameTaskInProgress()) {
                sessionsInProgress.add(sessionName);
            }
        }
        int numberOfRenameTasksInProgress = sessionsInProgress.size();
        if (numberOfRenameTasksInProgress > 0) {
            StringBuilder title = new StringBuilder();
            StringBuilder msg = new StringBuilder();
            title.append(numberOfRenameTasksInProgress);
            if (numberOfRenameTasksInProgress == 1) {
                title.append(" Session In Progress!");
                msg.append("Please wait until ");
                msg.append(sessionsInProgress.get(0));
                msg.append(" has completed.");
            } else {
                title.append(" Sessions In Progress!");
                msg.append("Please wait until the following sessions have completed: ");
                for (int i = 0; i < numberOfRenameTasksInProgress; i++) {
                    if (i > 0) {
                        msg.append(", ");
                    }
                    msg.append(sessionsInProgress.get(i));
                }
            }
            
            JOptionPane.showMessageDialog(contentPanel,
                                          msg.toString(),
                                          title.toString(),
                                          JOptionPane.WARNING_MESSAGE);
        }

        return (numberOfRenameTasksInProgress > 0);
    }

    private void createMenuBar() {
        JMenu menu = new JMenu("Menu");
        menu.setMnemonic(KeyEvent.VK_M);
        menuBar.add(menu);

        List<ProjectConfiguration> projectList = renamerConfig.getProjectList();
        addRenamerItems =
                new HashMap<JMenuItem, ProjectConfiguration>(projectList.size());
        for (ProjectConfiguration project : projectList) {
            JMenuItem addRenamerItem = new JMenuItem(
                    "Add '" + project.getName() + "' Rename Session");
            addRenamerItem.addActionListener(this);
            menu.add(addRenamerItem);
            addRenamerItems.put(addRenamerItem, project);
        }

        removeRenamerItem = new JMenuItem("Remove Current Rename Session",
                                          KeyEvent.VK_R);
        removeRenamerItem.addActionListener(this);
        menu.add(removeRenamerItem);

        menu.addSeparator();
        exitItem = new JMenuItem("Exit",
                                 KeyEvent.VK_E);
        exitItem.addActionListener(this);
        menu.add(exitItem);
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == removeRenamerItem) {
            removeSession();
        } else if (source == exitItem) {
            exitApplicationSafely();
        } else {
            JMenuItem addItem = (JMenuItem) source;
            ProjectConfiguration pConfig = addRenamerItems.get(addItem);
            if (pConfig != null) {
                addSession(pConfig);
            }
        }
    }

    private void addSession(ProjectConfiguration projectConfig) {
        File lsmDirectory = null;
        int currentTab = tabbedPane.getSelectedIndex();
        if (currentTab > -1) {
            String currentTitle = tabbedPane.getTitleAt(currentTab);
            MainView currentView = sessionList.get(currentTitle);
            lsmDirectory = currentView.getLsmDirectory();
        }
        MainView newView = new MainView(projectConfig, lsmDirectory);
        sessionCount++;
        String newTitle = "Session " + sessionCount;
        sessionList.put(newTitle, newView);
        tabbedPane.addTab(newTitle, newView.getPanel());
        int newSelectedIndex = tabbedPane.getTabCount() - 1;
        tabbedPane.setSelectedIndex(newSelectedIndex);
    }

    private void removeSession() {
        int currentTab = tabbedPane.getSelectedIndex();
        if (currentTab > -1) {
            String sessionTitle = tabbedPane.getTitleAt(currentTab);
            ArrayList<String> sessionNames = new ArrayList<String>();
            sessionNames.add(sessionTitle);
            if (! hasActiveSessions(sessionNames)) {
                sessionList.remove(sessionTitle);
                tabbedPane.removeTabAt(currentTab);
            }
        }
    }
}

