package nz.ac.auckland.integration.testing.predicate;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.TestResource;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Provides a mechanism for validating headers for an expectation or response
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class HeadersPredicate implements Predicate {

    private static final Logger logger = LoggerFactory.getLogger(HeadersPredicate.class);

    private TestResource<Map<String, Object>> resource;

    public HeadersPredicate(TestResource<Map<String, Object>> resource) {
        this.resource = resource;
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
            expectedHeaders = resource.getValue();
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
            headers = resource.getValue();
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
}
