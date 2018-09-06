/*
 * Copyright (c) 2017 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view;

import org.janelia.it.ims.tmog.config.ProjectConfiguration;
import org.janelia.it.ims.tmog.config.TransmogrifierConfiguration;
import org.janelia.it.ims.tmog.config.preferences.TransmogrifierPreferences;
import org.janelia.it.ims.tmog.view.component.NarrowOptionPane;

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

    private ColorScheme colorScheme;

    private JTabbedPane tabbedPane;
    private JMenuBar menuBar;
    private JPanel contentPanel;
    private Map<JMenuItem, ProjectConfiguration> addSessionItems;
    private JMenuItem removeSessionItem;
    private JMenuItem exitItem;
    private JMenuItem colorSchemeItem;
    private JMenuItem resizeToWindowItem;
    private JMenuItem resizeToDataItem;
    private JMenuItem resizeToPreferencesItem;
    private JMenuItem savePreferencesItem;
    private JMenuItem deletePreferencesItem;

    private HashMap<String, SessionView> sessionList;
    private int sessionCount;

    public TabbedView(ColorScheme colorScheme,
                      TransmogrifierConfiguration tmogConfig) {

        this.colorScheme = colorScheme;

        this.sessionList = new HashMap<>();
        this.sessionCount = 0;

        createMenuBar(tmogConfig);
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

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == removeSessionItem) {
            removeSession();
        } else if (source == exitItem) {
            exitApplicationSafely();
        } else if (source == colorSchemeItem) {
            colorScheme.toggle();
        } else if (source == resizeToWindowItem) {
            resize(SessionView.ResizeType.WINDOW);
        } else if (source == resizeToDataItem) {
            resize(SessionView.ResizeType.DATA);
        } else if (source == resizeToPreferencesItem) {
            resize(SessionView.ResizeType.PREFERENCES);
        } else if (source == savePreferencesItem) {
            updatePreferences(false);
        } else if (source == deletePreferencesItem) {
            updatePreferences(true);
        } else {
            JMenuItem addItem = (JMenuItem) source;
            ProjectConfiguration pConfig = addSessionItems.get(addItem);
            if (pConfig != null) {
                addSession(pConfig);
            }
        }
    }

    private void exitApplicationSafely() {
        if (! hasActiveSessions(sessionList.keySet())) {
            System.exit(0);
        }        
    }

    private boolean hasActiveSessions(Collection<String> sessionNames) {
        ArrayList<String> sessionsInProgress = new ArrayList<>();
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

    private void createMenuBar(TransmogrifierConfiguration tmogConfig) {
        createSessionMenu(tmogConfig);
        createViewMenu();
        createHelpMenu();
    }

    private void createSessionMenu(TransmogrifierConfiguration tmogConfig) {
        JMenu sessionMenu = new JMenu("Session");
        sessionMenu.setMnemonic(KeyEvent.VK_S);
        menuBar.add(sessionMenu);

        List<ProjectConfiguration> projectList = tmogConfig.getProjectList();
        addSessionItems =
                new HashMap<>(projectList.size());
        JMenuItem addSessionItem;
        for (ProjectConfiguration project : projectList) {
            //noinspection NullableProblems
            addSessionItem = createAndAddMenuItem(
                    "Add '" + project.getName() + "' Session",
                    null,
                    true,
                    sessionMenu);
            addSessionItems.put(addSessionItem, project);
        }

        removeSessionItem = createAndAddMenuItem("Remove Current Session",
                                                 KeyEvent.VK_R,
                                                 false,
                                                 sessionMenu);

        sessionMenu.addSeparator();

        exitItem = createAndAddMenuItem("Exit",
                                        KeyEvent.VK_E,
                                        true,
                                        sessionMenu);
    }

    private void createViewMenu() {
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);

        colorSchemeItem = createAndAddMenuItem("Toggle Color Scheme",
                                               KeyEvent.VK_C,
                                               true,
                                               viewMenu);
        viewMenu.addSeparator();

        resizeToDataItem = createAndAddMenuItem("Resize to Fit Data",
                                                KeyEvent.VK_F,
                                                false,
                                                viewMenu);
        resizeToWindowItem = createAndAddMenuItem("Resize to Fit Window",
                                                  KeyEvent.VK_W,
                                                  false,
                                                  viewMenu);
        resizeToPreferencesItem = createAndAddMenuItem("Resize to Preferences",
                                                       KeyEvent.VK_P,
                                                       false,
                                                       viewMenu);
        viewMenu.addSeparator();

        savePreferencesItem = createAndAddMenuItem("Save Current as Preference",
                                                   KeyEvent.VK_S,
                                                   false,
                                                   viewMenu);
        deletePreferencesItem = createAndAddMenuItem("Delete Preference",
                                                     KeyEvent.VK_D,
                                                     false,
                                                     viewMenu);

        menuBar.add(viewMenu);
    }

    private void createHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(helpMenu);

        JMenu shortcutsMenu = new JMenu("Keyboard Shortcuts");
        helpMenu.add(shortcutsMenu);

        shortcutsMenu.add(new JMenuItem(
                "Ctrl-D: fill down value from current cell to all cells below it"));
        shortcutsMenu.add(new JMenuItem(
                "Ctrl-R: copy all values from previous row to current row"));
        shortcutsMenu.add(new JMenuItem(
                "Ctrl-V: paste from clipboard (including on Mac OS)"));

        JMenu exclusionMenu = new JMenu("Excluding Multiple Items");
        helpMenu.add(exclusionMenu);

        exclusionMenu.add(new JMenuItem(
                "click on item names, then click on exclude button for one of the items"));

    }

    private JMenuItem createAndAddMenuItem(String text,
                                           Integer mnemonic,
                                           boolean enabled,
                                           JMenu menu) {
        JMenuItem item;

        if (mnemonic == null) {
            item = new JMenuItem(text);
        } else {
            item = new JMenuItem(text, mnemonic);
        }

        item.addActionListener(this);
        item.setEnabled(enabled);
        menu.add(item);

        return item;
    }

    private SessionView getCurrentView() {
        SessionView currentView = null;
        int currentTab = tabbedPane.getSelectedIndex();
        if (currentTab > -1) {
            String currentTitle = tabbedPane.getTitleAt(currentTab);
            currentView = sessionList.get(currentTitle);
        }
        return currentView;
    }

    private void resize(SessionView.ResizeType resizeType) {
        SessionView currentView = getCurrentView();
        if (currentView != null) {
            currentView.resizeDataTable(resizeType);
        }
    }

    private void updatePreferences(boolean clear) {

        SessionView currentView = getCurrentView();

        if (currentView != null) {

            String action;
            if (clear) {
                action = " deleted.";
            } else {
                action = " saved.";
            }

            TransmogrifierPreferences preferences =
                    TransmogrifierPreferences.getInstance();
            if (preferences.canWrite()) {

                if (clear) {
                    currentView.clearPreferencesForCurrentProject();
                } else {
                    currentView.setPreferencesForCurrentProject();
                }

                final boolean wasSaveSuccessful = preferences.save();
                if (wasSaveSuccessful) {
                    NarrowOptionPane.showMessageDialog(
                            contentPanel,
                            "Preferences were successfully" + action,
                            "Preferences Updated",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    NarrowOptionPane.showMessageDialog(
                            contentPanel,
                            "Preferences were NOT"  + action +
                            "  Please verify that you have access to the " +
                            "preferences file " + preferences.getAbsolutePath(),
                            "Preferences Not Updated",
                            JOptionPane.ERROR_MESSAGE);
                }

            } else {

                NarrowOptionPane.showMessageDialog(
                        contentPanel,
                        "Preferences were NOT"  + action +
                        "You do not have access to overwrite the " +
                        "preferences file " + preferences.getAbsolutePath(),
                        "Preferences Not Updated",
                        JOptionPane.ERROR_MESSAGE);

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
        final String newTitle = "Session " + sessionCount;
        SessionView newView = buildViewForProject(newTitle,
                                                  projectConfig,
                                                  defaultDirectory);
        sessionList.put(newTitle, newView);
        tabbedPane.addTab(newTitle,
                          newView.getSessionIcon(),
                          newView.getPanel());
        int newSelectedIndex = tabbedPane.getTabCount() - 1;
        tabbedPane.setSelectedIndex(newSelectedIndex);

        if (sessionList.size() == 1) {
            removeSessionItem.setEnabled(true);
            resizeToWindowItem.setEnabled(true);
            resizeToDataItem.setEnabled(true);
            resizeToPreferencesItem.setEnabled(true);

            TransmogrifierPreferences preferences =
                    TransmogrifierPreferences.getInstance();
            if (preferences.canWrite()) {
                savePreferencesItem.setEnabled(true);
                deletePreferencesItem.setEnabled(true);
            }

        }
    }

    private void removeSession() {
        int currentTab = tabbedPane.getSelectedIndex();
        if (currentTab > -1) {
            String sessionTitle = tabbedPane.getTitleAt(currentTab);
            ArrayList<String> sessionNames = new ArrayList<>();
            sessionNames.add(sessionTitle);
            if (! hasActiveSessions(sessionNames)) {
                sessionList.remove(sessionTitle);
                tabbedPane.removeTabAt(currentTab);
            }

            if (sessionList.size() == 0) {
                removeSessionItem.setEnabled(false);
                resizeToWindowItem.setEnabled(false);
                resizeToDataItem.setEnabled(false);
                resizeToPreferencesItem.setEnabled(false);
                savePreferencesItem.setEnabled(false);
                deletePreferencesItem.setEnabled(false);
            }
        }
    }

    private SessionView buildViewForProject(String sessionName,
                                            ProjectConfiguration projectConfig,
                                            File defaultDirectory) {
        SessionView newView;
        if (CollectorView.TASK_NAME.equals(projectConfig.getTaskName())) {
            newView = new CollectorView(sessionName,
                                        projectConfig,
                                        defaultDirectory,
                                        false,
                                        tabbedPane);
        } else if (CollectorView.SAGE_TASK_NAME.equals(projectConfig.getTaskName())) {
            newView = new CollectorView(sessionName,
                                        projectConfig,
                                        null,
                                        true,
                                        tabbedPane);
        } else {
            // default to rename task and view
            newView = new RenameView(sessionName,
                                     projectConfig,
                                     defaultDirectory,
                                     tabbedPane);
        }
        return newView;
    }
}

