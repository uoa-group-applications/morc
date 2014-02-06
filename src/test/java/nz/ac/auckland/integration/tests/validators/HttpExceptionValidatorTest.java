package nz.ac.auckland.integration.tests.validators;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.JsonTestResource;
import nz.ac.auckland.integration.testing.resource.PlainTextTestResource;
import nz.ac.auckland.integration.testing.resource.TestResource;
import nz.ac.auckland.integration.testing.predicate.HeadersPredicate;
import nz.ac.auckland.integration.testing.predicate.HttpErrorPredicate;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.component.http.HttpOperationFailedException;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpExceptionValidatorTest extends Assert {

    @Test
    public void testNullExchange() throws Exception {
        HttpErrorPredicate validator = new HttpErrorPredicate.Builder().build();
        assertFalse(validator.matches(null));
    }

    @Test
    public void testNullException() throws Exception {
        HttpErrorPredicate validator = new HttpErrorPredicate.Builder().build();
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        assertFalse(validator.matches(e));
    }

    @Test
    public void testSetResponseBodyHeadersStatusCode() throws Exception {
        Predicate responseBodyValidator = new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                return true;
            }
        };

        HeadersPredicate responseHeadersPredicate = new MockHeadersPredicate(null, true);

        HttpErrorPredicate validator = new HttpErrorPredicate.Builder()
                .responseBody(responseBodyValidator)
                .responseHeaders(responseHeadersPredicate)
                .statusCode(500)
                .build();

        assertEquals(validator.getStatusCode(), 500);
        assertEquals(validator.getBodyPredicate(), responseBodyValidator);
        assertEquals(validator.getHeadersPredicate(), responseHeadersPredicate);
    }

    @Test
    public void testWrongException() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new IOException());
        assertFalse(new HttpErrorPredicate().matches(e));
    }

    @Test
    public void testDifferentStatusCode() throws Exception {
        Predicate responseBodyValidator = new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                return true;
            }
        };

        HttpErrorPredicate validator = new HttpErrorPredicate.Builder()
                .responseBody(responseBodyValidator)
                .responseHeaders(new MockHeadersPredicate(null, true))
                .statusCode(500)
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new HttpOperationFailedException("uri", 123, "status", "location", null, null));
        assertFalse(validator.matches(e));
    }

    @Test
    public void testCorrectBody() throws Exception {
        Predicate responseBodyValidator = new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                return exchange.getIn().getBody(String.class).equals("foo");
            }
        };

        HttpErrorPredicate validator = new HttpErrorPredicate.Builder()
                .responseBody(responseBodyValidator)
                .responseHeaders(new MockHeadersPredicate(null, false))
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new HttpOperationFailedException("uri", 123, "status", "location", null, "foo"));
        assertFalse(validator.matches(e));
    }

    @Test
    public void testCorrectHeaders() throws Exception {
        Predicate responseBodyValidator = new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                return false;
            }
        };

        Predicate responseHeadersPredicate = new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                String foo = exchange.getIn().getHeader("foo", String.class);
                String baz = exchange.getIn().getHeader("baz", String.class);
                return foo.equals("baz") && baz.equals("foo");
            }
        };

        Map<String, String> map = new HashMap<>();
        map.put("foo", "baz");
        map.put("baz", "moo");

        HttpErrorPredicate validator = new HttpErrorPredicate.Builder()
                .responseBody(responseBodyValidator)
                .responseHeaders(new HeadersPredicate(new HeadersTestResource((Map) map)))
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new HttpOperationFailedException("uri", 123, "status", "location", map, "foo"));
        assertFalse(validator.matches(e));
    }

    @Test
    public void testPlainTextResource() throws Exception {
        PlainTextTestResource resource = new PlainTextTestResource("foo");

        HttpErrorPredicate validator = new HttpErrorPredicate.Builder()
                .responseBody(resource).build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new HttpOperationFailedException("uri", 123, "status", "location", null, "foo"));

        assertTrue(validator.matches(e));
    }

    @Test
    public void testJsonResource() throws Exception {
        JsonTestResource resource = new JsonTestResource("{\"foo\":\"baz\"}");

        HttpErrorPredicate validator = new HttpErrorPredicate.Builder()
                .responseBody(resource).build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new HttpOperationFailedException("uri", 123, "status", "location", null, "{\"foo\":\"baz\"}"));

        assertTrue(validator.matches(e));
    }

    @Test
    public void testHeadersResource() throws Exception {
        Predicate responseBodyValidator = new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                return false;
            }
        };

        Map<String, String> map = new HashMap<>();
        map.put("foo", "baz");
        map.put("baz", "moo");

        HeadersTestResource resource = new HeadersTestResource((Map) map);

        HttpErrorPredicate validator = new HttpErrorPredicate.Builder()
                .responseBody(responseBodyValidator)
                .responseHeaders(resource)
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new HttpOperationFailedException("uri", 123, "status", "location", map, "foo"));
        assertFalse(validator.matches(e));
    }
}

class MockHeadersPredicate extends HeadersPredicate {

    private boolean response = true;

    public MockHeadersPredicate(TestResource<Map<String, Object>> resource, boolean response) {
        super(resource);
        this.response = response;
    }

    @Override
    public boolean matches(Exchange exchange) {
        return response;
    }
}
