/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.plugin.imagedb.CompositeSetter.Token;

import java.util.List;

/**
 * Tests the CompositeSetter class.
 *
 * @author Eric Trautman
 */
public class CompositeSetterTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public CompositeSetterTest(String name) {
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
        return new TestSuite(CompositeSetterTest.class);
    }

    /**
     * Tests the parseTokens method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testParseTokens() throws Exception {
        CompositeSetter setter = new CompositeSetter("propertyType", "test");
        Object[][] testData = {
                // value, literal tokens, ref tokens, ref list
                { "${foo}",                   0, 1, "foo"},
                { "${foo}${bar}",             0, 2, "foo|bar"},
                { "${foo}${bar with spaces}", 0, 2, "foo|bar with spaces"},
                { "a",                        1, 0, ""},
                { "a${foo}b",                 2, 1, "foo"},
                { "a${foo}b with spaces",     2, 1, "foo"},
                { "a${foo}b${bar}c",          3, 2, "foo|bar"},
        };

        String value;
        int expectedLiterals;
        int expectedReferences;
        String expectedReferenceList;
        int actualLiterals;
        int actualReferences;
        StringBuilder actualReferenceList;
        List<Token> tokenList;
        for (Object[] testCaseData : testData) {
            value = (String) testCaseData[0];
            expectedLiterals = (Integer) testCaseData[1];
            expectedReferences = (Integer) testCaseData[2];
            expectedReferenceList = (String) testCaseData[3];
            tokenList = setter.parseTokens(value);
            actualLiterals = 0;
            actualReferences = 0;
            actualReferenceList = new StringBuilder();
            for (Token token : tokenList) {
                if (token.isLiteral()) {
                    actualLiterals++;
                } else {
                    actualReferences++;
                    if (actualReferenceList.length() > 0) {
                        actualReferenceList.append('|');
                    }
                    actualReferenceList.append(token.getValue());
                }
            }

            assertEquals("incorrect number of literals parsed for '" +
                         value + "'",
                         expectedLiterals, actualLiterals);

            assertEquals("incorrect number of references parsed for '" +
                         value + "'",
                         expectedReferences, actualReferences);

            assertEquals("incorrect references parsed for '" +
                         value + "'",
                         expectedReferenceList, actualReferenceList.toString());
        }

        String[] invalidValues = {
                "${",
                "a${foo",
                "${}",
                "a${}b",
                "${a${nested}b}"
        };
        for (String invalidValue : invalidValues) {
            try {
                setter.parseTokens(invalidValue);
                fail("invalid value '" + invalidValue +
                     "' did not cause exception");
            } catch (IllegalArgumentException e) {
                assertTrue(true); // test passed
            }
        }
    }

}