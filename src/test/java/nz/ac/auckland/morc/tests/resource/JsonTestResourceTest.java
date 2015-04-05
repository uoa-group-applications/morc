package nz.ac.auckland.morc.tests.resource;

import nz.ac.auckland.morc.resource.JsonTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
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
        assertFalse(validator.validate(""));
    }

    @Test
    public void testPassValueToConstructor() throws Exception {
        JsonTestResource validator = new JsonTestResource("{\"foo\":\"baz\"}");
        assertTrue(validator.validate("{\"foo\":\"baz\"}"));
    }

    @Test
    public void testCorrectToStringSize() throws Exception {
        assertEquals(100, new JsonTestResource(EXPECTED_VALUE).toString().length());
    }

    @Test
    public void testEmptyExchange() throws Exception {
        assertFalse(new JsonTestResource("{\"foo\":\"baz\"}").matches(new DefaultExchange(new DefaultCamelContext())));
    }

    @Test
    public void testNoMatch() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("{\"baz\":\"foo\"}");
        assertFalse(new JsonTestResource("{\"foo\":\"baz\"}").matches(e));
    }

    @Test
    public void testEmptyExpectation() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("{\"baz\":\"foo\"}");
        assertFalse(new JsonTestResource("").matches(e));
    }

    @Test
    public void testEmptyStringExchange() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("");
        assertFalse(new JsonTestResource("{\"foo\":\"baz\"}").matches(e));
    }

    @Test
    public void testInvalidJson() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());

        boolean exception = false;
        try {
            new JsonTestResource("{\"foo\":\"baz\" ").process(e);
        } catch (RuntimeException ex) {
            exception = true;
        }

        assertTrue("Exception not found", exception);
    }

    @Test
    public void testContentType() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        new JsonTestResource("{\"foo\":\"baz\" }").process(e);
        assertEquals("application/json", e.getIn().getHeader(Exchange.CONTENT_TYPE));
    }
}
