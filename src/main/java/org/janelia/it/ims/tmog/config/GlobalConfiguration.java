/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.config;

/**
 * This class encapsulates configuration information that applies to
 * all projects.
 *
 * @author Eric Trautman
 */
public class GlobalConfiguration {

    private String minimumVersion;
    private Integer frameSizePercentage;

    public GlobalConfiguration() {
    }

    public String getMinimumVersion() {
        return minimumVersion;
    }

    public void setMinimumVersion(String minimumVersion) {
        this.minimumVersion = minimumVersion.trim();
    }

    public Integer getFrameSizePercentage() {
        return frameSizePercentage;
    }

    public void setFrameSizePercentage(Integer frameSizePercentage) {
        this.frameSizePercentage = frameSizePercentage;
    }

    /**
     * Verifies the global configuration.
     *
     * @param  currentVersion  the current running application version.
     *
     * @throws ConfigurationException if any errors occur.
     */
    public void verify(String currentVersion) throws ConfigurationException {
        boolean isVersionValid = true;
        if (minimumVersion != null)  {
           if (currentVersion != null) {
               int minimumVersionValue = getVersionValue(minimumVersion);
               int currentVersionValue = getVersionValue(currentVersion);
               if (currentVersionValue < minimumVersionValue) {
                   isVersionValid = false;
               }
           } else {
               isVersionValid = false;
           }
        }

        if (! isVersionValid) {
            throw new ConfigurationException(
                    "Version " + currentVersion + " is running, but this " +
                    "configuration requires version " + minimumVersion +
                    " or later.");
        }
    }

    private int getVersionValue(String version) throws ConfigurationException {
        int value = 0;
        String versionNumbers[] = version.split("\\.");

        final int maxNumberOfSegments = 4;
        if (versionNumbers.length > maxNumberOfSegments) {
            throw new ConfigurationException(
                "Version number '" + version +
                "' contains more than 4 segments and cannot be validated.");
        }

        final int divisor = 100;
        int factor = (int) Math.pow((double)divisor,
                                    (double)maxNumberOfSegments - 1);
        for (String numberStr : versionNumbers) {
            try {
                int number = Integer.parseInt(numberStr);
                if (number >= divisor) {
                    throw new ConfigurationException(
                        "Version number '" + version +
                        "' contains the segment '" + number +
                        "' which is too large and cannot be validated.  " +
                        "All version number segments should be less than " +
                        divisor + ".");
                }
                value = value + (number * factor);
                factor = factor / divisor;
            } catch (NumberFormatException e) {
                throw new ConfigurationException(
                    "Unable to interpret version number '" + version + "'.", e);
            }
        }
        return value;
    }
}