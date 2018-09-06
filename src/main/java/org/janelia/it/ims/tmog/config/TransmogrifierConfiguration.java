/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config;

import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates all application configuration information.
 *
 * @author Eric Trautman
 */
public class TransmogrifierConfiguration {

    private GlobalConfiguration globalConfiguration;
    private List<ProjectConfiguration> projectList;

    public TransmogrifierConfiguration() {
        this.projectList = new ArrayList<ProjectConfiguration>();
    }

    public GlobalConfiguration getGlobalConfiguration() {
        return globalConfiguration;
    }

    public void setGlobalConfiguration(GlobalConfiguration globalConfiguration) {
        this.globalConfiguration = globalConfiguration;
    }

    public void addProjectConfiguration(ProjectConfiguration projectConfig) {
        this.projectList.add(projectConfig);
    }

    public List<ProjectConfiguration> getProjectList() {
        return projectList;
    }

    public ProjectConfiguration getDefaultProjectConfiguration() {
        ProjectConfiguration defaultProject = null;
        for (ProjectConfiguration project : projectList) {
            if (project.isDefault()) {
                defaultProject = project;
                break;
            }
        }
        return defaultProject;
    }
}
