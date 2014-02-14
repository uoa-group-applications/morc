package nz.ac.auckland.integration.tests.mock;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import nz.ac.auckland.integration.testing.mock.builder.SyncMockDefinitionBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import static nz.ac.auckland.integration.testing.MorcTestBuilder.*;

public class SyncMockDefinitionBuilderTest extends Assert {

    //to illustrate you must still specify the number of expected messages
    @Test
    public void testMatchedBodyAndHeadersNoExpectatations() throws Exception {

        MockDefinition def = new SyncMockDefinitionBuilder("")
                .responseBody(text("1"),text("2"),text("3"))
                .responseHeaders(headers(header("1","1")),headers(header("2","2")),headers(header("3","3"))).build(null);

        assertEquals(0,def.getExpectedMessageCount());
        assertEquals(0,def.getProcessors().size());
        assertEquals(0,def.getPredicates().size());
    }

    @Test
    public void testMatchedBodyAndHeaders() throws Exception {

        MockDefinition def = new SyncMockDefinitionBuilder("")
                .responseBody(text("1")).responseBody(text("2").getValue(), text("3").getValue()).expectedMessageCount(4)
                .responseHeaders(headers(header("1", "1")), headers(header("2", "2")), headers(header("3", "3"))).build(null);

        assertEquals(4,def.getExpectedMessageCount());
        assertEquals(4,def.getPredicates().size());
        assertEquals(4,def.getProcessors().size());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("");
        e.getIn().setHeader("1","1");

        def.getProcessors().get(0).process(e);
        assertEquals("1", e.getIn().getBody(String.class));
        assertEquals("1", e.getIn().getHeader("1"));

        def.getProcessors().get(1).process(e);
        assertEquals("2",e.getIn().getBody(String.class));
        assertEquals("2",e.getIn().getHeader("2"));

        def.getProcessors().get(2).process(e);
        assertEquals("3",e.getIn().getBody(String.class));
        assertEquals("3",e.getIn().getHeader("3"));

        e.getIn().setBody("");
        e.getIn().removeHeaders("*");

        def.getProcessors().get(3).process(e);
        assertEquals("", e.getIn().getBody(String.class));
        assertNull(e.getIn().getHeader("3"));
    }

    @Test
    public void testMoreBodiesThanExpectedMessages() throws Exception {
        MockDefinition def = new SyncMockDefinitionBuilder("")
                .responseBody(text("1"),text("2"),text("3"))
                .expectedMessageCount(1).build(null);

        assertEquals(1,def.getExpectedMessageCount());
        assertEquals(1,def.getProcessors().size());
        assertEquals(1,def.getPredicates().size());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("");

        def.getProcessors().get(0).process(e);
        assertEquals("1", e.getIn().getBody(String.class));
    }

    @Test
    public void testMoreBodiesThanHeaders() throws Exception {
        MockDefinition def = new SyncMockDefinitionBuilder("")
                .responseBody(text("1"), text("2"), text("3")).expectedMessageCount(3)
                .responseHeaders(headers(header("1", "1"))).build(null);

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("");

        def.getProcessors().get(0).process(e);
        assertEquals("1", e.getIn().getBody(String.class));
        assertEquals("1", e.getIn().getHeader("1"));

        e.getIn().removeHeader("1");
        e.getIn().setBody("");
        def.getProcessors().get(1).process(e);
        assertEquals("2",e.getIn().getBody(String.class));
        assertNull(e.getIn().getHeader("1"));

        e.getIn().removeHeader("1");
        e.getIn().setBody("");
        def.getProcessors().get(2).process(e);
        assertEquals("3",e.getIn().getBody(String.class));
        assertNull(e.getIn().getHeader("1"));

    }

    @Test
    public void testMoreHeadersThanBodies() throws Exception {
        MockDefinition def = new SyncMockDefinitionBuilder("")
                .responseBody(text("1")).expectedMessageCount(3)
                .responseHeaders(headers(header("1", "1")), headers(header("2", "2")), headers(header("3", "3"))).build(null);

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("");

        def.getProcessors().get(0).process(e);
        assertEquals("1", e.getIn().getBody(String.class));
        assertEquals("1", e.getIn().getHeader("1"));

        e.getIn().removeHeaders("*");
        e.getIn().setBody("");
        def.getProcessors().get(1).process(e);
        assertEquals("2",e.getIn().getHeader("2"));
        assertEquals("",e.getIn().getBody(String.class));

        e.getIn().removeHeaders("*");
        e.getIn().setBody("");
        def.getProcessors().get(2).process(e);
        assertEquals("3",e.getIn().getHeader("3"));
        assertEquals("",e.getIn().getBody(String.class));
    }


}
