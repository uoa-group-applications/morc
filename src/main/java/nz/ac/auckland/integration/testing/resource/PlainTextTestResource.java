package nz.ac.auckland.integration.testing.resource;

import nz.ac.auckland.integration.testing.validator.Validator;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.apache.commons.io.FileUtils;
import org.apache.cxf.helpers.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A simple mechanism for sending and comparing plain text values
 * using the Java equals mechanism
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class PlainTextTestResource extends StaticTestResource<String> implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(PlainTextTestResource.class);

    public PlainTextTestResource(String value) {
        super(value);
    }

    public PlainTextTestResource(File file) {
        super(file);
    }

    public PlainTextTestResource(URL url) {
        super(url);
    }

    public PlainTextTestResource(InputStream stream) {
        super(stream);
    }

    /**
     * @return The plain text from an external resource as a standard Java String
     * @throws Exception
     */
    protected String getResource(InputStream stream) throws Exception {
        return IOUtils.toString(stream, "UTF-8");
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
            logger.warn("Error attempting to convert exchange to a String", e);
            return false;
        }
        return value != null && validate(value);
    }

    public boolean validate(String value) {
        if (value == null) return false;
        try {
            String expectedInput = getValue();

            logger.trace("Expected Plain Text Value: {},\nActual Plain Text Value: {}",expectedInput,
                    value);

            if (value.isEmpty() || expectedInput.isEmpty()) return value.isEmpty() && expectedInput.isEmpty();
            return value.equals(expectedInput);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
