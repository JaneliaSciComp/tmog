/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the CvTermModel class.
 *
 * @author Eric Trautman
 */
public class CvTermModelTest
        extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public CvTermModelTest(String name) {
        super(name);
    }

    /**
     * Static method to return a suite of all tests.
     * <p/>
     * The JUnit framework uses Java reflection to build a suite of all public
     * methods that have names like "testXXXX()".
     *
     * @return suite of all tests defined in this class.
     */
    public static Test suite() {
        return new TestSuite(CvTermModelTest.class);
    }

    /**
     * Tests the getFileNameValue method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testRetrieveValidValues() throws Exception {
        CvTermModel model = new CvTermModel();
        model.setServiceUrl(VECTOR_URL);

        model.retrieveAndSetValidValues();

        final int modelSize = model.getSize();
        assertTrue("empty model built for " + valueOf(model),
                   modelSize > 0);

        final List<ValidValue> xaList = model.getValidValues().stream()
                .filter(validValue -> "XA".equals(validValue.getValue()))
                .collect(Collectors.toList());

        assertEquals("incorrect size for list of XA values",
                     1, xaList.size());

        final ValidValue xaValidValue = xaList.get(0);
        assertEquals("incorrect XA value",
                     "XA", xaValidValue.getValue());
        assertEquals("incorrect XA display name",
                     "Activation domain with balancer", xaValidValue.getDisplayName());

        CvTermModel prefixedModel = new CvTermModel();
        prefixedModel.setServiceUrl(VECTOR_URL);
        prefixedModel.setDisplayNamePrefixedForValues(true);

        prefixedModel.retrieveAndSetValidValues();

        final int prefixedModelSize = prefixedModel.getSize();
        assertEquals("different number of terms returned for " +
                     valueOf(prefixedModel),
                     modelSize, prefixedModelSize);

        Map<ValidValue, ValidValue> map =
                new LinkedHashMap<ValidValue, ValidValue>();
        ValidValue value;
        ValidValue prefixedValue;
        String displayName;
        String prefixedDisplayName;
        for (int i = 0; i < modelSize; i++) {
            value = (ValidValue) model.getElementAt(i);
            map.put(value, value);
        }

        for (int i = 0; i < prefixedModelSize; i++) {
            prefixedValue = (ValidValue) prefixedModel.getElementAt(i);
            value = map.get(prefixedValue);
            assertNotNull("prefixed value '" + prefixedValue +
                          "' missing from " + map.keySet(),
                         value);
            displayName = value.getDisplayName();
            prefixedDisplayName = prefixedValue.getDisplayName();
            if (! value.getValue().equals(displayName)) {
                assertTrue("displayNames should differ but both are '" + 
                           displayName + "'",
                           (! prefixedDisplayName.equals(displayName)));
            }
        }
    }

    private String valueOf(CvTermModel model) {
        return model.getServiceUrl() + ", " + model.isDisplayNamePrefixedForValues();
    }

    private static final String BASE_SERVICE_URL =
            "http://sage.int.janelia.org/sage-ws/cvs/";
    private static final String VECTOR_URL =
            BASE_SERVICE_URL + "rubin_crm_vector";
}