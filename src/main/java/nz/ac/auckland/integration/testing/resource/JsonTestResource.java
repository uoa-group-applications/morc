package nz.ac.auckland.integration.testing.resource;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Provides a mechanism for retrieving JSON values from a file/URL/String and also
 * validating the response from a target service.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class JsonTestResource extends TestResource<String> {

    public JsonTestResource(String value) {
        super(value);
    }

    public JsonTestResource(File file) {
        super(file);
    }

    public JsonTestResource(URL file) {
        super(file);
    }

    /**
     * @param input A JSON string
     * @return true if the Json trees match (uses the Jackson ObjectMapper to unmarshal the string and compare using Java equality)
     */
    public boolean validateInput(String input) {
        if (input == null) return false;
        try {

            String expectedInput = getValue();

            if (input.isEmpty() || expectedInput.isEmpty()) return input.isEmpty() && expectedInput.isEmpty();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode expectedJson = mapper.readTree(getValue());
            JsonNode inputJson = mapper.readTree(input);
            return expectedJson.equals(inputJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param file a reference to the JSON test resource
     * @return A JSON string
     * @throws IOException
     */
    protected String getResource(File file) throws IOException {
        return FileUtils.readFileToString(file);
    }
}
