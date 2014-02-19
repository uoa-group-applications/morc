package nz.ac.auckland.morc.resource;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.TypeConversionException;
import org.apache.cxf.helpers.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * Provides a mechanism for retrieving JSON values from a file/URL/String and also
 * validating the response from a target service.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class JsonTestResource extends StaticTestResource<String> implements Predicate {

    private static final Logger logger = LoggerFactory.getLogger(JsonTestResource.class);

    public JsonTestResource(String value) {
        super(value);
    }

    public JsonTestResource(File file) {
        super(file);
    }

    public JsonTestResource(URL url) {
        super(url);
    }

    public JsonTestResource(InputStream stream) {
        super(stream);
    }

    /**
     * @param stream an input stream we can read the file from (this will close it for you)
     */
    protected String getResource(InputStream stream) throws Exception {
        return IOUtils.toString(stream, "UTF-8");
    }

    /**
     * @param exchange The exchange containing the JSON string to validate
     * @return true if the Json trees match (uses the Jackson ObjectMapper to unmarshal the string and compare using Java equality)
     */
    public synchronized boolean matches(Exchange exchange) {
        String value;
        try {
            value = exchange.getIn().getBody(String.class);
        } catch (TypeConversionException e) {
            logger.warn("Error attempting to convert JSON to a String", e);
            return false;
        }
        return value != null && validate(value);
    }

    public boolean validate(String value) {
        if (value == null) return false;
        try {
            String expectedInput = getValue();

            logger.debug("Expected JSON Input: {},\nActual JSON Input: {}", expectedInput,
                    value);

            if (value.isEmpty() || expectedInput.isEmpty()) return value.isEmpty() && expectedInput.isEmpty();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode expectedJson = mapper.readTree(expectedInput);
            JsonNode inputJson = mapper.readTree(value);
            boolean equal = expectedJson.equals(inputJson);
            if (!equal) logger.warn("Differences exist between the expected JSON value and the encountered value");
            return equal;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        try {
            String value = "JsonTestResource:" + getValue();
            if (value.length() < 100) return value;
            else return value.substring(0, 97) + "...";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
