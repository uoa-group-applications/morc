package nz.ac.auckland.integration.testing.resource;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * A simple mechanism for sending and comparing plain text values
 * using the Java equals mechanism
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class PlainTextTestResource extends TestResource<String> {

    public PlainTextTestResource(String value) {
        super(value);
    }

    public PlainTextTestResource(File file) {
        super(file);
    }

    public PlainTextTestResource(URL file) {
        super(file);
    }

    private Logger logger = LoggerFactory.getLogger(PlainTextTestResource.class);

    /**
     *
     * @param value A Java String
     * @return true if the input String is the same as the test resource using Java String equality
     */
    public boolean validate(String value) {

        if (value == null) return false;

        try {
            String expectedInput = getValue();
            if (value.isEmpty() || expectedInput.isEmpty()) return value.isEmpty() && expectedInput.isEmpty();
            return value.equals(expectedInput);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * @return The plain text from an external resource as a standard Java String
     * @throws IOException
     */
    public String getResource(File file) throws IOException {
        return FileUtils.readFileToString(file);
    }
}
