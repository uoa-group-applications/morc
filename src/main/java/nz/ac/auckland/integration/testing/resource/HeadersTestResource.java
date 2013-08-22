package nz.ac.auckland.integration.testing.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provides a mechanism for setting key/value pairs for message headers
 * either as a mock response, or setting an expectation for the contents of a header.
 * Standard properties files are used if a File/URL is specified.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class HeadersTestResource extends TestResource<Map<String, Object>> {

    private Logger logger = LoggerFactory.getLogger(XmlTestResource.class);

    public HeadersTestResource(Map<String, Object> values) {
        super(values);
    }

    public HeadersTestResource(File file) {
        super(file);
    }

    public HeadersTestResource(URL file) {
        super(file);
    }

    /**
     * @param file a reference to a properties file
     * @return A Map containing header/value key pairs
     * @throws IOException
     */
    protected Map<String, Object> getResource(File file) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(file));

        Map<String, Object> headers = new HashMap<>();

        for (String key : properties.stringPropertyNames()) {
            headers.put(key, properties.get(key));
        }

        return Collections.unmodifiableMap(headers);
    }

    /**
     * @param input The headers we want to validate against the test resource
     * @return true if each header in the test resource is in input; additional headers in the input will be ignored
     */
    public boolean validateInput(Map<String, Object> input) {
        if (input == null) return false;

        Map<String, Object> expectedHeaders;

        try {
            expectedHeaders = getValue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //I'm not interested if the input has any additional headers
        for (String expectedKey : expectedHeaders.keySet()) {
            if (!input.containsKey(expectedKey)) {
                logger.warn("The key: {} was not found", expectedKey);
                return false;
            }

            if (!input.get(expectedKey).equals(expectedHeaders.get(expectedKey))) {
                logger.warn("The key: {} has an unexpected value", expectedKey);
                return false;
            }
        }

        return true;
    }

}
