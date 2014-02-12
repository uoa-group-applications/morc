package nz.ac.auckland.integration.tests.mock;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import nz.ac.auckland.integration.testing.mock.builder.ContentMockDefinitionBuilder;
import nz.ac.auckland.integration.testing.predicate.HeadersPredicate;
import org.apache.camel.Exchange;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import static nz.ac.auckland.integration.testing.MorcTestBuilder.*;


public class ContentMockDefinitionBuilderTest extends Assert {

    @Test
    public void testBodiesAndHeadersMatchedCorrect() throws Exception {

        MockDefinition def = new ContentMockDefinitionBuilder("").expectedBody(text("foo"), text("baz"), text("moo"))
                .expectedHeaders(headers(header("foo", "1")), headers(header("foo", "2")), headers(header("foo", "3")))
                .addRepeatedPredicate(new HeadersPredicate(headers(header("aaa", "bbb")))).build(null);

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
        MockDefinition def = new ContentMockDefinitionBuilder("").expectedBody(text("foo"), text("baz"), text("moo"))
                .expectedHeaders(headers(header("foo", "1")))
                .addRepeatedPredicate(new HeadersPredicate(headers(header("aaa", "bbb")))).build(null);

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
        MockDefinition def = new ContentMockDefinitionBuilder("").expectedBody(text("foo"))
                .expectedHeaders(headers(header("foo", "1")), headers(header("foo", "2")), headers(header("foo", "3")))
                .addRepeatedPredicate(new HeadersPredicate(headers(header("aaa", "bbb")))).build(null);

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
        MockDefinition def = new ContentMockDefinitionBuilder("").expectedBody(text("foo"))
                .expectedHeaders(headers(header("foo", "1"))).expectedMessageCount(3)
                .addRepeatedPredicate(new HeadersPredicate(headers(header("aaa", "bbb")))).build(null);

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
