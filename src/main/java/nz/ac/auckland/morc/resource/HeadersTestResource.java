package nz.ac.auckland.morc.resource;

import java.io.File;
import java.io.InputStream;
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
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class HeadersTestResource extends StaticTestResource<Map<String, Object>> {

    public HeadersTestResource(Map<String, Object> values) {
        super(values);
    }

    public HeadersTestResource(File file) {
        super(file);
    }

    public HeadersTestResource(URL url) {
        super(url);
    }

    public HeadersTestResource(InputStream stream) {
        super(stream);
    }

    /**
     * @param stream an input stream we can read the file from (this will close it for you)
     */
    protected Map<String, Object> getResource(InputStream stream) throws Exception {
        Properties properties = new Properties();
        properties.load(stream);

        Map<String, Object> headers = new HashMap<>();

        for (String key : properties.stringPropertyNames()) {
            headers.put(key, properties.get(key));
        }

        return Collections.unmodifiableMap(headers);
    }

    /**
     * A convenience method for returning a human consumable string of a map of headers
     */
    public static String formatHeaders(Map<String, Object> headers) {
        if (headers == null) return "";

        StringBuilder sb = new StringBuilder();

        for (String key : headers.keySet()) {
            Object value = headers.get(key);
            sb.append(String.format("(%s:%s) ", key, value == null ? "null" : value.toString().trim()));
        }

        return sb.toString().trim();
    }

}
