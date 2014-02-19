package nz.ac.auckland.morc.tests.specification;

import nz.ac.auckland.morc.specification.AsyncOrchestratedTestBuilder;
import nz.ac.auckland.morc.specification.OrchestratedTestSpecification;
import org.apache.camel.Exchange;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import static nz.ac.auckland.morc.MorcTestBuilder.*;

public class AsyncOrchestratedTestBuilderTest extends Assert {

    @Test
    public void testEqualBodiesAndHeaders() throws Exception {

        OrchestratedTestSpecification test = new AsyncOrchestratedTestBuilder("foo", "url").inputMessage(text("foo")).inputMessage(text("baz"))
                .inputHeaders(headers(header("1", "1"))).inputHeaders(headers(header("2", "2"))).build();

        assertEquals(2, test.getProcessors().size());
        assertEquals(2, test.getPredicates().size());
        assertEquals("url", test.getEndpointUri());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));

        test.getProcessors().get(0).process(e);
        assertEquals("foo", e.getIn().getBody(String.class));
        assertEquals("1", e.getIn().getHeader("1"));

        test.getProcessors().get(1).process(e);
        assertEquals("baz", e.getIn().getBody(String.class));
        assertEquals("2", e.getIn().getHeader("2"));
    }

    @Test
    public void testMoreBodiesThanHeaders() throws Exception {
        OrchestratedTestSpecification test = new AsyncOrchestratedTestBuilder("foo", "url")
                .inputMessage(text("foo"), text("baz"))
                .inputHeaders(headers(header("1", "1"))).build();

        assertEquals(2, test.getProcessors().size());
        assertEquals(2, test.getPredicates().size());
        assertEquals("url", test.getEndpointUri());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));

        test.getProcessors().get(0).process(e);
        assertEquals("foo", e.getIn().getBody(String.class));
        assertEquals("1", e.getIn().getHeader("1"));

        test.getProcessors().get(1).process(e);
        assertEquals("baz", e.getIn().getBody(String.class));
    }

    @Test
    public void testMoreHeadersThanBodies() throws Exception {
        OrchestratedTestSpecification test = new AsyncOrchestratedTestBuilder("foo", "url").inputMessage(text("foo"))
                .inputHeaders(headers(header("1", "1"))).inputHeaders(headers(header("2", "2"))).build();

        assertEquals(2, test.getProcessors().size());
        assertEquals(2, test.getPredicates().size());
        assertEquals("url", test.getEndpointUri());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));

        test.getProcessors().get(0).process(e);
        assertEquals("foo", e.getIn().getBody(String.class));
        assertEquals("1", e.getIn().getHeader("1"));

        test.getProcessors().get(1).process(e);
        assertEquals("foo", e.getIn().getBody(String.class));
        assertEquals("2", e.getIn().getHeader("2"));
    }

}
