package nz.ac.auckland.integration.tests.validators;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.JsonTestResource;
import nz.ac.auckland.integration.testing.resource.PlainTextTestResource;
import nz.ac.auckland.integration.testing.resource.TestResource;
import nz.ac.auckland.integration.testing.validator.HeadersValidator;
import nz.ac.auckland.integration.testing.validator.HttpErrorValidator;
import nz.ac.auckland.integration.testing.validator.Validator;
import org.apache.camel.Exchange;
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
        HttpErrorValidator validator = new HttpErrorValidator.Builder().build();
        assertFalse(validator.validate(null));
    }

    @Test
    public void testNullException() throws Exception {
        HttpErrorValidator validator = new HttpErrorValidator.Builder().build();
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        assertFalse(validator.validate(e));
    }

    @Test
    public void testSetResponseBodyHeadersStatusCode() throws Exception {
        Validator responseBodyValidator = new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return true;
            }
        };

        HeadersValidator responseHeadersValidator = new MockHeadersValidator(null, true);

        HttpErrorValidator validator = new HttpErrorValidator.Builder()
                .responseBodyValidator(responseBodyValidator)
                .responseHeadersValidator(new MockHeadersValidator(null, true))
                .statusCode(500)
                .build();

        assertEquals(validator.getStatusCode(), 500);
        assertEquals(validator.getResponseBodyValidator(), responseBodyValidator);
        assertEquals(validator.getResponseHeadersValidator(), responseHeadersValidator);
    }

    @Test
    public void testWrongException() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(new IOException());
        assertFalse(new HttpErrorValidator().validate(e));
    }

    @Test
    public void testDifferentStatusCode() throws Exception {
        Validator responseBodyValidator = new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return true;
            }
        };

        HttpErrorValidator validator = new HttpErrorValidator.Builder()
                .responseBodyValidator(responseBodyValidator)
                .responseHeadersValidator(new MockHeadersValidator(null, true))
                .statusCode(500)
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(new HttpOperationFailedException("uri", 123, "status", "location", null, null));
        assertFalse(validator.validate(e));
    }

    @Test
    public void testCorrectBody() throws Exception {
        Validator responseBodyValidator = new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return exchange.getIn().getBody(String.class).equals("foo");
            }
        };

        HttpErrorValidator validator = new HttpErrorValidator.Builder()
                .responseBodyValidator(responseBodyValidator)
                .responseHeadersValidator(new MockHeadersValidator(null, false))
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(new HttpOperationFailedException("uri", 123, "status", "location", null, "foo"));
        assertFalse(validator.validate(e));
    }

    @Test
    public void testCorrectHeaders() throws Exception {
        Validator responseBodyValidator = new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return false;
            }
        };

        Validator responseHeadersValidator = new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                String foo = exchange.getIn().getHeader("foo", String.class);
                String baz = exchange.getIn().getHeader("baz", String.class);
                return foo.equals("baz") && baz.equals("foo");
            }
        };

        Map<String, String> map = new HashMap<>();
        map.put("foo", "baz");
        map.put("baz", "moo");

        HttpErrorValidator validator = new HttpErrorValidator.Builder()
                .responseBodyValidator(responseBodyValidator)
                .responseHeadersValidator(new HeadersValidator(new HeadersTestResource((Map) map)))
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(new HttpOperationFailedException("uri", 123, "status", "location", map, "foo"));
        assertFalse(validator.validate(e));
    }

    @Test
    public void testPlainTextResource() throws Exception {
        PlainTextTestResource resource = new PlainTextTestResource("foo");

        HttpErrorValidator validator = new HttpErrorValidator.Builder()
                .responseBodyValidator(resource).build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(new HttpOperationFailedException("uri", 123, "status", "location", null, "foo"));

        assertTrue(validator.validate(e));
    }

    @Test
    public void testJsonResource() throws Exception {
        JsonTestResource resource = new JsonTestResource("{\"foo\":\"baz\"}");

        HttpErrorValidator validator = new HttpErrorValidator.Builder()
                .responseBodyValidator(resource).build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(new HttpOperationFailedException("uri", 123, "status", "location", null, "{\"foo\":\"baz\"}"));

        assertTrue(validator.validate(e));
    }

    @Test
    public void testHeadersResource() throws Exception {
        Validator responseBodyValidator = new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return false;
            }
        };

        Map<String, String> map = new HashMap<>();
        map.put("foo", "baz");
        map.put("baz", "moo");

        HeadersTestResource resource = new HeadersTestResource((Map) map);

        HttpErrorValidator validator = new HttpErrorValidator.Builder()
                .responseBodyValidator(responseBodyValidator)
                .responseHeadersValidator(resource)
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(new HttpOperationFailedException("uri", 123, "status", "location", map, "foo"));
        assertFalse(validator.validate(e));
    }
}

class MockHeadersValidator extends HeadersValidator {

    private boolean response = true;

    public MockHeadersValidator(TestResource<Map<String, Object>> resource, boolean response) {
        super(resource);
        this.response = response;
    }

    public boolean validate(Exchange exchange) {
        return response;
    }
}
