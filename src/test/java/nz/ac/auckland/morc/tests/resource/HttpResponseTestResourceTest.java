package nz.ac.auckland.morc.tests.resource;

import nz.ac.auckland.morc.predicate.HeadersPredicate;
import nz.ac.auckland.morc.resource.HttpErrorTestResource;
import nz.ac.auckland.morc.resource.TestResource;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class HttpResponseTestResourceTest extends Assert {

    @Test
    public void testNullExchange() throws Exception {
        HttpErrorTestResource validator = new HttpErrorTestResource();
        assertFalse(validator.matches(null));
    }

    @Test
    public void testNullException() throws Exception {
        HttpErrorTestResource validator = new HttpErrorTestResource();
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        assertFalse(validator.matches(e));
    }
    /*
    @Test
    public void testSetResponseBodyHeadersStatusCode() throws Exception {
        Predicate responseBodyValidator = new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                return true;
            }
        };

        HeadersPredicate responseHeadersPredicate = new MockHeadersPredicate(null, true);

        HttpErrorTestResource validator = new HttpErrorTestResource(500,responseBodyValidator,responseHeadersPredicate);

        assertEquals(validator.getStatusCode(), 500);
        assertEquals(validator.getBodyPredicate(), responseBodyValidator);
        assertEquals(validator.getHeadersPredicate(), responseHeadersPredicate);
    }

    @Test
    public void testWrongException() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new IOException());
        assertFalse(new HttpErrorTestResource().matches(e));
    }

    @Test
    public void testDifferentStatusCode() throws Exception {
        Predicate responseBodyValidator = new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                return true;
            }
        };

        HttpErrorTestResource validator = new HttpErrorTestResource.Builder()
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

        HttpErrorTestResource validator = new HttpErrorTestResource.Builder()
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

        HttpErrorTestResource validator = new HttpErrorTestResource.Builder()
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

        HttpErrorTestResource validator = new HttpErrorTestResource.Builder()
                .responseBody(resource).build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new HttpOperationFailedException("uri", 123, "status", "location", null, "foo"));

        assertTrue(validator.matches(e));
    }

    @Test
    public void testJsonResource() throws Exception {
        JsonTestResource resource = new JsonTestResource("{\"foo\":\"baz\"}");

        HttpErrorTestResource validator = new HttpErrorTestResource.Builder()
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

        HttpErrorTestResource validator = new HttpErrorTestResource.Builder()
                .responseBody(responseBodyValidator)
                .responseHeaders(resource)
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new HttpOperationFailedException("uri", 123, "status", "location", map, "foo"));
        assertFalse(validator.matches(e));
    }

    @Test
    public void testIncorrectBody() throws Exception {
        Predicate responseBodyValidator = new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                return exchange.getIn().getBody(String.class).equals("foo");
            }
        };

        HttpErrorTestResource validator = new HttpErrorTestResource.Builder()
                .responseBody(responseBodyValidator)
                .responseHeaders(new MockHeadersPredicate(null, false))
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new HttpOperationFailedException("uri", 123, "status", "location", null, "foo"));
        assertFalse(validator.matches(e));
    }

    @Test
    public void testNoBodyNoHeadersToString() throws Exception {
        assertTrue(new HttpErrorTestResource().toString().contains("HttpErrorTestResource:"));
    }
    */
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
