package nz.ac.auckland.integration.testing.validator;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.TestResource;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Provides a mechanism for validating headers for an expectation or response
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class HeadersValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(HeadersValidator.class);

    private TestResource<Map<String,Object>> resource;

    public HeadersValidator(TestResource<Map<String,Object>> resource) {
        this.resource = resource;
    }

    @SuppressWarnings("unchecked")
    public HeadersValidator(HeadersTestResource resource) {
        this.resource = resource;
    }

    /**
     * @param exchange The exchange containing the headers we need to validate
     * @return true if each header in the test resource is in input; additional headers in the input will be ignored
     */
    public boolean validate(Exchange exchange) {
        Map<String, Object> value = exchange.getIn().getHeaders();
        return value != null && validate(value);
    }

    public boolean validate(Map<String, Object> value) {
        if (value == null) return false;
        Map<String, Object> expectedHeaders;

        try {
            expectedHeaders = resource.getValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        logger.trace("Expected Headers: {}, Actual Headers: {}",HeadersTestResource.formatHeaders(expectedHeaders),
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
}
