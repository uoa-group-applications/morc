package nz.ac.auckland.morc.tests.resource;

import nz.ac.auckland.morc.resource.ContentTypeTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

public class ContentTypeTestResourceTest extends Assert {

    @Test
    public void testMatchesEqual() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());

        e.getIn().setHeader(Exchange.CONTENT_TYPE, "foo");

        assertTrue(new ContentTypeTestResource("foo").matches(e));
    }

    @Test
    public void testMatchesNoHeader() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());

        assertFalse(new ContentTypeTestResource("foo").matches(e));
    }

    @Test
    public void testMatchesNotSame() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());

        e.getIn().setHeader(Exchange.CONTENT_TYPE, "baz");

        assertFalse(new ContentTypeTestResource("foo").matches(e));
    }

    @Test
    public void testMatchesDefaultHeader() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());

        e.getIn().setHeader(Exchange.CONTENT_TYPE, "text/plain");

        assertTrue(new ContentTypeTestResource().matches(e));
    }

    @Test
    public void testSetHeader() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());

        new ContentTypeTestResource("foo").process(e);

        assertEquals("foo", e.getIn().getHeader(Exchange.CONTENT_TYPE));
    }

    @Test
    public void testSetDefaultHeader() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());

        new ContentTypeTestResource().process(e);

        assertEquals("text/plain", e.getIn().getHeader(Exchange.CONTENT_TYPE));
    }
}
