package org.janelia.it.ims.tmog.config.output;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.config.ConfigurationException;
import org.janelia.it.ims.tmog.field.DataField;

import java.io.File;
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

       // absolute path for missing directory
        OutputDirectoryConfiguration config =
                new OutputDirectoryConfiguration();
        File baseDirectory = new File("/");
        String absolutePathPrefix = baseDirectory.getAbsolutePath();
        String baseDirectoryName = absolutePathPrefix +
                                   "missingTestDirectory";
        Path path = new Path(baseDirectoryName);
        config.addComponent(path);
        ArrayList<DataField> fieldList = new ArrayList<DataField>();

        try {
            config.verify("testProject", fieldList);
            fail("missing absolute directory should have caused exception");
        } catch (ConfigurationException e) {
            assertEquals("absolute base directory does not match",
                         baseDirectoryName, config.getBasePath());
        }

        // relative path for missing directory
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
                    basePath.startsWith(absolutePathPrefix));
            assertTrue(
                    "invalid conversion of relative directory to " + basePath, 
                    basePath.endsWith(baseDirectoryName));
        }

        // no path
        config = new OutputDirectoryConfiguration();
        SourceFileModificationTime sfmt = new SourceFileModificationTime();
        config.addComponent(sfmt);
        config.verify("testProject", fieldList);
        basePath = config.getBasePath();
        assertTrue(
                "absolute directory not created for component list, " +
                "basePath is " + basePath,
                basePath.startsWith(absolutePathPrefix));

        // absolute path without trailing separator for existing directory
        File directory = new File("");
        String absolutePathName = directory.getAbsolutePath();
        path = new Path(absolutePathName);
        config = new OutputDirectoryConfiguration();
        config.addComponent(path);

        config.verify("testProject", fieldList);
        assertEquals("absolute path without trailing separator is incorrect",
                     absolutePathName, config.getBasePath());

        // absolute path without trailing spearator for existing directory
        String fileSeparator = System.getProperty("file.separator");
        absolutePathName = directory.getAbsolutePath() + fileSeparator;
        path = new Path(absolutePathName);
        config = new OutputDirectoryConfiguration();
        config.addComponent(path);

        config.verify("testProject", fieldList);
        assertEquals("absolute path with trailing separator is incorrect", 
                     absolutePathName, config.getBasePath());

        // relative path without trailing spearator for existing directory
        String relativePathName = "";
        directory = new File(relativePathName);
        absolutePathName = directory.getAbsolutePath();
        path = new Path(relativePathName);
        config = new OutputDirectoryConfiguration();
        config.addComponent(path);

        config.verify("testProject", fieldList);
        assertEquals("relative path without trailing separator is incorrect",
                     absolutePathName, config.getBasePath());

        // relative path without trailing spearator for existing directory
        relativePathName = "../";
        directory = new File(relativePathName);
        absolutePathName = directory.getAbsolutePath() + fileSeparator;
        path = new Path(relativePathName);
        config = new OutputDirectoryConfiguration();
        config.addComponent(path);

        config.verify("testProject", fieldList);
        assertEquals("absolute path with trailing separator is incorrect",
                     absolutePathName, config.getBasePath());

    }
}