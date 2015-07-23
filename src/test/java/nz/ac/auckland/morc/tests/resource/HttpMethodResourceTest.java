package nz.ac.auckland.morc.tests.resource;

import nz.ac.auckland.morc.resource.HttpMethodTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

public class HttpMethodResourceTest extends Assert {

    @Test
    public void testNullExchange() throws Exception {
        HttpMethodTestResource resource = new HttpMethodTestResource(HttpMethodTestResource.HttpMethod.POST);
        assertFalse(resource.matches(null));

    }

    @Test
    public void testNoHeader() throws Exception {
        HttpMethodTestResource resource = new HttpMethodTestResource(HttpMethodTestResource.HttpMethod.POST);
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        assertFalse(resource.matches(e));
    }

    @Test
    public void testMethodMatch() throws Exception {
        HttpMethodTestResource resource = new HttpMethodTestResource(HttpMethodTestResource.HttpMethod.POST);
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setHeader(Exchange.HTTP_METHOD,"POST");
        assertTrue(resource.matches(e));
    }

    @Test
    public void testMethodMisMatch() throws Exception {
        HttpMethodTestResource resource = new HttpMethodTestResource(HttpMethodTestResource.HttpMethod.POST);
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setHeader(Exchange.HTTP_METHOD,"GET");
        assertFalse(resource.matches(e));
    }

    @Test
    public void testSetHttpMethod() throws Exception {
        HttpMethodTestResource resource = new HttpMethodTestResource(HttpMethodTestResource.HttpMethod.POST);
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        resource.process(e);
        assertEquals("POST",e.getIn().getHeader(Exchange.HTTP_METHOD));
    }

}
