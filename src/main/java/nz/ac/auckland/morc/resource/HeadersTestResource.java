package nz.ac.auckland.morc.resource;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(HeadersTestResource.class);

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

    /**
     * @param exchange The exchange containing the headers we need to validate
     * @return true if each header in the test resource is in input; additional headers in the input will be ignored
     */
    public boolean matches(Exchange exchange) {
        Map<String, Object> value = exchange.getIn().getHeaders();
        return matches(value);
    }

    public boolean matches(Map<String, Object> value) {
        if (value == null) return false;
        Map<String, Object> expectedHeaders;

        try {
            expectedHeaders = getValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        logger.debug("Expected Headers: {}, Actual Headers: {}", HeadersTestResource.formatHeaders(expectedHeaders),
                HeadersTestResource.formatHeaders(value));

        //I'm not interested if the input has any additional headers
        for (String expectedKey : expectedHeaders.keySet()) {
            if (!value.containsKey(expectedKey)) {
                logger.warn("The key: {} was not found", expectedKey);
                return false;
            }

            if (!value.get(expectedKey).equals(expectedHeaders.get(expectedKey))) {
                logger.warn("The key: {} has an unexpected value", expectedKey);
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        Map<String, Object> headers;
        try {
            headers = getValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (String key : headers.keySet()) {
            builder.append(key).append(":").append(headers.get(key)).append(",");
            if (++i == 3) break;
        }

        String output = builder.toString();
        if (output.endsWith(",")) output = output.substring(0, output.length() - 1);

        return "HeadersPredicate:{" + output + (headers.keySet().size() > 3 ? ",..." : "") + "}";
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Map<String, Object> headers = getValue();

        logger.trace("Setting headers of exchange from endpoint {} to {}",
                (exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown"),
                headers);

        Map<String, Object> currentHeaders = exchange.getIn().getHeaders();
        currentHeaders.putAll(headers);
    }
}
