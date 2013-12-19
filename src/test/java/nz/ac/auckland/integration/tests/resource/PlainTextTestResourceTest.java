package nz.ac.auckland.integration.tests.resource;

import nz.ac.auckland.integration.testing.resource.PlainTextTestResource;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class PlainTextTestResourceTest extends Assert {

    private static final String EXPECTED_VALUE = "test";

    URL inputUrl = this.getClass().getResource("/data/plaintext-test1.txt");
    URL inputUrl2 = this.getClass().getResource("/data/plaintext-test2.txt");

    @Test
    public void testReadFileFromClasspath() throws Exception {
        PlainTextTestResource resource = new PlainTextTestResource(inputUrl);
        String actualValue = resource.getValue();
        assertEquals(actualValue, EXPECTED_VALUE);
    }

    @Test
    public void testCompareInput() throws Exception {
        PlainTextTestResource validator = new PlainTextTestResource(inputUrl);
        assertTrue(validator.validate(EXPECTED_VALUE));
    }

    @Test
    public void testCompareDifferentInput() throws Exception {
        PlainTextTestResource validator = new PlainTextTestResource(inputUrl);
        assertFalse(validator.validate("sample"));
    }

    @Test
    public void testNullInput() throws Exception {
        PlainTextTestResource validator = new PlainTextTestResource(inputUrl);
        String nullStr = null;
        assertFalse(validator.validate(nullStr));
    }

    @Test
    public void testEmptyFile() throws Exception {
        PlainTextTestResource validator = new PlainTextTestResource(inputUrl2);
        assertTrue(validator.validate(""));
    }

    @Test
    public void testPassValueToConstructor() throws Exception {
        PlainTextTestResource validator = new PlainTextTestResource("foo");
        assertTrue(validator.validate("foo"));
    }

}
