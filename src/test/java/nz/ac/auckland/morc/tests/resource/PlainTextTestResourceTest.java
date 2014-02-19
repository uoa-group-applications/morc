package nz.ac.auckland.morc.tests.resource;

import nz.ac.auckland.morc.resource.PlainTextTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
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

    @Test
    public void testEmptyExchange() throws Exception {
        assertFalse(new PlainTextTestResource("foo").matches(new DefaultExchange(new DefaultCamelContext())));
    }

    @Test
    public void testEmptyExpectationBlankExchange() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("");
        assertTrue(new PlainTextTestResource("").matches(e));
    }

    @Test
    public void testEmptyExpectationCompleteExchange() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("foo");
        assertFalse(new PlainTextTestResource("").matches(e));
    }

    @Test
    public void testEmptyStringExchangeCompleteBody() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("");
        assertFalse(new PlainTextTestResource("foo").matches(e));
    }

    @Test
    public void testLongString() throws Exception {
        assertEquals(100, new PlainTextTestResource("testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest").toString().length());
    }

}
