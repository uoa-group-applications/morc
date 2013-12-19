package nz.ac.auckland.integration.tests.resource;

import nz.ac.auckland.integration.testing.resource.JsonTestResource;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class JsonTestResourceTest extends Assert {

    private static final String EXPECTED_VALUE = "{\n" +
            "    \"firstName\":\"foo\",\n" +
            "    \"lastName\":\"baz\",\n" +
            "    \"address\": {\n" +
            "        \"street\":\"foostreet\",\n" +
            "        \"city\":\"baz\"\n" +
            "    },\n" +
            "    \"phone\": [\n" +
            "        {\n" +
            "            \"type\":\"home\",\n" +
            "            \"number\":1234\n" +
            "        },\n" +
            "        {\n" +
            "            \"type\":\"work\",\n" +
            "            \"number\":4321\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    URL inputUrl = this.getClass().getResource("/data/json-test1.json");
    URL inputUrl2 = this.getClass().getResource("/data/json-test2.json");

    @Test
    public void testReadFileFromClasspath() throws Exception {
        JsonTestResource resource = new JsonTestResource(inputUrl);

        String actualValue = resource.getValue();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode expectedJson = mapper.readTree(EXPECTED_VALUE);
        JsonNode inputJson = mapper.readTree(actualValue);

        assertTrue(expectedJson.equals(inputJson));
    }

    @Test
    public void testCompareInput() throws Exception {
        JsonTestResource validator = new JsonTestResource(inputUrl);
        assertTrue(validator.validate(EXPECTED_VALUE));
    }

    @Test
    public void testCompareDifferentInput() throws Exception {
        JsonTestResource validator = new JsonTestResource(inputUrl);
        assertFalse(validator.validate("{\"name\":\"foo\"}"));
    }

    @Test
    public void testNullInput() throws Exception {
        JsonTestResource validator = new JsonTestResource(inputUrl);
        String nullStr = null;
        assertFalse(validator.validate(nullStr));
    }

    @Test
    public void testEmptyFile() throws Exception {
        JsonTestResource validator = new JsonTestResource(inputUrl2);
        assertTrue(validator.validate(""));
    }

    @Test
    public void testPassValueToConstructor() throws Exception {
        JsonTestResource validator = new JsonTestResource("{\"foo\":\"baz\"}");
        assertTrue(validator.validate("{\"foo\":\"baz\"}"));
    }
}
