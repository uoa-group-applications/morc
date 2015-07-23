package nz.ac.auckland.morc.tests.resource;

import nz.ac.auckland.morc.resource.HttpStatusCodeTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.component.http.HttpOperationFailedException;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class HttpStatusResourceTest extends Assert {

    @Test
    public void testSetResponseCode() throws Exception {
        HttpStatusCodeTestResource resource = new HttpStatusCodeTestResource(201);
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        resource.process(e);
        assertEquals(201, e.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
    }

    @Test
    public void testNullExchange() throws Exception {
        HttpStatusCodeTestResource resource = new HttpStatusCodeTestResource(201);
        assertFalse(resource.matches(null));
    }

    @Test
    public void testNoResponseCode() throws Exception {
        HttpStatusCodeTestResource resource = new HttpStatusCodeTestResource(201);
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        assertFalse(resource.matches(e));
    }

    @Test
    public void testMismatchStatusCode() throws Exception {
        HttpStatusCodeTestResource resource = new HttpStatusCodeTestResource(201);
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 101);
        assertFalse(resource.matches(e));
    }

    @Test
    public void testMatchStatusCodeLt400() throws Exception {
        HttpStatusCodeTestResource resource = new HttpStatusCodeTestResource(101);
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 101);
        assertTrue(resource.matches(e));
    }

    @Test
    public void testWrongExceptionGe400() throws Exception {
        HttpStatusCodeTestResource resource = new HttpStatusCodeTestResource(500);
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new IOException());
        assertFalse(resource.matches(e));
    }

    @Test
    public void testNullExceptionGe400() throws Exception {
        HttpStatusCodeTestResource resource = new HttpStatusCodeTestResource(500);
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
        assertFalse(resource.matches(e));
    }


    @Test
    public void testFoundExceptionGe400Match() throws Exception {
        HttpStatusCodeTestResource resource = new HttpStatusCodeTestResource(500);
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 505);
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new HttpOperationFailedException(null, 505, null, null, null, null));
        assertFalse(resource.matches(e));
    }

    @Test
    public void testFoundExceptionGe400MisMatch() throws Exception {
        HttpStatusCodeTestResource resource = new HttpStatusCodeTestResource(505);
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 505);
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new HttpOperationFailedException(null, 505, null, null, null, null));
        assertTrue(resource.matches(e));
    }

}
