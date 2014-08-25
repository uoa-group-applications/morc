package nz.ac.auckland.morc.tests.specification;

import nz.ac.auckland.morc.mock.MockDefinition;
import nz.ac.auckland.morc.processor.BodyProcessor;
import nz.ac.auckland.morc.specification.AsyncOrchestratedTestBuilder;
import nz.ac.auckland.morc.specification.OrchestratedTestSpecification;
import nz.ac.auckland.morc.specification.SyncOrchestratedTestBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

import static nz.ac.auckland.morc.MorcTestBuilder.*;

public class OrchestratedTestBuilderTest extends Assert {

    @Test
    public void testNoProcessorsSpecified() throws Exception {
        IllegalArgumentException e = null;
        try {
            new OrchestratedTestSpecification.OrchestratedTestSpecificationBuilder("foo", "uri")
                    .build();
        } catch (IllegalArgumentException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testMoreProcessorsThanPredicates() throws Exception {

        OrchestratedTestSpecification test = new OrchestratedTestSpecification.OrchestratedTestSpecificationBuilder("foo", "uri")
                .addProcessors(new BodyProcessor(text("foo"))).addProcessors(new BodyProcessor(text("foo")))
                .addPredicates(text("baz"))
                .build();

        assertEquals(2, test.getProcessors().size());
        assertEquals(2, test.getPredicates().size());
        assertEquals(2, test.getTotalPublishMessageCount());
        assertEquals(0, test.getTotalMockMessageCount());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("baz");

        assertTrue(test.getPredicates().get(0).matches(e));
        assertTrue(test.getPredicates().get(1).matches(e));

        e.setProperty(Exchange.EXCEPTION_CAUGHT, new Exception());
        assertFalse(test.getPredicates().get(0).matches(e));
        assertFalse(test.getPredicates().get(1).matches(e));
    }

    @Test
    public void testExpectedException() throws Exception {
        OrchestratedTestSpecification test = new OrchestratedTestSpecification.OrchestratedTestSpecificationBuilder("foo", "uri")
                .addProcessors(new BodyProcessor(text("foo"))).addProcessors(new BodyProcessor(text("foo")))
                .expectsException()
                .addPredicates(text("baz"))
                .build();

        assertEquals(2, test.getProcessors().size());
        assertEquals(2, test.getPredicates().size());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("baz");

        assertFalse(test.getPredicates().get(0).matches(e));
        assertFalse(test.getPredicates().get(1).matches(e));

        e.setProperty(Exchange.EXCEPTION_CAUGHT, new Exception());
        assertTrue(test.getPredicates().get(0).matches(e));
        assertTrue(test.getPredicates().get(1).matches(e));
    }

    @Test
    public void testAddEndpointSameClass() throws Exception {
        OrchestratedTestSpecification test = new SyncOrchestratedTestBuilder("foo", "url").requestBody(text("foo")).requestBody(text("baz"))
                .requestHeaders(headers(header("1", "1"))).requestHeaders(headers(header("2", "2")))
                .expectedResponseBody(text("1")).expectedResponseBody(text("2"))
                .expectedResponseHeaders(headers(header("foo", "baz"))).expectedResponseHeaders(headers(header("baz", "foo")))
                .addEndpoint("2")
                .requestBody(text("foo"))
                .requestHeaders(headers(header("1", "1"))).requestHeaders(headers(header("2", "2")))
                .expectedResponseBody(text("1"))
                .expectedResponseHeaders(headers(header("foo", "baz"))).expectedResponseHeaders(headers(header("baz", "foo"))).build();

        assertEquals(2, test.getPartCount());
        assertNotNull(test.getNextPart());
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

        test = test.getNextPart();

        assertEquals("2", test.getEndpointUri());
        assertEquals(2, test.getProcessors().size());
        assertEquals(2, test.getPredicates().size());

        e = new DefaultExchange(new DefaultCamelContext());
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

    @Test
    public void testAddEndpointDifferentClass() throws Exception {
        OrchestratedTestSpecification test = new SyncOrchestratedTestBuilder("foo", "url").requestBody(text("foo")).requestBody(text("baz"))
                .requestHeaders(headers(header("1", "1"))).requestHeaders(headers(header("2", "2")))
                .expectedResponseBody(text("1")).expectedResponseBody(text("2"))
                .expectedResponseHeaders(headers(header("foo", "baz"))).expectedResponseHeaders(headers(header("baz", "foo")))
                .addEndpoint("2", AsyncOrchestratedTestBuilder.class).inputMessage(text("foo")).inputMessage(text("baz"))
                .inputHeaders(headers(header("1", "1"))).inputHeaders(headers(header("2", "2"))).build();

        assertEquals(2, test.getPartCount());
        assertNotNull(test.getNextPart());
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

        test = test.getNextPart();

        assertEquals(2, test.getProcessors().size());
        assertEquals(2, test.getPredicates().size());
        assertEquals("2", test.getEndpointUri());

        e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));

        test.getProcessors().get(0).process(e);
        assertEquals("foo", e.getIn().getBody(String.class));
        assertEquals("1", e.getIn().getHeader("1"));

        test.getProcessors().get(1).process(e);
        assertEquals("baz", e.getIn().getBody(String.class));
        assertEquals("2", e.getIn().getHeader("2"));
    }


    @Test
    public void testAddEndpointBuildFirstClass() throws Exception {
        SyncOrchestratedTestBuilder test = new SyncOrchestratedTestBuilder("foo", "url");

        test.requestBody(text("foo")).requestBody(text("baz"))
                .requestHeaders(headers(header("1", "1"))).requestHeaders(headers(header("2", "2")))
                .expectedResponseBody(text("1")).expectedResponseBody(text("2"))
                .expectedResponseHeaders(headers(header("foo", "baz"))).expectedResponseHeaders(headers(header("baz", "foo")))
                .addEndpoint("2", asyncTest()).inputMessage(text("foo")).inputMessage(text("baz"))
                .inputHeaders(headers(header("1", "1"))).inputHeaders(headers(header("2", "2")))
                .addEndpoint("3", syncTest()).requestBody(text("foo")).requestBody(text("baz"))
                .requestHeaders(headers(header("1", "1"))).requestHeaders(headers(header("2", "2"))).build();

        OrchestratedTestSpecification spec = test.build();

        assertEquals(3, spec.getPartCount());
    }


    @Test
    public void testUnexpectedException() throws Exception {

        OrchestratedTestSpecification test = new OrchestratedTestSpecification.OrchestratedTestSpecificationBuilder("foo", "baz")
                .addProcessors(new BodyProcessor(text("1"))).addProcessors(new BodyProcessor(text("2")))
                .addPredicates(text("1")).addPredicates(text("2")).build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("1");

        assertTrue(test.getPredicates().get(0).matches(e));
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new Exception());
        assertFalse(test.getPredicates().get(0).matches(e));

        e.getIn().setBody("2");
        e.removeProperty(Exchange.EXCEPTION_CAUGHT);
        assertTrue(test.getPredicates().get(1).matches(e));
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new Exception());
        assertFalse(test.getPredicates().get(1).matches(e));
    }

    @Test
    public void testOrderingConfiguredCorrectly() throws Exception {
        OrchestratedTestSpecification test = new OrchestratedTestSpecification.OrchestratedTestSpecificationBuilder("foo", "baz")
                .addProcessors(new BodyProcessor(text("foo")))
                .addExpectation(asyncExpectation("baz").expectedBody(text("1")).expectedBody(text("2")))
                .addExpectation(syncExpectation("foo").expectedBody(text("1")).expectedBody(text("2")))
                .addExpectation(asyncExpectation("moo").expectedBody(text("1")).expectedBody(text("2")).ordering(MockDefinition.OrderingType.NONE))
                .addExpectation(asyncExpectation("baz").expectedBody(text("3")).expectedBody(text("4")))
                .addExpectation(syncExpectation("foo").expectedBody(text("3")).expectedBody(text("4")))
                .addExpectation(asyncExpectation("baz").expectedBody(text("4")).expectedBody(text("5")))
                .addExpectation(asyncExpectation("cow").expectedBody(text("1")).expectedBody(text("2")).ordering(MockDefinition.OrderingType.NONE))
                .build();

        assertEquals(7, test.getEndpointNodesOrdering().size());
        OrchestratedTestSpecification.EndpointNode nextNode = null;
        for (OrchestratedTestSpecification.EndpointNode node : test.getEndpointNodesOrdering()) {
            if (!node.getEndpointUri().equals("foo")) {
                assertEquals(0, node.getChildrenNodes().size());
                assertTrue(node.getEndpointUri().equals("moo") || node.getEndpointUri().equals("baz") || node.getEndpointUri().equals("cow"));
                continue;
            }
            nextNode = node;
        }

        assertNotNull(nextNode);

        assertEquals(1, nextNode.getChildrenNodes().size());
        assertEquals("foo", nextNode.getEndpointUri());
        nextNode = new ArrayList<>(nextNode.getChildrenNodes()).get(0);
        for (OrchestratedTestSpecification.EndpointNode node : nextNode.getChildrenNodes()) {
            if (node.getEndpointUri().equals("foo")) {
                nextNode = node;
                continue;
            }
            assertEquals("baz", node.getEndpointUri());
            assertEquals("baz", node.getEndpointUri());
        }

        assertEquals(1, nextNode.getChildrenNodes().size());
        for (OrchestratedTestSpecification.EndpointNode node : nextNode.getChildrenNodes()) {
            assertEquals("foo", node.getEndpointUri());
            nextNode = node;
        }

        assertEquals(2, nextNode.getChildrenNodes().size());
        for (OrchestratedTestSpecification.EndpointNode node : nextNode.getChildrenNodes()) {
            assertEquals("baz", node.getEndpointUri());
            assertEquals(0, node.getChildrenNodes().size());
        }


    }

    @Test
    public void testExecuteDelay() throws Exception {
        OrchestratedTestSpecification test = new AsyncOrchestratedTestBuilder("foo", "baz")
                .inputMessage(text("foo"))
                .executeDelay(1000l).addPart("baz").inputMessage(text("foo")).executeDelay(2000l).addPart("moo")
                .inputMessage(text("foo")).build();

        assertEquals(1000l, test.getExecuteDelay().delay());
        assertEquals(2000l, test.getNextPart().getExecuteDelay().delay());
        assertEquals(0, test.getNextPart().getNextPart().getExecuteDelay().delay());
    }
}
