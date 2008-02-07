package org.janelia.it.ims.imagerenamer.config.output;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.imagerenamer.config.ConfigurationException;
import org.janelia.it.ims.imagerenamer.field.RenameField;

import java.util.ArrayList;

/**
 * Tests the OutputDirectoryConfiguration class.
 *
 * @author Eric Trautman
 */
public class OutputDirectoryConfigurationTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public OutputDirectoryConfigurationTest(String name) {
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
        return new TestSuite(OutputDirectoryConfigurationTest.class);
    }

    /**
     * Tests the verify method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testVerify() throws Exception {
        // absolute path
        // relative path
        // no path

        OutputDirectoryConfiguration config =
                new OutputDirectoryConfiguration();
        String fileNameSeparator = System.getProperty("file.separator");
        String baseDirectoryName = fileNameSeparator +
                                   "missingTestDirectory";
        Path path = new Path(baseDirectoryName);
        config.addComponent(path);
        ArrayList<RenameField> fieldList = new ArrayList<RenameField>();

        try {
            config.verify("testProject", fieldList);
            fail("missing absolute directory should have caused exception");
        } catch (ConfigurationException e) {
            assertEquals("absolute base directory does not match", 
                         baseDirectoryName, config.getBasePath());
        }

        config = new OutputDirectoryConfiguration();
        baseDirectoryName = "missingTestDirectory";
        path = new Path(baseDirectoryName);
        config.addComponent(path);
        String basePath;

        try {
            config.verify("testProject", fieldList);
            fail("missing relative directory should have caused exception");
        } catch (ConfigurationException e) {
            basePath = config.getBasePath();
            assertTrue(
                    "relative directory was not made absolute, basePath is " +
                    basePath,
                    basePath.startsWith(fileNameSeparator));
            assertTrue(
                    "invalid conversion of relative directory to " + basePath, 
                    basePath.endsWith(baseDirectoryName));
        }

        config = new OutputDirectoryConfiguration();
        SourceFileModificationTime sfmt = new SourceFileModificationTime();
        config.addComponent(sfmt);
        config.verify("testProject", fieldList);
        basePath = config.getBasePath();
        assertTrue(
                "absolute directory not created for component list, " +
                "basePath is " + basePath,
                basePath.startsWith(fileNameSeparator));

    }
}