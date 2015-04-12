package nz.ac.auckland.morc.tests.resource;

import nz.ac.auckland.morc.processor.MultiProcessor;
import nz.ac.auckland.morc.resource.HeadersTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeadersTestResourceTest extends Assert {

    private static final Map<String, Object> EXPECTED_PROPERTIES;

    static {
        EXPECTED_PROPERTIES = new HashMap<>();
        EXPECTED_PROPERTIES.put("foo", "baz");
        EXPECTED_PROPERTIES.put("abc", "123");
    }

    URL inputUrl = this.getClass().getResource("/data/header-test1.properties");
    URL inputUrl2 = this.getClass().getResource("/data/header-test2.properties");

    @Test
    public void testReadFileFromClassPath() throws Exception {
        HeadersTestResource resource = new HeadersTestResource(inputUrl);

        Map<String, Object> actualValues = resource.getValue();

        assertEquals(actualValues.size(), EXPECTED_PROPERTIES.size());

        for (String expectedProperty : EXPECTED_PROPERTIES.keySet()) {
            assertTrue(actualValues.containsKey(expectedProperty));
            assertEquals(actualValues.get(expectedProperty), EXPECTED_PROPERTIES.get(expectedProperty));
        }
    }

    @Test
    public void testCompareInput() throws Exception {
        assertTrue(new HeadersTestResource(inputUrl).matches(EXPECTED_PROPERTIES));
    }

    @Test
    public void testCompareDifferentInput() throws Exception {
        Map<String, Object> differentProperties = new HashMap<>();
        differentProperties.put("baz", "foo");
        differentProperties.put("123", "abc");

        assertFalse(new HeadersTestResource(inputUrl).matches(differentProperties));
    }

    @Test
    public void testCompareWrongInput() throws Exception {
        Map<String, Object> differentProperties = new HashMap<>();
        differentProperties.put("foo", "foo");
        differentProperties.put("abc", "123");

        assertFalse(new HeadersTestResource(inputUrl).matches(differentProperties));
    }

    @Test
    public void testCompareMoreInputHeaders() throws Exception {
        Map<String, Object> expectedHeaders = new HashMap<>();
        expectedHeaders.put("foo", "baz");
        expectedHeaders.put("abc", "123");
        expectedHeaders.put("moo", "cow");

        HeadersTestResource receivedHeaders = new HeadersTestResource(inputUrl);

        assertFalse(new HeadersTestResource(expectedHeaders).matches(receivedHeaders.getValue()));
    }

    @Test
    public void testNullProperties() throws Exception {
        Map<String, Object> nullMap = null;
        assertFalse(new HeadersTestResource(inputUrl).matches(nullMap));
    }

    @Test
    public void testEmptyPropertiesFile() throws Exception {
        assertTrue(new HeadersTestResource(inputUrl2).matches(new HashMap<>()));
    }

    @Test
    public void testPassValueToConstructor() throws Exception {
        HashMap<String, Object> values1 = new HashMap<>();
        values1.put("foo", "baz");
        values1.put("abc", "123");

        HashMap<String, Object> values2 = new HashMap<>();
        values2.put("foo", "baz");
        values2.put("abc", "123");

        assertTrue(new HeadersTestResource(values1).matches(values2));
    }

    @Test
    public void testFormatNullHeaders() throws Exception {
        assertTrue(HeadersTestResource.formatHeaders(null).equals(""));
    }

    @Test
    public void testFormatWithNullHeader() throws Exception {
        HashMap<String, Object> values = new HashMap<>();
        values.put("foo", "baz");
        values.put("abc", null);

        assertTrue(HeadersTestResource.formatHeaders(values).contains("null"));
    }

    @Test
    public void testSetMultipleHeaders() throws Exception {
        HashMap<String, Object> values = new HashMap<>();
        values.put("foo", "baz");
        values.put("abc", "123");

        HeadersTestResource headers = new HeadersTestResource(values);

        HashMap<String, Object> values1 = new HashMap<>();
        values.put("123", "456");

        HeadersTestResource headers1 = new HeadersTestResource(values1);

        List<Processor> processors = new ArrayList<>();
        processors.add(headers);
        processors.add(headers1);

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        new MultiProcessor(processors).process(e);

        assertEquals("baz", e.getIn().getHeader("foo"));
        assertEquals("123", e.getIn().getHeader("abc"));
        assertEquals("456", e.getIn().getHeader("123"));
    }

}
