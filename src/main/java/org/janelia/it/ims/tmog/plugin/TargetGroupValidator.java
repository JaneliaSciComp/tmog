/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.Target;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This plug-in uses a regular expression pattern to derive a group name
 * for each target (file) being processed.  It then compares the name
 * of each target in a group against a list of required patterns to
 * determine if the group is incomplete (missing targets with a particular
 * pattern).  Finally as each row is validated, the plug-in checks the row's
 * derived group name against the list of incomplete groups and flags the
 * row as invalid if its group is incomplete.
 *
 * @author Eric Trautman
 */
public class TargetGroupValidator
        implements RowValidator {

    /** Name of the property that identifies the group name pattern. */
    public static final String GROUP_NAME_PATTERN_PROPERTY =
            "groupNamePattern";

    /** Prefix for all required member pattern property names. */
    public static final String MEMBER_PATTERN_PROPERTY_PREFIX =
            "requiredMemberPattern";

    /**
     * Name of the property that identifies full file path should be used
     * for pattern matching.
     */
    public static final String USE_FULL_PATH_PROPERTY =
            "useFullPath";

    /**
     * Name of the property that identifies a name or path
     * for testing the group pattern.
     */
    public static final String TEST_PROPERTY = "testName";

    /** The pattern used to derive a target's group name. */
    private Pattern groupNamePattern;

    /** The set of required patterns found in each complete group. */
    private Set<Pattern> requiredPatterns;

    /** Indicates if the full file path should be used for pattern matching. */
    private boolean useFullPath;

    /** Cache of incomplete groups for each session. */
    private Map<String, Map<String, List<Pattern>>> sessionToIncompleteGroupMap;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public TargetGroupValidator() {
        this.sessionToIncompleteGroupMap =
                new ConcurrentHashMap<String, Map<String, List<Pattern>>>();
    }

    /**
     * Verifies that the plugin is properly configured and ready for use.
     *
     * @param  config  the plugin configuration.
     *
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    @Override
    public void init(PluginConfiguration config) throws ExternalSystemException {

        final PluginPropertyHelper helper =
                new PluginPropertyHelper(config,
                                         INIT_FAILURE_MSG);

        final String patternString =
                helper.getRequiredProperty(GROUP_NAME_PATTERN_PROPERTY);
        try {
            this.groupNamePattern = Pattern.compile(patternString);
        } catch (Exception e) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "The " + GROUP_NAME_PATTERN_PROPERTY + " value '" +
                    patternString + "' could not be parsed.  " + e.getMessage(),
                    e);
        }

        this.requiredPatterns =
                new TreeSet<Pattern>(new Comparator<Pattern>() {
                    @Override
                    public int compare(Pattern o1,
                                       Pattern o2) {
                        return o1.pattern().compareTo(o2.pattern());
                    }
                });

        String propertyValue;
        Pattern requiredPattern;
        final Map<String, String> props = config.getProperties();
        for (String propertyName : props.keySet()) {
            if (propertyName.startsWith(MEMBER_PATTERN_PROPERTY_PREFIX)) {
                propertyValue = props.get(propertyName);
                try {
                    requiredPattern = Pattern.compile(propertyValue);
                    this.requiredPatterns.add(requiredPattern);
                } catch (Exception e) {
                    throw new ExternalSystemException(
                            INIT_FAILURE_MSG +
                            "The " + propertyName + " value '" + propertyValue +
                            "' could not be parsed.  " + e.getMessage(),
                            e);
                }
            }
        }

        if (this.requiredPatterns.size() == 0) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "At least one " +
                    MEMBER_PATTERN_PROPERTY_PREFIX +
                    " value must be defined.");
        }

        this.useFullPath = Boolean.parseBoolean(
                helper.getRequiredProperty(USE_FULL_PATH_PROPERTY));

        final String testName = helper.getRequiredProperty(TEST_PROPERTY);
        Matcher m = this.groupNamePattern.matcher(testName);
        if (! m.matches()) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "The " + TEST_PROPERTY + " value '" + testName +
                    "' does not match the " + GROUP_NAME_PATTERN_PROPERTY +
                    " '" + patternString + "'.");
        }

        if (m.groupCount() == 0) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "The " +
                    GROUP_NAME_PATTERN_PROPERTY + " '" + patternString +
                    "' must contain parentheses to identify " +
                    "the group portion of each target name.");
        }

        boolean testNameMatchedRequiredPattern = false;
        for (Pattern pattern : this.requiredPatterns) {
            m = pattern.matcher(testName);
            if (m.matches()) {
                testNameMatchedRequiredPattern = true;
                break;
            }
        }

        if (! testNameMatchedRequiredPattern) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "The " + TEST_PROPERTY + " value '" +
                    testName + "' does not match any of the " +
                    MEMBER_PATTERN_PROPERTY_PREFIX + " values.");
        }

    }

    /**
     * Loops through the data rows to determine which file groups are
     * incomplete (missing files with specific suffixes).
     * Results are saved for the session and then referenced later
     * during specific row validation (see {@link #validate}) so
     * that errors can be displayed in context.
     *
     * @param  sessionName  unique name for session being validated.
     * @param  allRows      unmodifiable list of all rows for the session
     *                      about to be validated.
     *
     * @throws ExternalSystemException
     *   if any error occurs while setting up for validation.
     */
    public void startSessionValidation(String sessionName,
                                       List<DataRow> allRows)
            throws ExternalSystemException {

        Map<String, Set<Pattern>> groupToPatternMap =
                new LinkedHashMap<String, Set<Pattern>>();

        String targetName;
        String groupName;
        Matcher m;
        Set<Pattern> patternSet;
        for (DataRow row : allRows) {
            targetName = getTargetName(row.getTarget());
            groupName = getGroupName(targetName);
            if (groupName != null) {
                patternSet = groupToPatternMap.get(groupName);
                if (patternSet == null) {
                    patternSet = new HashSet<Pattern>();
                    groupToPatternMap.put(groupName, patternSet);
                }
                for (Pattern pattern : requiredPatterns) {
                    m = pattern.matcher(targetName);
                    if (m.matches()) {
                        patternSet.add(pattern);
                        break;
                    }
                }
            }
        }

        Map<String, List<Pattern>> groupToMissingMap =
                new HashMap<String, List<Pattern>>();

        List<Pattern> missingPatterns;
        for (String group : groupToPatternMap.keySet()) {
            patternSet = groupToPatternMap.get(group);
            missingPatterns = null;
            for (Pattern pattern : requiredPatterns) {
                if (! patternSet.contains(pattern)) {
                    if (missingPatterns == null) {
                        missingPatterns = new ArrayList<Pattern>();
                    }
                    missingPatterns.add(pattern);
                }
            }
            if ((missingPatterns != null) && (missingPatterns.size() > 0)) {
                groupToMissingMap.put(group, missingPatterns);
            }
        }

        if (groupToMissingMap.size() > 0) {
            addSessionData(sessionName, groupToMissingMap);
        } else {
            removeSessionData(sessionName);
        }

    }

    /**
     * Validates the set of information collected for a specific row.
     *
     * @param  sessionName  unique name for session being validated.
     * @param  row          the user supplied information to be validated.
     *
     * @throws ExternalDataException
     *   if the data is not valid.
     *
     * @throws ExternalSystemException
     *   if any error occurs while validating the data.
     */
    public void validate(String sessionName,
                         PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        final Map<String, List<Pattern>> groupToMissingMap =
                sessionToIncompleteGroupMap.get(sessionName);

        if (groupToMissingMap != null) {
            final String targetName =
                    getTargetName(row.getDataRow().getTarget());
            final String groupName = getGroupName(targetName);

            if (groupName == null) {
                throw new ExternalDataException(
                        "Unable to derive group name for '" + targetName +
                        "' using pattern: " +
                        groupNamePattern.toString());
            }

            List<Pattern> missingPatterns = groupToMissingMap.get(groupName);
            if ((missingPatterns != null) && (missingPatterns.size() > 0)) {
                StringBuilder msg = new StringBuilder(128);
                msg.append("The '");
                msg.append(groupName);
                msg.append("' group of files does not contain any files ");
                msg.append("that match the following pattern");
                if (missingPatterns.size() == 1) {
                    msg.append(": ");
                    msg.append(missingPatterns.get(0).toString());
                } else {
                    msg.append("s:\n");
                    for (Pattern pattern : missingPatterns) {
                        msg.append("  ");
                        msg.append(pattern.toString());
                        msg.append('\n');
                    }
                }
                throw new ExternalDataException(msg.toString());
            }
        }
    }

    /**
     * Removes any incomplete file groups saved for the specified session.
     *
     * @param  sessionName  unique name for session being validated.
     */
    public void stopSessionValidation(String sessionName) {
        removeSessionData(sessionName);
    }

    private String getTargetName(Target target) {
        String targetName;
        if (useFullPath && (target instanceof FileTarget)) {
            File file = ((FileTarget) target).getFile();
            targetName = file.getAbsolutePath();
        } else {
            targetName = target.getName();
        }
        return targetName;
    }

    private String getGroupName(String targetName) {
        String groupName = null;
        Matcher m = groupNamePattern.matcher(targetName);
        if (m.matches() && (m.groupCount() == 1)) {
            groupName = m.group(1);
        }
        return groupName;
    }

    private synchronized void addSessionData(String sessionName,
                                             Map<String, List<Pattern>> groupToMissingMap) {
        sessionToIncompleteGroupMap.put(sessionName, groupToMissingMap);
    }

    private synchronized void removeSessionData(String sessionName) {
        sessionToIncompleteGroupMap.remove(sessionName);
    }

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize the Target Group Validator Plugin.  ";
}