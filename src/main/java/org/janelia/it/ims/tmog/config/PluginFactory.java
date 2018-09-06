/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config;

import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.Plugin;
import org.janelia.it.ims.tmog.plugin.RowListener;
import org.janelia.it.ims.tmog.plugin.RowUpdater;
import org.janelia.it.ims.tmog.plugin.RowValidator;
import org.janelia.it.ims.tmog.plugin.SessionListener;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class constructs configured plug-in listener instances.
 *
 * @author Eric Trautman
 */
public class PluginFactory {

    private ConfigurationLoader loader;

    private List<PluginConfiguration> rowUpdaterPlugins;
    private List<RowUpdater> rowUpdaters;

    private List<PluginConfiguration> rowListenerPlugins;
    private List<RowListener> rowListeners;

    private List<PluginConfiguration> rowValidatorPlugins;
    private List<RowValidator> rowValidators;

    private List<PluginConfiguration> sessionListenerPlugins;
    private List<SessionListener> sessionListeners;

    public PluginFactory() {
        rowUpdaterPlugins = new ArrayList<PluginConfiguration>();
        rowUpdaters = new ArrayList<RowUpdater>();
        rowListenerPlugins = new ArrayList<PluginConfiguration>();
        rowListeners = new ArrayList<RowListener>();
        rowValidatorPlugins = new ArrayList<PluginConfiguration>();
        rowValidators = new ArrayList<RowValidator>();
        sessionListenerPlugins = new ArrayList<PluginConfiguration>();
        sessionListeners = new ArrayList<SessionListener>();
    }

    public void setLoader(ConfigurationLoader loader) {
        this.loader = loader;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void addRowUpdaterPlugin(PluginConfiguration plugin) {
        rowUpdaterPlugins.add(plugin);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void addRowListenerPlugin(PluginConfiguration plugin) {
        rowListenerPlugins.add(plugin);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void addRowValidatorPlugin(PluginConfiguration plugin) {
        rowValidatorPlugins.add(plugin);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void addSessionListenerPlugin(PluginConfiguration plugin) {
        sessionListenerPlugins.add(plugin);
    }

    public List<RowUpdater> getRowUpdaters() {
        return rowUpdaters;
    }

    public List<RowListener> getRowListeners() {
        return rowListeners;
    }

    public List<RowValidator> getRowValidators() {
        return rowValidators;
    }

    public List<SessionListener> getSessionListeners() {
        return sessionListeners;
    }

    public void constructInstances(String projectName)
            throws ConfigurationException {

        // construct instances based upon configured classes
        List<Object> constructedInstances = new ArrayList<Object>();
        constructedInstances.addAll(
                constructInstancesForClass(projectName,
                                           rowUpdaterPlugins,
                                           RowUpdater.class));
        constructedInstances.addAll(
                constructInstancesForClass(projectName,
                                           rowListenerPlugins,
                                           RowListener.class));
        constructedInstances.addAll(
                constructInstancesForClass(projectName,
                                           rowValidatorPlugins,
                                           RowValidator.class));
        constructedInstances.addAll(
                constructInstancesForClass(projectName,
                                           sessionListenerPlugins,
                                           SessionListener.class));

        // add constructed instances to appropriate list(s)
        for (Object instance : constructedInstances) {
            if (instance instanceof RowUpdater) {
                rowUpdaters.add((RowUpdater) instance);
            }
            if (instance instanceof RowListener) {
                rowListeners.add((RowListener) instance);
            }
            if (instance instanceof RowValidator) {
                rowValidators.add((RowValidator) instance);
            }
            if (instance instanceof SessionListener) {
                sessionListeners.add((SessionListener) instance);
            }
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
                    loader.publishMessage("  initializing " + className.substring(className.lastIndexOf('.')+1));
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

    public static Object constructInstance(String className,
                                           String projectName)
            throws ConfigurationException {

        // see http://java.sun.com/javase/6/docs/technotes/guides/reflection/enhancements.html
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(
                    "The configured plugin class (" + className +
                    ") for the " + projectName +
                    " project cannot be found.", e);
        }

        final Class<?>[] args = new Class[0];
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
