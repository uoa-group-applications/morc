package nz.ac.auckland.integration.tests.specification;

import nz.ac.auckland.integration.testing.processor.BodyProcessor;
import nz.ac.auckland.integration.testing.specification.AsyncOrchestratedTestBuilder;
import nz.ac.auckland.integration.testing.specification.OrchestratedTestSpecification;
import nz.ac.auckland.integration.testing.specification.SyncOrchestratedTestBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;
import static nz.ac.auckland.integration.testing.MorcTestBuilder.*;

public class OrchestratedTestBuilderTest extends Assert {

    @Test
    public void testNoProcessorsSpecified() throws Exception {
        IllegalArgumentException e = null;
        try {
            new OrchestratedTestSpecification.OrchestratedTestSpecificationBuilder("foo","uri")
                .build();
        } catch (IllegalArgumentException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testMoreProcessorsThanPredicates() throws Exception {

        OrchestratedTestSpecification test = new OrchestratedTestSpecification.OrchestratedTestSpecificationBuilder("foo","uri")
            .addProcessors(new BodyProcessor(text("foo"))).addProcessors(new BodyProcessor(text("foo")))
            .addPredicates(text("baz"))
            .build();

        assertEquals(2,test.getProcessors().size());
        assertEquals(2,test.getPredicates().size());
        assertEquals(2,test.getTotalPublishMessageCount());
        assertEquals(0,test.getTotalMockMessageCount());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("baz");

        assertTrue(test.getPredicates().get(0).matches(e));
        assertTrue(test.getPredicates().get(1).matches(e));

        e.setProperty(Exchange.EXCEPTION_CAUGHT,new Exception());
        assertFalse(test.getPredicates().get(0).matches(e));
        assertFalse(test.getPredicates().get(1).matches(e));

        assertEquals(10000+(1000*2),test.getResultWaitTime());
    }

    @Test
    public void testExpectedException() throws Exception {
        OrchestratedTestSpecification test = new OrchestratedTestSpecification.OrchestratedTestSpecificationBuilder("foo","uri")
            .addProcessors(new BodyProcessor(text("foo"))).addProcessors(new BodyProcessor(text("foo")))
            .expectsException()
            .addPredicates(text("baz"))
            .build();

        assertEquals(2,test.getProcessors().size());
        assertEquals(2,test.getPredicates().size());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("baz");

        assertFalse(test.getPredicates().get(0).matches(e));
        assertFalse(test.getPredicates().get(1).matches(e));

        e.setProperty(Exchange.EXCEPTION_CAUGHT,new Exception());
        assertTrue(test.getPredicates().get(0).matches(e));
        assertTrue(test.getPredicates().get(1).matches(e));
    }

    @Test
    public void testAddEndpointSameClass() throws Exception {
        OrchestratedTestSpecification test = new SyncOrchestratedTestBuilder("foo","url").requestBody(text("foo")).requestBody(text("baz"))
                .requestHeaders(headers(header("1", "1"))).requestHeaders(headers(header("2","2")))
                .expectedResponseBody(text("1")).expectedResponseBody(text("2"))
                .expectedResponseHeaders(headers(header("foo","baz"))).expectedResponseHeaders(headers(header("baz","foo")))
                .addEndpoint("2")
                    .requestBody(text("foo"))
                    .requestHeaders(headers(header("1","1"))).requestHeaders(headers(header("2","2")))
                    .expectedResponseBody(text("1"))
                    .expectedResponseHeaders(headers(header("foo","baz"))).expectedResponseHeaders(headers(header("baz","foo"))).build();

        assertEquals(2,test.getPartCount());
        assertNotNull(test.getNextPart());
        assertEquals(2,test.getProcessors().size());
        assertEquals(2,test.getPredicates().size());
        assertEquals("url",test.getEndpointUri());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("1");
        assertFalse(test.getPredicates().get(0).matches(e));
        e.getIn().setHeader("foo","baz");
        assertTrue(test.getPredicates().get(0).matches(e));

        e.getIn().setBody("2");
        assertFalse(test.getPredicates().get(1).matches(e));
        e.getIn().setHeader("baz","foo");
        assertTrue(test.getPredicates().get(1).matches(e));

        test.getProcessors().get(0).process(e);
        assertEquals("foo",e.getIn().getBody(String.class));
        assertEquals("1",e.getIn().getHeader("1"));

        test.getProcessors().get(1).process(e);
        assertEquals("baz",e.getIn().getBody(String.class));
        assertEquals("2",e.getIn().getHeader("2"));

        test = test.getNextPart();

        assertEquals("2",test.getEndpointUri());
        assertEquals(2,test.getProcessors().size());
        assertEquals(2,test.getPredicates().size());

        e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("1");
        assertFalse(test.getPredicates().get(0).matches(e));
        e.getIn().setHeader("foo","baz");
        assertTrue(test.getPredicates().get(0).matches(e));

        e.getIn().setHeader("baz","foo");
        assertTrue(test.getPredicates().get(1).matches(e));

        test.getProcessors().get(0).process(e);
        assertEquals("foo",e.getIn().getBody(String.class));
        assertEquals("1",e.getIn().getHeader("1"));

        test.getProcessors().get(1).process(e);
        assertEquals("2",e.getIn().getHeader("2"));
    }

    @Test
    public void testAddEndpointDifferentClass() throws Exception {
        OrchestratedTestSpecification test = new SyncOrchestratedTestBuilder("foo","url").requestBody(text("foo")).requestBody(text("baz"))
                .requestHeaders(headers(header("1","1"))).requestHeaders(headers(header("2","2")))
                .expectedResponseBody(text("1")).expectedResponseBody(text("2"))
                .expectedResponseHeaders(headers(header("foo","baz"))).expectedResponseHeaders(headers(header("baz","foo")))
                .addEndpoint("2", AsyncOrchestratedTestBuilder.class).inputMessage(text("foo")).inputMessage(text("baz"))
                                .inputHeaders(headers(header("1", "1"))).inputHeaders(headers(header("2", "2"))).build();

        assertEquals(2,test.getPartCount());
        assertNotNull(test.getNextPart());
        assertEquals(2,test.getProcessors().size());
        assertEquals(2,test.getPredicates().size());
        assertEquals("url",test.getEndpointUri());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("1");
        assertFalse(test.getPredicates().get(0).matches(e));
        e.getIn().setHeader("foo","baz");
        assertTrue(test.getPredicates().get(0).matches(e));

        e.getIn().setBody("2");
        assertFalse(test.getPredicates().get(1).matches(e));
        e.getIn().setHeader("baz","foo");
        assertTrue(test.getPredicates().get(1).matches(e));

        test.getProcessors().get(0).process(e);
        assertEquals("foo",e.getIn().getBody(String.class));
        assertEquals("1",e.getIn().getHeader("1"));

        test.getProcessors().get(1).process(e);
        assertEquals("baz",e.getIn().getBody(String.class));
        assertEquals("2",e.getIn().getHeader("2"));

        test = test.getNextPart();

        assertEquals(2,test.getProcessors().size());
        assertEquals(2,test.getPredicates().size());
        assertEquals("2",test.getEndpointUri());

        e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));

        test.getProcessors().get(0).process(e);
        assertEquals("foo",e.getIn().getBody(String.class));
        assertEquals("1",e.getIn().getHeader("1"));

        test.getProcessors().get(1).process(e);
        assertEquals("baz",e.getIn().getBody(String.class));
        assertEquals("2",e.getIn().getHeader("2"));
    }

}
