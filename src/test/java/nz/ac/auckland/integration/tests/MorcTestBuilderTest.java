package nz.ac.auckland.integration.tests;

import nz.ac.auckland.integration.testing.predicate.HttpErrorPredicate;
import nz.ac.auckland.integration.testing.resource.PlainTextTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static nz.ac.auckland.integration.testing.MorcTestBuilder.*;

public class MorcTestBuilderTest extends Assert {

    @Test
    public void testDelayProcessor() throws Exception {
        long time = new Date().getTime();
        delay(5000).process(null);
        long finishTime = new Date().getTime();

        assertTrue(finishTime >= time+5000l);
    }

    @Test
    public void testHttpExceptionResponseCode() {
        assertEquals(0,httpExceptionResponse().build().getStatusCode());
        assertEquals(123,httpExceptionResponse(123).getStatusCode());
    }

    @Test
    public void testHttpExceptionResponseCodePredicate() throws Exception {
        Predicate p = new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                exchange.setProperty("foo","baz");
                return true;
            }
        };

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        HttpErrorPredicate predicate = httpExceptionResponse(123,p);
        assertEquals(123,predicate.getStatusCode());
        assertTrue(predicate.getBodyPredicate().matches(e));
        assertEquals("baz",e.getProperty("foo"));

    }

    @Test
    public void testHttpExceptionPredicate() throws Exception {
        Predicate p = new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                exchange.setProperty("foo","baz");
                return true;
            }
        };

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        HttpErrorPredicate predicate = httpExceptionResponse(p);
        assertEquals(0,predicate.getStatusCode());
        assertTrue(predicate.getBodyPredicate().matches(e));
        assertEquals("baz",e.getProperty("foo"));
    }

    @Test
    public void testCSVMappingCreated() throws Exception {
        List<Map<String,String>> csv = csv(text("foo,baz\n1,2\n3,4\n5,6"));

        assertEquals(3,csv.size());
        assertEquals("1",csv.get(0).get("foo"));
        assertEquals("2",csv.get(0).get("baz"));

        assertEquals("3",csv.get(1).get("foo"));
        assertEquals("4",csv.get(1).get("baz"));

        assertEquals("5",csv.get(2).get("foo"));
        assertEquals("6",csv.get(2).get("baz"));
    }

    @Test
    public void testCSVDuplicateHeaders() throws Exception {
        IllegalArgumentException e = null;
        try {
            csv(text("foo,foo\n1,2\n3,4\n5,6"));
        } catch (IllegalArgumentException ex) {
            e = ex;
        }
        assertNotNull(e);
    }

    @Test
    public void testCSVTooManyValues() throws Exception {
        IllegalArgumentException e = null;
        try {
            csv(text("foo,baz\n1,2\n3,4,5\n6,7"));
        } catch (IllegalArgumentException ex) {
            e = ex;
        }
        assertNotNull(e);
    }

    @Test
    public void testDirRetrieval() throws Exception {
        PlainTextTestResource[] resources = text(dir("data/multidirtest/**/*.txt"));
        assertEquals(4,resources.length);
        assertEquals("a",resources[0].getValue());
        assertEquals("b",resources[1].getValue());
        assertEquals("c",resources[2].getValue());
        assertEquals("d",resources[3].getValue());
    }

    @Test
    public void testGroovyTemplateCompleted() throws Exception {
        List<Map<String,String>> csv = csv(text("foo,baz\n1,2\n3,4\n5,6"));
        PlainTextTestResource[] resources = text(groovy(text("value:${foo},value:${baz}"), csv)); 

        assertEquals(3,resources.length);
        assertTrue(text("value:1,value:2").validate(resources[0].getValue()));
        assertTrue(text("value:3,value:4").validate(resources[1].getValue()));
        assertTrue(text("value:5,value:6").validate(resources[2].getValue()));
    }

    @Test
    public void testXPathNoNamespaces() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("<foo><baz>moo</baz></foo>");

        assertTrue(xpath("/foo/baz/text() = 'moo'").matches(e));
    }

    @Test
    public void testXPathWithNamespaces() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("<ns0:foo xmlns:ns0='http://foo.com'><ns1:baz xmlns:ns1='http://baz.com'>moo</ns1:baz></ns0:foo>");

        assertTrue(xpath("/ns0:foo/ns1:baz/text() = 'moo'",namespace("ns0","http://foo.com"),
                namespace("ns1","http://baz.com")).matches(e));
    }

    @Test
    public void testNoMatchXPathWithNamespaces() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("<ns0:foo xmlns:ns0='http://foo.com'><ns1:baz xmlns:ns1='http://baz.com'>moo</ns1:baz></ns0:foo>");

        assertFalse(xpath("/ns0:foo/ns1:baz/text() = 'cow'", namespace("ns0", "http://foo.com"),
                namespace("ns1", "http://baz.com")).matches(e));
    }

    @Test
    public void testRegexMatch() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("aaabbbccc");

        assertTrue(regex("a{3}b{3}c{3}").matches(e));
    }

    @Test
    public void testNoRegexMatch() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("aabbcc");

        assertFalse(regex("a{3}b{3}c{3}").matches(e));
    }
}
