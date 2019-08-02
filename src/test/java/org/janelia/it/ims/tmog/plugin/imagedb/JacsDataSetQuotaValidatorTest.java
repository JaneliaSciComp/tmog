/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.config.PluginConfiguration;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the {@link JacsDataSetQuotaValidator} class.
 *
 * @author Eric Trautman
 */
public class JacsDataSetQuotaValidatorTest
        extends TestCase {

    public JacsDataSetQuotaValidatorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(JacsDataSetQuotaValidatorTest.class);
    }

    public void testInit() throws Exception {

        final JacsDataSetQuotaValidator validator = new JacsDataSetQuotaValidator();
        final PluginConfiguration pluginConfig = new PluginConfiguration();
        // http://jade1.int.janelia.org:8880/jacsstorage/master_api/v1/storage/quota/nrsFilestore/report?${Data Set}
        // http://jacs-dev.int.janelia.org:8880/jacsstorage/master_api/v1/storage/quota/nrsFilestore/report?${Data Set}
        // http://jacs-webdav1:8880/JFS/api/quota/filestore/status/${Data Set}
        // http://jacs-dev.int.janelia.org:8880/jacsstorage/master_api/v1/storage/quota/nrsFilestore/report/${Data Set}
        pluginConfig.setProperty(JacsDataSetQuotaValidator.SERVICE_URL_PROPERTY,
                                 "https://api.int.janelia.org/SCSW/JADEServices/v1/storage/quota/nrsFilestore/report/${Data Set}");
        pluginConfig.setProperty(JacsDataSetQuotaValidator.TEST_DATA_SET_PROPERTY,
                                 "nerna_polarity_case_3");

        validator.init(pluginConfig);
    }


}