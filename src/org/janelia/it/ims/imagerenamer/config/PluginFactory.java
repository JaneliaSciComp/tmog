/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.config;

import org.janelia.it.ims.imagerenamer.plugin.CopyListener;
import org.janelia.it.ims.imagerenamer.plugin.ExternalSystemException;
import org.janelia.it.ims.imagerenamer.plugin.Plugin;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRowValidator;
import org.janelia.it.ims.imagerenamer.plugin.SessionListener;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class constructs configured CopyListener instances.
 *
 * @author Eric Trautman
 */
public class PluginFactory {

    private List<PluginConfiguration> copyListenerPlugins;
    private List<CopyListener> copyListeners;

    private List<PluginConfiguration> rowValidatorPlugins;
    private List<RenameFieldRowValidator> rowValidators;

    private List<PluginConfiguration> sessionListenerPlugins;
    private List<SessionListener> sessionListeners;

    public PluginFactory() {
        copyListenerPlugins = new ArrayList<PluginConfiguration>();
        copyListeners = new ArrayList<CopyListener>();
        rowValidatorPlugins = new ArrayList<PluginConfiguration>();
        rowValidators = new ArrayList<RenameFieldRowValidator>();
        sessionListenerPlugins = new ArrayList<PluginConfiguration>();
        sessionListeners = new ArrayList<SessionListener>();
    }

    public void addCopyListenerPlugin(PluginConfiguration plugin) {
        copyListenerPlugins.add(plugin);
    }

    public void addRowValidatorPlugin(PluginConfiguration plugin) {
        rowValidatorPlugins.add(plugin);
    }

    public void addSessionListenerPlugin(PluginConfiguration plugin) {
        sessionListenerPlugins.add(plugin);
    }

    public List<CopyListener> getCopyListeners() {
        return copyListeners;
    }

    public List<RenameFieldRowValidator> getRowValidators() {
        return rowValidators;
    }

    public List<SessionListener> getSessionListeners() {
        return sessionListeners;
    }

    public void constructInstances(String projectName)
            throws ConfigurationException {

        List<Object> pluginInstances =
                constructInstancesForClass(projectName,
                                           copyListenerPlugins,
                                           CopyListener.class);
        for (Object instance : pluginInstances) {
            copyListeners.add((CopyListener) instance);
        }

        pluginInstances =
                constructInstancesForClass(projectName,
                                           rowValidatorPlugins,
                                           RenameFieldRowValidator.class);
        for (Object instance : pluginInstances) {
            rowValidators.add((RenameFieldRowValidator) instance);
        }

        pluginInstances =
                constructInstancesForClass(projectName,
                                           sessionListenerPlugins,
                                           SessionListener.class);
        for (Object instance : pluginInstances) {
            sessionListeners.add((SessionListener) instance);
        }
    }

    private List<Object> constructInstancesForClass(String projectName,
                                                    List<PluginConfiguration> pluginConfigurations,
                                                    Class basePluginClass)
            throws ConfigurationException {

        ArrayList<Object> pluginInstances = new ArrayList<Object>();
        for (PluginConfiguration pluginConfig : pluginConfigurations) {
            String className = pluginConfig.getClassName();
            Object newInstance = constructInstance(className, projectName);
            if (basePluginClass.isInstance(newInstance)) {
                Plugin plugin = (Plugin) newInstance;
                try {
                    plugin.init(pluginConfig);
                } catch (ExternalSystemException e) {
                    throw new ConfigurationException(e.getMessage(), e);
                }
                pluginInstances.add(plugin);
            } else {
                throw new ConfigurationException(
                        "The configured plugin class (" +
                        className + ") for the " + projectName +
                        " project does not implement " +
                        basePluginClass.getName() + ".");
            }
        }

        return pluginInstances;
    }

    private static Object constructInstance(String className,
                                            String projectName)
            throws ConfigurationException {

        Class clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(
                    "The configured plugin class (" + className +
                    ") for the " + projectName +
                    " project cannot be found.", e);
        }

        final Class[] args = new Class[0];
        Constructor constructor;
        try {
            constructor = clazz.getConstructor(args);
        } catch (NoSuchMethodException e) {
            throw new ConfigurationException(
                    "The configured plugin class (" + className +
                    ") for the " + projectName +
                    " project does not have an empty constructor.", e);
        }

        Object newInstance;
        try {
            newInstance = constructor.newInstance();
        } catch (InstantiationException e) {
            throw new ConfigurationException(
                    "configured plugin class (" + className +
                    ") for the " + projectName +
                    " project cannot be created.", e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException(
                    "configured plugin class (" + className +
                    ") for the " + projectName +
                    " project cannot be accessed.", e);
        } catch (InvocationTargetException e) {
            throw new ConfigurationException(
                    "configured plugin class (" + className +
                    ") for the " + projectName +
                    " project cannot be called.", e);
        }

        return newInstance;
    }
}
