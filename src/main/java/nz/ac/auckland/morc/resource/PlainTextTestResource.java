package nz.ac.auckland.morc.resource;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.TypeConversionException;
import org.apache.cxf.helpers.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * A simple mechanism for sending and comparing plain text values
 * using the Java equals mechanism
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class PlainTextTestResource extends StaticTestResource<String> implements Predicate {

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
    public synchronized boolean matches(Exchange exchange) {
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

            logger.debug("Expected Plain Text Value: {},\nActual Plain Text Value: {}", expectedInput,
                    value);

            boolean match;

            if (value.isEmpty() || expectedInput.isEmpty()) match = value.isEmpty() && expectedInput.isEmpty();
            else match = value.equals(expectedInput);

            if (!match)
                logger.warn("Differences exist between the expected plain text value and the encountered value");

            return match;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        try {
            String value = "PlainTextTestResource:" + getValue();
            if (value.length() < 100) return value;
            else return value.substring(0, 97) + "...";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
