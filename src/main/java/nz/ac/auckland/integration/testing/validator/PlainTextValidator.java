package nz.ac.auckland.integration.testing.validator;

import nz.ac.auckland.integration.testing.resource.PlainTextTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple mechanism for sending and comparing plain text values
 * using the Java equals mechanism
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class PlainTextValidator implements Validator {

    private Logger logger = LoggerFactory.getLogger(PlainTextValidator.class);
    PlainTextTestResource resource;

    /**
     * @param resource The plain text string we wish to validate against
     */
    public PlainTextValidator(PlainTextTestResource resource) {
        this.resource = resource;
    }

    /**
     * @param exchange The exchange containing the text string to validate against
     * @return true if the input String is the same as the test resource using Java String equality
     */
    public boolean validate(Exchange exchange) {
        String value;
        try {
            value = exchange.getIn().getBody(String.class);
        } catch (TypeConversionException e) {
            logger.warn("Error attempting to convert JSON to a String",e);
            return false;
        }
        return value != null && validate(value);
    }

    public boolean validate(String value) {
        try {
            String expectedInput = resource.getValue();
            if (value.isEmpty() || expectedInput.isEmpty()) return value.isEmpty() && expectedInput.isEmpty();
            return value.equals(expectedInput);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
