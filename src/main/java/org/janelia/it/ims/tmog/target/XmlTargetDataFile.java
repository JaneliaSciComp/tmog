/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.target;

import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.ConfigurationException;
import org.janelia.it.utils.StringUtil;
import org.janelia.it.utils.digester.ElementNameCallParamRule;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link TargetDataFile} implementation for an XML file that adheres to
 * the following structure:
 *
 * <pre>
 * {@code
 *
 *   <root>
 *       <group>
 *           <group-property-a>value-a</group-property-a>
 *           <group-property-b>value-b</group-property-b>
 *           <target>
 *               <target-name>name-1</target-name>
 *               <target-property-c>value-c1</target-property-c>
 *           </target>
 *           <target>
 *               <target-name>name-2</target-name>
 *               <target-property-c>value-c2</target-property-c>
 *           </target>
 *       </group>
 *       <group>
 *          ...
 *       </group>
 *   </root>
 *
 * }
 * </pre>
 *
 * The specific element names are configured via path attributes,
 * but the basic structure needs to match that listed above.
 *
 * @author Eric Trautman
 */
public class XmlTargetDataFile
        implements TargetDataFile {

    // see attribute descriptions in setter methods below ...

    private String targetGroupPath;
    private String relativeTargetPath;
    private String relativeTargetNamePath;
    private Set<String> relativeGroupPropertyPaths;
    private Set<String> relativeTargetPropertyPaths;

    private Digester digester;

    public XmlTargetDataFile() {
        this.relativeGroupPropertyPaths = new LinkedHashSet<String>();
        this.relativeTargetPropertyPaths = new LinkedHashSet<String>();
    }

    /**
     * Set the xml path for the parent element of all common and specific
     * properties for a group of targets.
     *
     * @param  targetGroupPath  xml path for the target group element.
     */
    public void setTargetGroupPath(String targetGroupPath) {
        if (StringUtil.isDefined(targetGroupPath)) {
            this.targetGroupPath = targetGroupPath;
        }
    }

    /**
     * Set the relative path for the parent element of a
     * specific target and all of its properties.
     *
     * @param  relativePath  path (relative to the targetGroupPath or
     *                       root level if no targetGroupPath is defined)
     *                       for the target element.
     */
    public void setRelativeTargetPath(String relativePath) {
        if (StringUtil.isDefined(relativePath)) {
            this.relativeTargetPath = relativePath;
        }
    }

    /**
     * Set the relative path for the parent element of a
     * specific target and all of its properties.
     *
     * @param  relativePath  path (relative to the targetGroupPath or
     *                       root level if no targetGroupPath is defined)
     *                       for the target element.
     */
    public void setRelativeTargetNamePath(String relativePath) {
        if (StringUtil.isDefined(relativePath)) {
            this.relativeTargetNamePath = relativePath;
        }
    }

    /**
     * Adds a relative group property path.
     *
     * @param  relativePath  path (relative to the targetGroupPath)
     *                       for the group property element.
     */
    public void addRelativeGroupPropertyPath(String relativePath) {
        if (StringUtil.isDefined(relativePath)) {
            relativeGroupPropertyPaths.add(relativePath);
        }
    }

    /**
     * Adds a relative target property path.
     *
     * @param  relativePath  path (relative to the relativeTargetPath)
     *                       for the target property element.
     */
    public void addRelativeTargetPropertyPath(String relativePath) {
        if (StringUtil.isDefined(relativePath)) {
            relativeTargetPropertyPaths.add(relativePath);
        }
    }

    /**
     * Validates the configured data file parameters.
     *
     * @throws ConfigurationException
     *   if any of the settings are invalid.
     */
    @Override
    public void verify()
            throws ConfigurationException {

        if (relativeTargetPath == null) {
            throw new ConfigurationException(
                    "relativeTargetPath must be defined for xmlPropertyFile");
        }

        if (relativeTargetNamePath == null) {
            throw new ConfigurationException(
                    "relativeTargetNamePath must be defined for xmlPropertyFile");
        }

        if (targetGroupPath == null) {
            if (relativeGroupPropertyPaths.size() > 0) {
                throw new ConfigurationException(
                        "targetGroupPath must be defined for xmlPropertyFile " +
                        "since groupProperty elements are defined");
            }
        }
    }

    /**
     * @param  stream  data stream from which to parse target information.
     *
     * @return list of targets parsed from the specified stream.
     *
     * @throws IllegalArgumentException
     *   if any errors occur while processing the stream.
     */
    @Override
    public TargetList getTargets(InputStream stream)
            throws IllegalArgumentException {

        TargetList targetList = new TargetList();

        try {
            List<TargetPropertiesGroup> groupList =
                    new ArrayList<TargetPropertiesGroup>();

            setDigesterIfNecessary();

            digester.clear();
            digester.push(groupList);
            if (targetGroupPath == null) {
                digester.push(new TargetPropertiesGroup());
            }
            digester.parse(stream);

            File targetFile;
            for (TargetPropertiesGroup group : groupList) {
                for (TargetProperties properties : group.getList()) {
                    targetFile = new File(properties.getTargetName());
                    if (targetFile.exists()) {
                        targetList.addTarget(
                                new FileTarget(targetFile, properties));
                    } else {
                        final String message = "skipping non-existent file: " +
                                               targetFile.getAbsolutePath();
                        targetList.appendToSummary(message + "\n");
                        LOG.warn(message);
                    }
                }
            }
            
        } catch (Throwable t) {
            throw new IllegalArgumentException(
                    "Failed to parse selected XML file.", t);
        }

        return targetList;
    }

    private synchronized void setDigesterIfNecessary() {

        if (digester == null) {

            digester = new Digester();
            digester.setValidating(false);

            String path;
            String targetPath;

            if (targetGroupPath == null) {

                targetPath = relativeTargetPath;

            } else {

                targetPath = targetGroupPath + "/" + relativeTargetPath;

                // parse group shared parameters if group path defined ...

                digester.addObjectCreate(targetGroupPath,
                                         TargetPropertiesGroup.class);
                digester.addSetNext(targetGroupPath, "add");

                for (String relativePath : relativeGroupPropertyPaths) {
                    path = targetGroupPath + "/" + relativePath;
                    // xml = "<sample><gender>male</gender></sample>
                    // path ="/sample/gender"
                    // call addSharedProperty("gender", "male")
                    digester.addCallMethod(path, "addSharedProperty", 2);
                    digester.addRule(path, new ElementNameCallParamRule(0));
                    digester.addCallParam(path, 1);
                }
            }

            digester.addObjectCreate(targetPath,
                                     TargetProperties.class);
            digester.addSetNext(targetPath, "addTargetProperties");

            path = targetPath + "/" + relativeTargetNamePath;
            digester.addCallMethod(path, "setTargetName", 1);
            digester.addCallParam(path, 0);

            for (String relativePath : relativeTargetPropertyPaths) {
                path = targetPath + "/" + relativePath;
                digester.addCallMethod(path, "addSpecificProperty", 2);
                digester.addRule(path, new ElementNameCallParamRule(0));
                digester.addCallParam(path, 1);
            }

        }
    }

    private static final Logger LOG =
            Logger.getLogger(XmlTargetDataFile.class);
}