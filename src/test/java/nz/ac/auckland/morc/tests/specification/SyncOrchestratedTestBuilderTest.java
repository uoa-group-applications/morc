package nz.ac.auckland.morc.tests.specification;

import nz.ac.auckland.morc.MorcMethods;
import nz.ac.auckland.morc.processor.BodyProcessor;
import nz.ac.auckland.morc.specification.OrchestratedTestSpecification;
import nz.ac.auckland.morc.specification.SyncOrchestratedTestBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

public class SyncOrchestratedTestBuilderTest extends Assert implements MorcMethods {

    @Test
    public void testEqualBodiesAndHeaders() throws Exception {

        OrchestratedTestSpecification test = new SyncOrchestratedTestBuilder("foo", "url").requestBody(text("foo")).requestBody(text("baz"))
                .requestHeaders(headers(header("1", "1"))).requestHeaders(headers(header("2", "2")))
                .expectedResponseBody(text("1")).expectedResponseBody(text("2"))
                .expectedResponseHeaders(headers(header("foo", "baz"))).expectedResponseHeaders(headers(header("baz", "foo"))).build();

        assertEquals(2, test.getProcessors().size());
        assertEquals(2, test.getPredicates().size());
        assertEquals("url", test.getEndpointUri());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("1");
        assertFalse(test.getPredicates().get(0).matches(e));
        e.getIn().setHeader("foo", "baz");
        assertTrue(test.getPredicates().get(0).matches(e));

        e.getIn().setBody("2");
        assertFalse(test.getPredicates().get(1).matches(e));
        e.getIn().setHeader("baz", "foo");
        assertTrue(test.getPredicates().get(1).matches(e));

        test.getProcessors().get(0).process(e);
        assertEquals("foo", e.getIn().getBody(String.class));
        assertEquals("1", e.getIn().getHeader("1"));

        test.getProcessors().get(1).process(e);
        assertEquals("baz", e.getIn().getBody(String.class));
        assertEquals("2", e.getIn().getHeader("2"));
    }

    @Test
    public void testMoreBodiesThanHeaders() throws Exception {
        OrchestratedTestSpecification test = new SyncOrchestratedTestBuilder("foo", "url")
                .requestBody(text("foo"), text("baz"))
                .requestHeaders(headers(header("1", "1")))
                .expectedResponseBody(text("1")).expectedResponseBody(text("2"))
                .expectedResponseHeaders(headers(header("foo", "baz"))).build();

        assertEquals(2, test.getProcessors().size());
        assertEquals(2, test.getPredicates().size());
        assertEquals("url", test.getEndpointUri());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("1");
        assertFalse(test.getPredicates().get(0).matches(e));
        e.getIn().setHeader("foo", "baz");
        assertTrue(test.getPredicates().get(0).matches(e));

        e.getIn().setBody("2");
        assertTrue(test.getPredicates().get(1).matches(e));

        test.getProcessors().get(0).process(e);
        assertEquals("foo", e.getIn().getBody(String.class));
        assertEquals("1", e.getIn().getHeader("1"));

        test.getProcessors().get(1).process(e);
        assertEquals("baz", e.getIn().getBody(String.class));
    }

    @Test
    public void testMoreHeadersThanBodies() throws Exception {
        OrchestratedTestSpecification test = new SyncOrchestratedTestBuilder("foo", "url")
                .request(new BodyProcessor(text("foo")))
                .requestHeaders(headers(header("1", "1"))).requestHeaders(headers(header("2", "2")))
                .expectedResponseBody(text("1"))
                .expectedResponseHeaders(headers(header("foo", "baz"))).expectedResponseHeaders(headers(header("baz", "foo"))).build();

        assertEquals(2, test.getProcessors().size());
        assertEquals(2, test.getPredicates().size());
        assertEquals("url", test.getEndpointUri());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("1");
        assertFalse(test.getPredicates().get(0).matches(e));
        e.getIn().setHeader("foo", "baz");
        assertTrue(test.getPredicates().get(0).matches(e));

        e.getIn().setHeader("baz", "foo");
        assertTrue(test.getPredicates().get(1).matches(e));

        test.getProcessors().get(0).process(e);
        assertEquals("foo", e.getIn().getBody(String.class));
        assertEquals("1", e.getIn().getHeader("1"));

        test.getProcessors().get(1).process(e);
        assertEquals("2", e.getIn().getHeader("2"));
    }

}
