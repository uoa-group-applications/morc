package nz.ac.auckland.integration.testing.resource;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Used for retrieving resources necessary for testing and carrying out validation of
 * responses against the expected value
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public abstract class TestResource<T> {
    private File file;
    private T value;

    /**
     * @param value set the value directly for this test resource
     */
    public TestResource(T value) {
        this.value = value;
    }

    /**
     * @param file A reference to a file containing a resource of the specified type T
     */
    public TestResource(File file) {
        this.file = file;
    }

    /**
     * @param url A reference to a resource of the specified type T
     */
    public TestResource(URL url) {
        String fileName = url.getFile();
        if (fileName.isEmpty()) throw new RuntimeException("File Not Found: " + url.toString());

        this.file = new File(url.getFile());
    }

    /**
     * @param input A given message of type T used for comparison against the test resource itself
     * @return true if the input matches this test input
     */
    public abstract boolean validateInput(T input);

    /**
     * @param file a reference to the actual test resource
     * @return The test resource in the appropriate format
     * @throws IOException
     */
    protected abstract T getResource(File file) throws IOException;

    /**
     * @return The test resource in the appropriate format
     * @throws IOException
     */
    public T getValue() throws IOException {
        if (value != null) return value;
        else return getResource(file);
    }
}
