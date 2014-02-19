package nz.ac.auckland.morc.resource;

import java.io.*;
import java.net.URL;

/**
 * Used for retrieving static resources necessary for either sending values to an artifact under testing,
 * or validating an expectation
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public abstract class StaticTestResource<T> implements TestResource {
    private InputStream stream;
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
        try {
            this.stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param url A reference to a resource of the specified type T
     */
    public StaticTestResource(URL url) {
        try {
            this.stream = url.openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public StaticTestResource(InputStream inputStream) {
        this.stream = inputStream;
    }

    /**
     * @param stream an input stream we can read the file from (this will close it for you)
     * @return The test resource in the appropriate format
     * @throws Exception
     */
    protected abstract T getResource(InputStream stream) throws Exception;

    /**
     * @return The test resource in the appropriate format
     * @throws IOException
     */
    public T getValue() throws Exception {
        if (value != null) return value;

        value = getResource(stream);
        stream.close();
        return value;
    }
}
