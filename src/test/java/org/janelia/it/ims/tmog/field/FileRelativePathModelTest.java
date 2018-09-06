package org.janelia.it.ims.tmog.field;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.ims.tmog.target.FileTarget;

import java.io.File;

/**
 * Tests the FileRelativePathModel class.
 *
 * @author Eric Trautman
 */
public class FileRelativePathModelTest extends TestCase {

    /**
     * Constructs a test case with the given name.
     *
     * @param  name  name of the test case.
     */
    public FileRelativePathModelTest(String name) {
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
        return new TestSuite(FileRelativePathModelTest.class);
    }

    /**
     * Tests the getFileNameValue method.
     *
     * @throws Exception
     *   if any unexpected errors occur.
     */
    public void testGetFileNameValue() throws Exception {
        String testData[][] = {
                {"/root", "/root/foo/bar/file.txt",  "foo/bar"},
                {"/root", "/root/foo/file.txt",      "foo"},
                {"/root", "/root/file.txt",          ""},
                {"/root", "/foo/file.txt",           ""},
                {"/root", "/root/foo\\bar/file.txt", "foo/bar"},
        };

        FileRelativePathModel model;
        for (String[] data : testData) {
            String rootPath = data[0];
            String fullPath = data[1];
            String expectedRelativePath = data[2];
            File rootPathFile = new File(rootPath);
            File file = new File(fullPath);
            model = new FileRelativePathModel();
            model.initializeValue(new FileTarget(file, rootPathFile));
            assertEquals("invalid relative path returned for file with path '" +
                         fullPath + "'",
                         expectedRelativePath, model.getFileNameValue());
        }
    }
}