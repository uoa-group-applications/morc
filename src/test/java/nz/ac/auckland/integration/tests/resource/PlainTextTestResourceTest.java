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
        PlainTextTestResource resource = new PlainTextTestResource(inputUrl);
        assertTrue(resource.validate(EXPECTED_VALUE));
    }

    @Test
    public void testCompareDifferentInput() throws Exception {
        PlainTextTestResource resource = new PlainTextTestResource(inputUrl);
        assertFalse(resource.validate("sample"));
    }

    @Test
    public void testNullInput() throws Exception {
        PlainTextTestResource resource = new PlainTextTestResource(inputUrl);
        assertFalse(resource.validate(null));
    }

    @Test
    public void testEmptyFile() throws Exception {
        PlainTextTestResource resource = new PlainTextTestResource(inputUrl2);
        assertTrue(resource.validate(""));
    }

    @Test
    public void testPassValueToConstructor() throws Exception {
        PlainTextTestResource resource = new PlainTextTestResource("foo");
        assertTrue(resource.validate("foo"));
    }

}
