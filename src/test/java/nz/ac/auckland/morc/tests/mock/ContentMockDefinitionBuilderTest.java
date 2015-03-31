package nz.ac.auckland.morc.tests.mock;

import nz.ac.auckland.morc.MorcMethods;
import nz.ac.auckland.morc.mock.MockDefinition;
import nz.ac.auckland.morc.mock.builder.ContentMockDefinitionBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;


public class ContentMockDefinitionBuilderTest extends Assert implements MorcMethods {

    @Test
    public void testBodiesAndHeadersMatchedCorrect() throws Exception {

        MockDefinition def = new ContentMockDefinitionBuilder("").expectation(text("foo"), headers(header("foo", "1")))
                .expectation(text("baz"), headers(header("foo", "2")))
                .expectation(text("moo"), headers(header("foo", "3")))
                .addRepeatedPredicate(headers(header("aaa", "bbb"))).build(null);

        assertEquals(3, def.getPredicates().size());
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));

        e.getIn().setHeader("aaa", "bbb");
        e.getIn().setBody("foo");
        e.getIn().setHeader("foo", "1");
        assertTrue(def.getPredicates().get(0).matches(e));
        e.getIn().setBody("baz");
        assertFalse(def.getPredicates().get(0).matches(e));

        e.getIn().setBody("baz");
        e.getIn().setHeader("foo", "2");
        assertTrue(def.getPredicates().get(1).matches(e));

        e.getIn().setBody("moo");
        e.getIn().setHeader("foo", "3");
        assertTrue(def.getPredicates().get(2).matches(e));
    }

    @Test
    public void testMoreBodiesThanHeaders() throws Exception {
        MockDefinition def = new ContentMockDefinitionBuilder("").expectation(text("foo"), headers(header("foo", "1")))
                .expectation(text("baz"))
                .expectation(text("moo"))
                .addRepeatedPredicate(headers(header("aaa", "bbb"))).build(null);

        assertEquals(3, def.getPredicates().size());
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));

        e.getIn().setHeader("foo", "1");
        e.getIn().setHeader("aaa", "bbb");
        e.getIn().setBody("foo");
        assertTrue(def.getPredicates().get(0).matches(e));
        e.getIn().setBody("baz");
        assertFalse(def.getPredicates().get(0).matches(e));

        e.getIn().setBody("baz");
        assertTrue(def.getPredicates().get(1).matches(e));

        e.getIn().setBody("moo");
        assertTrue(def.getPredicates().get(2).matches(e));
    }

    @Test
    public void testMoreHeadersThanBodies() throws Exception {
        MockDefinition def = new ContentMockDefinitionBuilder("")
                .expectation(text("foo"), headers(header("foo", "1")))
                .expectation(headers(header("foo", "2")))
                .expectation(headers(header("foo", "3")))
                .addRepeatedPredicate(headers(header("aaa", "bbb"))).build(null);

        assertEquals(3, def.getPredicates().size());
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));

        e.getIn().setHeader("aaa", "bbb");
        e.getIn().setBody("foo");
        e.getIn().setHeader("foo", "1");
        assertTrue(def.getPredicates().get(0).matches(e));
        e.getIn().setBody("baz");
        assertFalse(def.getPredicates().get(0).matches(e));

        e.getIn().setBody("foo");
        e.getIn().setHeader("foo", "2");
        assertTrue(def.getPredicates().get(1).matches(e));
        e.getIn().setBody("foo");
        e.getIn().setHeader("foo", "3");
        assertTrue(def.getPredicates().get(2).matches(e));
    }

    @Test
    public void testBodiesAndHeadersLargeExpectedMessageCount() throws Exception {
        MockDefinition def = new ContentMockDefinitionBuilder("").expectation(text("foo"), headers(header("foo", "1")))
                .expectedMessageCount(3)
                .addRepeatedPredicate(headers(header("aaa", "bbb"))).build(null);

        assertEquals(3, def.getPredicates().size());
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));

        e.getIn().setHeader("aaa", "bbb");
        e.getIn().setBody("foo");
        e.getIn().setHeader("foo", "1");
        assertTrue(def.getPredicates().get(0).matches(e));

        //should accept anything in bodies/headers
        e.getIn().setBody("notfoo");
        e.getIn().setHeader("foo", "2");

        assertTrue(def.getPredicates().get(1).matches(e));

        assertTrue(def.getPredicates().get(2).matches(e));
    }

}
