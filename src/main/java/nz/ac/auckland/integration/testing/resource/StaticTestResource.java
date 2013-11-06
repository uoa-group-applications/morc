package nz.ac.auckland.integration.testing.resource;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Used for retrieving static resources necessary for either sending values to an artifact under testing,
 * or validating an expectation
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public abstract class StaticTestResource<T> implements TestResource {
    private File file;
    private T value;

    /**
     * @param value set the value directly for this test resource
     */
    public StaticTestResource(T value) {
        this.value = value;
    }

    /**
     * @param file A reference to a file containing a resource of the specified type T
     */
    public StaticTestResource(File file) {
        this.file = file;
    }

    /**
     * @param url A reference to a resource of the specified type T
     */
    public StaticTestResource(URL url) {
        String fileName = url.getFile();
        if (fileName.isEmpty()) throw new RuntimeException("File Not Found: " + url.toString());

        this.file = new File(url.getFile());
    }

    /**
     * @param file a reference to the actual test resource
     * @return The test resource in the appropriate format
     * @throws Exception
     */
    protected abstract T getResource(File file) throws Exception;

    /**
     * @return The test resource in the appropriate format
     * @throws IOException
     */
    public T getValue() throws Exception {
        if (value != null) return value;
        else return getResource(file);
    }
}
