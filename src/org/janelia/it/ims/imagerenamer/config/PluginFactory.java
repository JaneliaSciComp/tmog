/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.config;

import org.janelia.it.ims.imagerenamer.plugin.CopyCompleteListener;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRowValidator;
import org.janelia.it.ims.imagerenamer.plugin.ExternalSystemException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

/**
 * This class constructs configured CopyCompleteListener instances.
 *
 * @author Eric Trautman
 */
public class PluginFactory {

    private Set<PluginConfiguration> copyCompleteListenerPlugins;
    private Set<CopyCompleteListener> copyCompleteListeners;

    private Set<PluginConfiguration> rowValidatorPlugins;
    private Set<RenameFieldRowValidator> rowValidators;

    public PluginFactory() {
        copyCompleteListenerPlugins = new HashSet<PluginConfiguration>();
        copyCompleteListeners = new HashSet<CopyCompleteListener>();
        rowValidatorPlugins = new HashSet<PluginConfiguration>();
        rowValidators = new HashSet<RenameFieldRowValidator>();
    }

    public void addCopyCompleteListenerPlugin(PluginConfiguration plugin) {
        copyCompleteListenerPlugins.add(plugin);
    }

    public void addRowValidatorPlugin(PluginConfiguration plugin) {
        rowValidatorPlugins.add(plugin);
    }

    public Set<CopyCompleteListener> getCopyCompleteListeners() {
        return copyCompleteListeners;
    }

    public Set<RenameFieldRowValidator> getRowValidators() {
        return rowValidators;
    }

    public void constructInstances() throws ConfigurationException {

        for (PluginConfiguration pluginConfig : copyCompleteListenerPlugins) {
            String className = pluginConfig.getClassName();
            Object newInstance = constructInstance(className);
            if (newInstance instanceof CopyCompleteListener) {
                CopyCompleteListener plugin =
                        (CopyCompleteListener) newInstance;
                try {
                    plugin.init();
                } catch (ExternalSystemException e) {
                    throw new ConfigurationException(e.getMessage(), e);
                }
                copyCompleteListeners.add(plugin);
            } else {
                throw new ConfigurationException(
                        "configured copy complete listener class (" +
                        className + ") does not implement " +
                        CopyCompleteListener.class.getName());
            }
        }

        for (PluginConfiguration pluginConfig : rowValidatorPlugins) {
            String className = pluginConfig.getClassName();
            Object newInstance = constructInstance(className);
            if (newInstance instanceof RenameFieldRowValidator) {
                RenameFieldRowValidator plugin =
                        (RenameFieldRowValidator) newInstance;
                try {
                    plugin.init();
                } catch (ExternalSystemException e) {
                    throw new ConfigurationException(e.getMessage(), e);
                }
                rowValidators.add(plugin);
            } else {
                throw new ConfigurationException(
                        "configured field row validator class (" +
                        className + ") does not implement " +
                        RenameFieldRowValidator.class.getName());
            }
        }

    }

    private static Object constructInstance(String className)
            throws ConfigurationException {

        Class clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(
                    "configured plugin class (" + className +
                    ") cannot be found", e);
        }

        final Class[] args = new Class[0];
        Constructor constructor;
        try {
            constructor = clazz.getConstructor(args);
        } catch (NoSuchMethodException e) {
            throw new ConfigurationException(
                    "configured plugin class (" + className +
                    ") does not have an empty constructor", e);
        }

        Object newInstance;
        try {
            newInstance = constructor.newInstance();
        } catch (InstantiationException e) {
            throw new ConfigurationException(
                    "configured plugin class (" + className +
                    ") cannot be created", e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException(
                    "configured plugin class (" + className +
                    ") cannot be accessed", e);
        } catch (InvocationTargetException e) {
            throw new ConfigurationException(
                    "configured plugin class (" + className +
                    ") cannot be called", e);
        }

        return newInstance;
    }
}
