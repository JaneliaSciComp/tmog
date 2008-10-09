/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.view;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.ConfigurationException;
import org.janelia.it.ims.tmog.config.ProjectConfiguration;
import org.janelia.it.ims.tmog.config.TransmogrifierConfiguration;
import org.janelia.it.ims.tmog.view.component.NarrowOptionPane;
import org.janelia.it.utils.LoggingUtils;

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
 * This class manages the tabbed view of transmogrifier sessions.
 *
 * @author Eric Trautman
 */
public class TabbedView implements ActionListener {

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(TabbedView.class);

    private JTabbedPane tabbedPane;
    private JMenuBar menuBar;
    private JPanel contentPanel;
    private Map<JMenuItem, ProjectConfiguration> addSessionItems;
    private JMenuItem removeSessionItem;
    private JMenuItem exitItem;
    private TransmogrifierConfiguration tmogConfig;

    private HashMap<String, SessionView> sessionList;
    private int sessionCount;

    public TabbedView() {
        this.sessionList = new HashMap<String, SessionView>();
        this.sessionCount = 0;

        LoggingUtils.setLoggingContext();
        Runnable setContextInDispatchThread = new Runnable() {
            public void run() { LoggingUtils.setLoggingContext(); }
        };
        SwingUtilities.invokeLater(setContextInDispatchThread);

        LOG.info("starting transmogrifier");

        String configFileName = "transmogrifier_config.xml";
        tmogConfig = new TransmogrifierConfiguration();
        try {
            tmogConfig.load(configFileName);
        } catch (ConfigurationException e) {
            LOG.error("Configuration Error", e);
            NarrowOptionPane.showMessageDialog(contentPanel,
                                               e.getMessage(),
                                               "Configuration Error",
                                               JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        createMenuBar();
        ProjectConfiguration defaultProject =
                tmogConfig.getDefaultProjectConfiguration();
        if (defaultProject != null) {
            addSession(defaultProject);
        }
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public JMenuBar getMenuBar() {
        return menuBar;
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
            SessionView session = sessionList.get(sessionName);
            if (session.isTaskInProgress()) {
                sessionsInProgress.add(sessionName);
            }
        }
        int numberOfTasksInProgress = sessionsInProgress.size();
        if (numberOfTasksInProgress > 0) {
            StringBuilder title = new StringBuilder();
            StringBuilder msg = new StringBuilder();
            title.append(numberOfTasksInProgress);
            if (numberOfTasksInProgress == 1) {
                title.append(" Session In Progress!");
                msg.append("Please wait until ");
                msg.append(sessionsInProgress.get(0));
                msg.append(" has completed.");
            } else {
                title.append(" Sessions In Progress!");
                msg.append("Please wait until the following sessions have completed: ");
                for (int i = 0; i < numberOfTasksInProgress; i++) {
                    if (i > 0) {
                        msg.append(", ");
                    }
                    msg.append(sessionsInProgress.get(i));
                }
            }
            
            NarrowOptionPane.showMessageDialog(contentPanel,
                                               msg.toString(),
                                               title.toString(),
                                               JOptionPane.WARNING_MESSAGE);
        }

        return (numberOfTasksInProgress > 0);
    }

    private void createMenuBar() {
        JMenu menu = new JMenu("Menu");
        menu.setMnemonic(KeyEvent.VK_M);
        menuBar.add(menu);

        List<ProjectConfiguration> projectList = tmogConfig.getProjectList();
        addSessionItems =
                new HashMap<JMenuItem, ProjectConfiguration>(projectList.size());
        for (ProjectConfiguration project : projectList) {
            JMenuItem addSessionItem = new JMenuItem(
                    "Add '" + project.getName() + "' Session");
            addSessionItem.addActionListener(this);
            menu.add(addSessionItem);
            addSessionItems.put(addSessionItem, project);
        }

        removeSessionItem = new JMenuItem("Remove Current Session",
                                          KeyEvent.VK_R);
        removeSessionItem.addActionListener(this);
        menu.add(removeSessionItem);

        menu.addSeparator();
        exitItem = new JMenuItem("Exit",
                                 KeyEvent.VK_E);
        exitItem.addActionListener(this);
        menu.add(exitItem);
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == removeSessionItem) {
            removeSession();
        } else if (source == exitItem) {
            exitApplicationSafely();
        } else {
            JMenuItem addItem = (JMenuItem) source;
            ProjectConfiguration pConfig = addSessionItems.get(addItem);
            if (pConfig != null) {
                addSession(pConfig);
            }
        }
    }

    private void addSession(ProjectConfiguration projectConfig) {
        File defaultDirectory = null;
        int currentTab = tabbedPane.getSelectedIndex();
        if (currentTab > -1) {
            String currentTitle = tabbedPane.getTitleAt(currentTab);
            SessionView currentView = sessionList.get(currentTitle);
            defaultDirectory = currentView.getDefaultDirectory();
        }
        sessionCount++;
        String newTitle = "Session " + sessionCount;
        SessionView newView = buildViewForProject(projectConfig,
                                                  defaultDirectory);
        sessionList.put(newTitle, newView);
        tabbedPane.addTab(newTitle,
                          newView.getSessionIcon(),
                          newView.getPanel());
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

    private SessionView buildViewForProject(ProjectConfiguration projectConfig,
                                            File defaultDirectory) {
        SessionView newView;
        if (CollectorView.TASK_NAME.equals(projectConfig.getTaskName())) {
            newView = new CollectorView(projectConfig,
                                        defaultDirectory,
                                        tabbedPane);
        } else {
            // default to rename task and view
            newView = new RenameView(projectConfig,
                            defaultDirectory,
                            tabbedPane);
        }
        return newView;
    }
}

