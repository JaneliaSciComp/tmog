package org.janelia.it.utils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the PathUtil class.
 *
 * @author Eric Trautman
 */
public class PathUtilTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public PathUtilTest(String name) {
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
        return new TestSuite(PathUtilTest.class);
    }

    /**
     * Tests the convertPathToMac method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testConvertPathToMac() throws Exception {
        String[][] testData = {
                { "\\\\dm6.int.janelia.org\\rubinlab\\confocalStacks",
                  "/Volumes/rubinlab/confocalStacks"
                },
                { "\\home\\rubinlab\\confocalStacks",
                  "/home/rubinlab/confocalStacks"
                },
                { "\\home\\rubinlab/confocalStacks",
                  "/home/rubinlab/confocalStacks"
                },
                { "C:\\home\\rubinlab\\confocalStacks",
                  "/home/rubinlab/confocalStacks"
                },
                { "C:",
                  ""
                },
                { null,
                  null
                }
        };

        for (String[] data : testData) {
            String srcPath = data[0];
            String expectedResult = data[1];
            assertEquals("invalid conversion for '" + srcPath + "'",
                         expectedResult, PathUtil.convertPathToMac(srcPath));
        }
    }

    /**
     * Tests the convertPathToWindows method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testConvertPathToWindows() throws Exception {
        String[][] testData = {
                { "smb://dm6.int.janelia.org/rubinlab/confocalStacks",
                  "\\\\dm6.int.janelia.org\\rubinlab\\confocalStacks"
                },
                { "/home/rubinlab/confocalStacks",
                  "\\home\\rubinlab\\confocalStacks"
                },
                { "/home/rubinlab\\confocalStacks",
                  "\\home\\rubinlab\\confocalStacks"
                },
                { null,
                  null
                }
        };

        for (String[] data : testData) {
            String srcPath = data[0];
            String expectedResult = data[1];
            assertEquals("invalid conversion for '" + srcPath + "'",
                         expectedResult, PathUtil.convertPathToWindows(srcPath));
        }
    }

    /**
     * Tests the convertPathToUnix method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testConvertPathToUnix() throws Exception {
        String[][] testData = {
                { "\\\\dm6.int.janelia.org\\rubinlab\\confocalStacks",
                  "//dm6.int.janelia.org/rubinlab/confocalStacks"
                },
                { "\\home\\rubinlab\\confocalStacks",
                  "/home/rubinlab/confocalStacks"
                },
                { "\\home\\rubinlab/confocalStacks",
                  "/home/rubinlab/confocalStacks"
                },
                { "C:\\home\\rubinlab\\confocalStacks",
                  "/home/rubinlab/confocalStacks"
                },
                { "C:",
                  ""
                },
                { null,
                  null
                }
        };

        for (String[] data : testData) {
            String srcPath = data[0];
            String expectedResult = data[1];
            assertEquals("invalid conversion for '" + srcPath + "'",
                         expectedResult, PathUtil.convertPathToUnix(srcPath));
        }
    }

}
