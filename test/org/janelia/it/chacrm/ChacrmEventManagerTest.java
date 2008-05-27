package org.janelia.it.chacrm;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.regex.Pattern;

/**
 * Tests the ChacrmEventManager class.
 *
 * @author Eric Trautman
 */
public class ChacrmEventManagerTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public ChacrmEventManagerTest(String name) {
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
        return new TestSuite(ChacrmEventManagerTest.class);
    }

    /**
     * Tests the getImageLocation method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetImageLocation() throws Exception {
        Pattern pattern =
                Pattern.compile("[/\\\\]++rubinlab[/\\\\]++confocalStacks[/\\\\]++");
        String[][] testData = {
                {
                        "/Volumes/rubinlab/confocalStacks/20070123/GMR_test.lsm",
                        "20070123/GMR_test.lsm"
                },
                {
                        "/Volumes/rubinlab//confocalStacks/20070123/GMR_test.lsm",
                        "20070123/GMR_test.lsm"
                },
                {
                        "/Volumes/rubinlab/confocalStacks/rubinlab/confocalStacks/20070123/GMR_test.lsm",
                        "rubinlab/confocalStacks/20070123/GMR_test.lsm"
                },
                {
                        "\\\\smb.int.janelia.org\\rubinlab\\confocalStacks\\20070123\\GMR_test.lsm",
                        "20070123\\GMR_test.lsm"
                },
                {
                        "/Volumes/rubinlab/confocalStacks",
                        null
                },
                {
                        "\\\\smb.int.janelia.org\\rubinlab\\confocalStack\\20070123\\GMR_test.lsm",
                        null
                },
                {
                        "/smb.int.janelia.org/rubinlab/confocalStack/20070123/GMR_test.lsm",
                        null
                }
        };

        for (String[] data : testData) {
            String fileName = data[0];
            String expectedRelativePath = data[1];

            ImageLocation imageLocaton =
                    ChacrmEventManager.getImageLocation(fileName, pattern);
            String actualRelativePath = null;
            if (imageLocaton != null) {
                actualRelativePath = imageLocaton.getRelativePath();
            }

            assertEquals("invalid relative path returned for file name: " +
                         fileName,
                         expectedRelativePath, actualRelativePath);
        }
    }
}
