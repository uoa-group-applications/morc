package nz.ac.auckland.integration.tests.resource;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.HashMap;
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
        HeadersTestResource resource = new HeadersTestResource(inputUrl);

        Map<String, Object> actualValues = resource.getValue();

        assertTrue(resource.validateInput(actualValues));
    }

    @Test
    public void testCompareDifferentInput() throws Exception {
        Map<String, Object> differentProperties = new HashMap<>();
        differentProperties.put("baz", "foo");
        differentProperties.put("123", "abc");

        HeadersTestResource resource = new HeadersTestResource(inputUrl);
        assertFalse(resource.validateInput(differentProperties));
    }

    @Test
    public void testCompareMoreInputHeaders() throws Exception {
        Map<String, Object> differentProperties = new HashMap<>();
        differentProperties.put("foo", "baz");
        differentProperties.put("abc", "123");
        differentProperties.put("moo", "cow");

        HeadersTestResource resource = new HeadersTestResource(inputUrl);
        assertTrue(resource.validateInput(differentProperties));
    }

    @Test
    public void testNullProperties() throws Exception {
        HeadersTestResource resource = new HeadersTestResource(inputUrl);
        assertFalse(resource.validateInput(null));
    }

    @Test
    public void testEmptyPropertiesFile() throws Exception {
        HeadersTestResource resource = new HeadersTestResource(inputUrl2);
        assertTrue(resource.validateInput(new HashMap<String, Object>()));
    }

    @Test
    public void testPassValueToConstructor() throws Exception {
        HashMap<String, Object> values1 = new HashMap<>();
        values1.put("foo", "baz");
        values1.put("abc", "123");

        HashMap<String, Object> values2 = new HashMap<>();
        values2.put("foo", "baz");
        values2.put("abc", "123");

        HeadersTestResource resource = new HeadersTestResource(values1);
        assertTrue(resource.validateInput(values2));
    }

}
