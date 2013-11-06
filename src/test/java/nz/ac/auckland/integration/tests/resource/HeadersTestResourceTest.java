package nz.ac.auckland.integration.tests.resource;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.validator.HeadersValidator;
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
        HeadersValidator validator = new HeadersValidator(new HeadersTestResource(inputUrl));
        assertTrue(validator.validate(EXPECTED_PROPERTIES));
    }

    @Test
    public void testCompareDifferentInput() throws Exception {
        Map<String, Object> differentProperties = new HashMap<>();
        differentProperties.put("baz", "foo");
        differentProperties.put("123", "abc");

        HeadersValidator validator = new HeadersValidator(new HeadersTestResource(inputUrl));
        assertFalse(validator.validate(differentProperties));
    }

    @Test
    public void testCompareWrongInput() throws Exception {
        Map<String, Object> differentProperties = new HashMap<>();
        differentProperties.put("foo", "foo");
        differentProperties.put("abc", "123");

        HeadersValidator validator = new HeadersValidator(new HeadersTestResource(inputUrl));
        assertFalse(validator.validate(differentProperties));
    }

    @Test
    public void testCompareMoreInputHeaders() throws Exception {
        Map<String, Object> expectedHeaders = new HashMap<>();
        expectedHeaders.put("foo", "baz");
        expectedHeaders.put("abc", "123");
        expectedHeaders.put("moo", "cow");

        HeadersTestResource receivedHeaders = new HeadersTestResource(inputUrl);

        HeadersValidator validator = new HeadersValidator(new HeadersTestResource(expectedHeaders));
        assertFalse(validator.validate(receivedHeaders.getValue()));
    }

    @Test
    public void testNullProperties() throws Exception {
        HeadersValidator validator = new HeadersValidator(new HeadersTestResource(EXPECTED_PROPERTIES));
        Map<String, Object> nullMap = null;
        assertFalse(validator.validate(nullMap));
    }

    @Test
    public void testEmptyPropertiesFile() throws Exception {
        HeadersValidator validator = new HeadersValidator(new HeadersTestResource(inputUrl2));
        assertTrue(validator.validate(new HashMap<String, Object>()));
    }

    @Test
    public void testPassValueToConstructor() throws Exception {
        HashMap<String, Object> values1 = new HashMap<>();
        values1.put("foo", "baz");
        values1.put("abc", "123");

        HashMap<String, Object> values2 = new HashMap<>();
        values2.put("foo", "baz");
        values2.put("abc", "123");

        HeadersValidator validator = new HeadersValidator(new HeadersTestResource(values1));
        assertTrue(validator.validate(values2));
    }

}
