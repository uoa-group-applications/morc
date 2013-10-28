package nz.ac.auckland.integration.testing.validator;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.StaticTestResource;
import org.apache.camel.Exchange;
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
 * Provides a mechanism for validating headers for an expectation or response
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class HeadersValidator implements Validator {

    private Logger logger = LoggerFactory.getLogger(XmlValidator.class);

    private HeadersTestResource resource;

    public HeadersValidator(HeadersTestResource resource) {
        this.resource = resource;
    }

    /**
     * @param exchange The exchange containing the headers we need to validate
     * @return true if each header in the test resource is in input; additional headers in the input will be ignored
     */
    public boolean validate(Exchange exchange) {
        Map<String, Object> value = exchange.getIn().getHeaders();
        if (value == null) return false;

        Map<String, Object> expectedHeaders;

        try {
            expectedHeaders = resource.getValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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
