package nz.ac.auckland.morc.tests;

import nz.ac.auckland.morc.MorcMethods;
import nz.ac.auckland.morc.mock.MockDefinition;
import nz.ac.auckland.morc.mock.builder.SyncMockDefinitionBuilder;
import nz.ac.auckland.morc.predicate.MultiPredicate;
import nz.ac.auckland.morc.resource.GroovyTemplateTestResource;
import nz.ac.auckland.morc.resource.TestResource;
import nz.ac.auckland.morc.utility.XmlUtilities;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MorcTestBuilderTest extends Assert implements MorcMethods {

    @Test
    public void testDelayProcessor() throws Exception {
        long time = new Date().getTime();
        delay(5000).process(null);
        long finishTime = new Date().getTime();

        assertTrue(finishTime >= time + 5000l);
    }

    @Test
    public void testMultiPredicate() throws Exception {
        Predicate predicate = multiPredicate(text("a"), headers(header("a", "b"), header("c", "d")));
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("a");
        Map<String,Object> headers = new HashMap<>();
        headers.put("a","b");
        headers.put("c","d");
        e.getIn().setHeaders(headers);
        assertTrue(predicate.matches(e));
        assertTrue(multiPredicate(headers(header("a", "b"), header("c", "d"))).matches(e));
    }

    @Test
    public void testCSVMappingCreated() throws Exception {
        List<Map<String, String>> csv = csv(text("foo,baz\n1,2\n3,4\n5,6"));

        assertEquals(3, csv.size());
        assertEquals("1", csv.get(0).get("foo"));
        assertEquals("2", csv.get(0).get("baz"));

        assertEquals("3", csv.get(1).get("foo"));
        assertEquals("4", csv.get(1).get("baz"));

        assertEquals("5", csv.get(2).get("foo"));
        assertEquals("6", csv.get(2).get("baz"));
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
    public void testTextDirRetrieval() throws Exception {
        TestResource[] resources = text(dir("data/multidirtest/**/*.txt"));
        assertEquals(4, resources.length);
        assertEquals("a", resources[0].getValue());
        assertEquals("b", resources[1].getValue());
        assertEquals("c", resources[2].getValue());
        assertEquals("d", resources[3].getValue());
    }

    @Test
    public void testXmlDirRetrieval() throws Exception {
        TestResource[] resources = xml(dir("data/xmlmultidirtest/*.xml"));
        assertEquals(2, resources.length);

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody(resources[0].getValue());

        assertTrue(xml("<foo/>").matches(e));

        e.getIn().setBody(resources[1].getValue());
        assertTrue(xml("<baz/>").matches(e));
    }

    @Test
    public void testJsonDirRetrieval() throws Exception {
        TestResource[] resources = json(dir("data/jsonmultidirtest/*.json"));
        assertEquals(2, resources.length);

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody(resources[0].getValue());

        assertTrue(json("{ \"foo\":\"baz\" }").matches(e));

        e.getIn().setBody(resources[1].getValue());
        assertTrue(json("{ \"baz\":\"moo\" }").matches(e));
    }

    @Test
    public void testGroovyTemplateCompleted() throws Exception {
        List<Map<String, String>> csv = csv(text("foo,baz\n1,2\n3,4\n5,6"));
        TestResource[] resources = text(groovy(text("value:${foo},value:${baz}"), csv));

        assertEquals(3, resources.length);
        assertTrue(text("value:1,value:2").validate((String) resources[0].getValue()));
        assertTrue(text("value:3,value:4").validate((String) resources[1].getValue()));
        assertTrue(text("value:5,value:6").validate((String) resources[2].getValue()));
    }

    @Test
    public void testXmlGroovyTemplateCompleted() throws Exception {
        List<Map<String, String>> csv = csv(text("foo,baz\n1,2\n3,4\n5,6"));
        XmlRuntimeTestResource[] resources = xml(groovy("<result><input>${foo}</input><output>${baz}</output></result>", csv));

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        resources[0].process(e);
        assertTrue(xml("<result><input>1</input><output>2</output></result>").matches(e));
        resources[1].process(e);
        assertTrue(xml("<result><input>3</input><output>4</output></result>").matches(e));
        resources[2].process(e);
        assertTrue(xml("<result><input>5</input><output>6</output></result>").matches(e));
    }

    @Test
    public void testJsonGroovyTemplateCompleted() throws Exception {
        List<Map<String, String>> csv = csv(text("foo,baz\n1,2\n3,4\n5,6"));
        JsonRuntimeTestResource[] resources = json(groovy("{ \"${foo}\":\"${baz}\" }", csv));

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        resources[0].process(e);
        assertTrue(json("{ \"1\":\"2\" }").matches(e));
        resources[1].process(e);
        assertTrue(json("{ \"3\":\"4\" }").matches(e));
        resources[2].process(e);
        assertTrue(json("{ \"5\":\"6\" }").matches(e));
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

        assertTrue(xpath("/ns0:foo/ns1:baz/text() = 'moo'", namespace("ns0", "http://foo.com"),
                namespace("ns1", "http://baz.com")).matches(e));
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

    @Test
    public void testGroovyDelayedEvaluation() throws Exception {
        List<Map<String, String>> csv = csv(text("foo,baz\n1,2\n3,4"));

        long startTime = new Date().getTime();
        Thread.sleep(2000);
        TestResource[] resources = text(groovy(text("${new Date().getTime()}"), csv));
        long nextTime = Long.parseLong((String) resources[0].getValue());
        assertTrue(nextTime > startTime);
        Thread.sleep(2000);
        long lastTime = Long.parseLong((String) resources[1].getValue());
        assertTrue(lastTime > nextTime);
    }

    @Test
    public void testSingleGroovyResource() throws Exception {
        TestResource<String> resource = groovy(text("$foo $baz $moo"), var("foo", "1"), var("baz", "2"), var("moo", "3"));
        assertTrue(resource.getValue().equals("1 2 3"));
    }

    @Test
    public void testSingleGroovyResourceStringTemplate() throws Exception {
        TestResource<String> resource = groovy("$foo $baz $moo", var("foo", "1"), var("baz", "2"), var("moo", "3"));
        assertTrue(resource.getValue().equals("1 2 3"));
    }

    @Test
    public void testXmlGroovyResource() throws Exception {
        XmlRuntimeTestResource[] resources = xml(groovy("<foo>$foo</foo>", var("foo", "baz")),
                groovy("<foo>$foo</foo>", var("foo", "abc")));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody(new XmlUtilities().getXmlAsDocument("<foo>baz</foo>"));
        assertTrue(resources[0].matches(e));
        e.getIn().setBody(new XmlUtilities().getXmlAsDocument("<foo>foo</foo>"));
        assertFalse(resources[0].matches(e));

        assertFalse(resources[1].matches(e));
        e.getIn().setBody(new XmlUtilities().getXmlAsDocument("<foo>abc</foo>"));
        assertTrue(resources[1].matches(e));

        resources[0].process(e);
        assertTrue(xml("<foo>baz</foo>").matches(e));

        resources[1].process(e);
        assertTrue(xml("<foo>abc</foo>").matches(e));
    }

    @Test
    public void testJsonGroovyResource() throws Exception {
        JsonRuntimeTestResource[] resources = json(groovy("{ \"foo\": \"$foo\" }", var("foo", "baz")),
                groovy("{ \"foo\": \"$foo\" }", var("foo", "abc")));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("{ \"foo\": \"baz\" }");
        assertTrue(resources[0].matches(e));
        e.getIn().setBody("{ \"foo\": \"foo\" }");
        assertFalse(resources[0].matches(e));

        assertFalse(resources[1].matches(e));
        e.getIn().setBody("{ \"foo\": \"abc\" }");
        assertTrue(resources[1].matches(e));

        resources[0].process(e);
        assertTrue(json("{ \"foo\": \"baz\" }").matches(e));

        resources[1].process(e);
        assertTrue(json("{ \"foo\": \"abc\" }").matches(e));
    }

    @Test
    public void testTextGroovyResource() throws Exception {
        GroovyTemplateTestResource[] resources = text(groovy("$foo $baz", var("foo", "baz"), var("baz", "foo")));
        assertTrue(resources[0].getValue().equals("baz foo"));
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("baz foo");
        assertTrue(resources[0].matches(e));
    }

    @Test
    public void testRandomGenerator() throws Exception {
        MockDefinition mock = new SyncMockDefinitionBuilder("foo").lenientProcessor(randomSelector())
                .lenient(exchange -> true)
                .response(text("foo")).response(text("baz")).response(text("moo")).build(null);

        int foo = 0, baz = 0, moo = 0;

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        for (int i = 0; i < 1000; i++) {
            mock.getLenientProcessor().process(e);
            String body = e.getIn().getBody(String.class);
            if (body.equals("foo")) foo++;
            else if (body.equals("baz")) baz++;
            else if (body.equals("moo")) moo++;
            else throw new Exception("Body not expected!");
        }

        assertTrue(foo > 0);
        assertTrue(baz > 0);
        assertTrue(moo > 0);
    }
}
