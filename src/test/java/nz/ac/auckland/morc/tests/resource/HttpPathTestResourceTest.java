package nz.ac.auckland.morc.tests.resource;

import nz.ac.auckland.morc.resource.HttpPathTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

public class HttpPathTestResourceTest extends Assert {

    @Test
    public void testSettingHeader() throws Exception {
        HttpPathTestResource resource = new HttpPathTestResource("/foo");
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        resource.process(e);
        assertEquals("/foo",e.getIn().getHeader(Exchange.HTTP_PATH));
    }

    @Test
    public void testNullExchange() throws Exception {
        HttpPathTestResource resource = new HttpPathTestResource("/foo");
        assertFalse(resource.matches(null));
    }

    @Test
    public void testExchangeNoPath() throws Exception {
        HttpPathTestResource resource = new HttpPathTestResource("/foo");
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        assertFalse(resource.matches(e));
    }

    @Test
    public void testPathMatches() throws Exception {
        HttpPathTestResource resource = new HttpPathTestResource("/foo");
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setHeader(Exchange.HTTP_PATH,"/foo");
        assertTrue(resource.matches(e));
    }

    @Test
    public void testPathMismatch() throws Exception {
        HttpPathTestResource resource = new HttpPathTestResource("/foo");
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setHeader(Exchange.HTTP_PATH,"/baz");
        assertFalse(resource.matches(e));
    }
}
