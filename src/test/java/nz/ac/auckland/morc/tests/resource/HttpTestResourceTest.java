package nz.ac.auckland.morc.tests.resource;

import nz.ac.auckland.morc.MorcMethods;
import nz.ac.auckland.morc.resource.HttpErrorTestResource;
import nz.ac.auckland.morc.resource.HttpResponseTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.component.http.HttpOperationFailedException;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class HttpTestResourceTest extends Assert implements MorcMethods {

    @Test
    public void testDefaultStatusCodePredicate() throws Exception {
        HttpResponseTestResource resource = new HttpResponseTestResource();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);

        assertTrue(resource.matches(e));
    }

    @Test
    public void testDefaultNonStandardCodeProcessor() throws Exception {
        HttpResponseTestResource resource = new HttpResponseTestResource(505);

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        resource.process(e);

        assertTrue(505 == e.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class));
    }

    @Test
    public void testNonStandardCodeProcessor() throws Exception {
        HttpErrorTestResource resource = new HttpErrorTestResource(555);

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        resource.process(e);

        assertTrue(555 == e.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class));
    }

    @Test
    public void testBodyProcessor() throws Exception {
        HttpResponseTestResource resource = httpResponse(555, text("foo"));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        resource.process(e);

        assertEquals("foo", e.getIn().getBody(String.class));
    }

    @Test
    public void testBodyPredicate() throws Exception {
        HttpResponseTestResource resource = httpResponse(555, text("foo"));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 555);
        e.getIn().setBody("foo");

        assertTrue(resource.matches(e));

        e.getIn().setBody("baz");

        assertFalse(resource.matches(e));
    }

    @Test
    public void testBodyPredicateFails0Status() throws Exception {
        HttpResponseTestResource resource = httpResponse(0, text("foo"));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
        e.getIn().setBody("foo");
        assertTrue(resource.matches(e));

        e.getIn().setBody("baz");
        assertFalse(resource.matches(e));
    }

    @Test
    public void testHeadersProcessor() throws Exception {
        HttpResponseTestResource resource = httpResponse(201, text("foo"), headers(header("foo", "baz"), header("moo", "cow")));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        resource.process(e);

        assertEquals("foo", e.getIn().getBody(String.class));
        assertTrue(201 == e.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Long.class));
        assertEquals("baz", e.getIn().getHeader("foo"));
        assertEquals("cow", e.getIn().getHeader("moo"));
    }

    @Test
    public void testHeadersPredicate() throws Exception {
        HttpResponseTestResource resource = httpResponse(201, text("foo"), headers(header("foo", "baz"), header("moo", "cow")));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("foo");
        e.getIn().setHeader("foo", "baz");
        e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);

        assertFalse(resource.matches(e));

        e.getIn().setHeader("moo", "cow");

        assertTrue(resource.matches(e));
    }

    @Test
    public void testHeadersPredicateFails0Status() throws Exception {
        HttpResponseTestResource resource = httpResponse(0, text("foo"), headers(header("foo", "baz"), header("moo", "cow")));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("foo");
        e.getIn().setHeader("foo", "baz");
        e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);

        assertFalse(resource.matches(e));

        e.getIn().setHeader("moo", "cow");

        assertTrue(resource.matches(e));
    }

    @Test
    public void testHttpErrorPredicate() throws Exception {
        HttpResponseTestResource resource = httpErrorResponse(501, text("foo"), headers(header("foo", "baz"), header("moo", "cow")));

        Map<String, String> map = new HashMap<>();
        map.put("foo", "baz");

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new HttpOperationFailedException("uri", 501, "status", "location", map, "foo"));

        assertFalse(resource.matches(e));

        map.put("moo", "cow");
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new HttpOperationFailedException("uri", 501, "status", "location", map, "foo"));

        assertTrue(resource.matches(e));
    }

    @Test
    public void testHttpErrorWrongExceptionType() throws Exception {
        HttpResponseTestResource resource = httpErrorResponse(501, text("foo"), headers(header("foo", "baz"), header("moo", "cow")));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new Exception());
        e.getIn().setBody("foo");
        e.getIn().setHeader("foo", "baz");
        e.getIn().setHeader("moo", "cow");
        e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);

        assertFalse(resource.matches(e));
    }

    @Test
    public void testHttpErrorDefault500() throws Exception {
        HttpErrorTestResource resource = httpErrorResponse();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        resource.process(e);

        assertTrue(500l == e.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Long.class));
    }

    @Test
    public void testHttpErrorBodyOnly() throws Exception {
        HttpResponseTestResource resource = httpErrorResponse(501, text("foo"));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new HttpOperationFailedException("uri", 501, "status", "location", null, "foo"));
        assertTrue(resource.matches(e));
    }

    @Test
    public void testNoBodyNoHeadersToString() throws Exception {
        assertTrue(new HttpErrorTestResource().toString().contains("HttpErrorTestResource:"));
    }

    @Test
    public void testNoResponseCode() throws Exception {
        HttpResponseTestResource resource = new HttpResponseTestResource();

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        assertFalse(resource.matches(e));
    }

    @Test
    public void testNullExchange() throws Exception {
        HttpResponseTestResource resource = new HttpResponseTestResource();

        assertFalse(resource.matches(null));
    }

    @Test
    public void testNoStatusCodeMatch() throws Exception {
        HttpResponseTestResource resource = new HttpResponseTestResource(201);

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);

        assertFalse(resource.matches(e));
    }

}
