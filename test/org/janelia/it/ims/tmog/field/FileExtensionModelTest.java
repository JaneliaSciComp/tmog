package org.janelia.it.ims.tmog.field;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;

/**
 * Tests the FileExtensionModel class.
 *
 * @author Eric Trautman
 */
public class FileExtensionModelTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public FileExtensionModelTest(String name) {
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
        return new TestSuite(FileExtensionModelTest.class);
    }

    /**
     * Tests the getFileNameValue method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetFileNameValue() throws Exception {
        String testData[][] = {
                {"foo",                ""},
                {"foo.",               ""},
                {".",                  ""},
                {"foo.bar",            ".bar"},
                {"foo.bar.txt",        ".txt"},
                {"foo...html",         ".html"},
                {"/foo/bar/test.lsm",  ".lsm"},
        };

        FileExtensionModel model = new FileExtensionModel();
        for (String[] data : testData) {
            String name = data[0];
            String expectedExtension = data[1];
            File file = new File(name);
            model.initializeValue(new FileTarget(file));
            assertEquals("invalid extension returned for file with name '" +
                         name + "'",
                         expectedExtension, model.getFileNameValue());
        }
    }
}
