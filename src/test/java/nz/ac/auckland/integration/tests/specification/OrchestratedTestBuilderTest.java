package nz.ac.auckland.integration.tests.specification;

import nz.ac.auckland.integration.testing.OrchestratedTest;
import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
import nz.ac.auckland.integration.testing.expectation.*;
import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.JsonTestResource;
import nz.ac.auckland.integration.testing.resource.PlainTextTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.specification.AsyncOrchestratedTestSpecification;
import nz.ac.auckland.integration.testing.specification.SyncOrchestratedTestSpecification;
import nz.ac.auckland.integration.testing.utility.XMLUtilities;
import nz.ac.auckland.integration.testing.utility.XPathSelector;
import nz.ac.auckland.integration.testing.validator.*;
import org.apache.camel.Exchange;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpOperationFailedException;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class OrchestratedTestBuilderTest extends Assert {

    private static final String EXPECTED_XML_VALUE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<v1:isOfInterest xmlns:v1=\"http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1\">\n" +
            "\t<v1:entity>HREmployee</v1:entity>\n" +
            "\t<v1:identifier name=\"uoaid\">2512472</v1:identifier>\n" +
            "</v1:isOfInterest>\n";

    private static final String EXPECTED_JSON_VALUE = "{\n" +
            "    \"firstName\":\"foo\",\n" +
            "    \"lastName\":\"baz\",\n" +
            "    \"address\": {\n" +
            "        \"street\":\"foostreet\",\n" +
            "        \"city\":\"baz\"\n" +
            "    },\n" +
            "    \"phone\": [\n" +
            "        {\n" +
            "            \"type\":\"home\",\n" +
            "            \"number\":1234\n" +
            "        },\n" +
            "        {\n" +
            "            \"type\":\"work\",\n" +
            "            \"number\":4321\n" +
            "        }\n" +
            "    ]\n" +
            "}";


    @Test
    public void testAsyncTest() {
        AsyncOrchestratedTestSpecification.Builder builder = new AsyncOrchestratedTestSpecification.Builder("endpointUri", "description")
                .addExpectation(OrchestratedTestBuilder.exceptionExpectation("foo"));
        AsyncOrchestratedTestSpecification spec = builder.build();

        assertEquals(spec.getDescription(), "description");
        assertEquals(spec.getTargetServiceUri(), "endpointUri");
    }

    @Test
    public void testSyncTest() {
        SyncOrchestratedTestSpecification.Builder builder = new SyncOrchestratedTestSpecification.Builder("endpointUri", "description");
        SyncOrchestratedTestSpecification spec = builder.build();

        assertEquals(spec.getDescription(), "description");
        assertEquals(spec.getTargetServiceUri(), "endpointUri");
    }

    @Test
    public void testXmlString() throws Exception {
        XmlTestResource xml = OrchestratedTestBuilder.xml("<foo/>");
        DetailedDiff difference = new DetailedDiff(new Diff("<foo/>", new XMLUtilities().getDocumentAsString(xml.getValue())));
        assertTrue(difference.similar());
    }

    @Test
    public void testXmlFile() throws Exception {
        File f = new File(this.getClass().getResource("/data/xml-test1.xml").toURI());
        XmlTestResource xml = OrchestratedTestBuilder.xml(f);
        assertTrue(new XmlValidator(xml).validate(EXPECTED_XML_VALUE));
    }

    @Test
    public void testXmlURL() throws Exception {
        XmlTestResource xml = OrchestratedTestBuilder.xml(this.getClass().getResource("/data/xml-test1.xml"));
        assertTrue(new XmlValidator(xml).validate(EXPECTED_XML_VALUE));
    }

    @Test
    public void testJsonString() throws Exception {
        JsonTestResource json = OrchestratedTestBuilder.json("{foo:\"foo\"}");
        assertEquals("{foo:\"foo\"}", json.getValue());
    }

    @Test
    public void testJsonFile() throws Exception {
        File f = new File(this.getClass().getResource("/data/json-test1.json").toURI());
        JsonTestResource json = OrchestratedTestBuilder.json(f);
        assertTrue(new JsonValidator(json).validate(EXPECTED_JSON_VALUE));
    }

    @Test
    public void testJsonURL() throws Exception {
        JsonTestResource json = OrchestratedTestBuilder.json(this.getClass().getResource("/data/json-test1.json"));
        assertTrue(new JsonValidator(json).validate(EXPECTED_JSON_VALUE));
    }

    @Test
    public void testPlainTextString() throws Exception {
        PlainTextTestResource text = OrchestratedTestBuilder.text("foo");
        assertEquals("foo", text.getValue());
    }

    @Test
    public void testPlainTextFile() throws Exception {
        File f = new File(this.getClass().getResource("/data/plaintext-test1.txt").toURI());
        PlainTextTestResource text = OrchestratedTestBuilder.text(f);
        assertTrue(new PlainTextValidator(text).validate("test"));
    }

    @Test
    public void testPlainTextURL() throws Exception {
        PlainTextTestResource text = OrchestratedTestBuilder.text(this.getClass().getResource("/data/plaintext-test1.txt"));
        assertTrue(new PlainTextValidator(text).validate("test"));
    }

    @Test
    public void testMapHeaders() throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put("foo", "baz");

        HeadersTestResource headersTest = OrchestratedTestBuilder.headers(headers);

        assertEquals(1, headersTest.getValue().size());
        assertEquals("baz", headersTest.getValue().get("foo"));
    }

    @Test
    public void testHeadersFile() throws Exception {
        File f = new File(this.getClass().getResource("/data/header-test1.properties").toURI());
        HeadersTestResource headers = OrchestratedTestBuilder.headers(f);

        assertEquals(2, headers.getValue().size());
        assertEquals("baz", headers.getValue().get("foo"));
        assertEquals("123", headers.getValue().get("abc"));
    }

    @Test
    public void testHeadersURL() throws Exception {
        HeadersTestResource headers = OrchestratedTestBuilder.headers(this.getClass().getResource("/data/header-test1.properties"));

        assertEquals(2, headers.getValue().size());
        assertEquals("baz", headers.getValue().get("foo"));
        assertEquals("123", headers.getValue().get("abc"));
    }

    @Test
    public void testHeadersHeaderValue() throws Exception {
        HeadersTestResource headers = OrchestratedTestBuilder.headers(
                OrchestratedTestBuilder.header("foo", "baz"), OrchestratedTestBuilder.header("abc", "123"));

        assertEquals(2, headers.getValue().size());
        assertEquals("baz", headers.getValue().get("foo"));
        assertEquals("123", headers.getValue().get("abc"));
    }

    @Test
    public void testClasspath() throws Exception {
        URL classpath = OrchestratedTestBuilder.classpath("/data/header-test1.properties");
        assertTrue(classpath.getPath().contains("/data/header-test1.properties"));
    }

    @Test
    public void testFile() throws Exception {
        File file = OrchestratedTestBuilder.file("/data/header-test1.properties");
        assertEquals(file.getPath(), "/data/header-test1.properties");
    }

    @Test
    public void testAsyncExpectation() throws Exception {
        AsyncMockExpectation.Builder builder = OrchestratedTestBuilder.asyncExpectation("foo");
        assertEquals("foo", builder.build().getEndpointUri());
    }

    @Test
    public void testSyncExpectation() throws Exception {
        SyncMockExpectation.Builder builder = OrchestratedTestBuilder.syncExpectation("foo");
        assertEquals("foo", builder.build().getEndpointUri());
    }


    @Test
    public void testCxfFaultExpectation() throws Exception {
        HttpErrorMockExpectation.Builder builder = OrchestratedTestBuilder.wsFaultExpectation("foo");
        assertEquals("foo", builder.build().getEndpointUri());
    }

    @Test
    public void testExceptionExpectation() throws Exception {
        ExceptionMockExpectation.Builder builder = OrchestratedTestBuilder.exceptionExpectation("foo");
        assertEquals("foo", builder.build().getEndpointUri());
    }

    @Test
    public void testUnreceivedExpectation() throws Exception {
        UnreceivedMockExpectation.Builder builder = OrchestratedTestBuilder.unreceivedExpectation("foo");
        assertEquals("foo", builder.build().getEndpointUri());
    }

    @Test
    public void testNonExistentResource() throws Exception {
        Exception e = null;
        try {
            OrchestratedTestBuilder.classpath("/nosuchfile.xml");
        } catch (RuntimeException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testHttpExceptionNoValidator() throws Exception {
        assertTrue(OrchestratedTestBuilder.httpException() instanceof HttpExceptionValidator.Builder);
    }

    @Test
    public void testHttpExceptionStatusCode() throws Exception {
        assertEquals(OrchestratedTestBuilder.httpException(500).getStatusCode(),500);
    }

    @Test
    public void testHttpExceptionValidator() throws Exception {
        HttpExceptionValidator validator = OrchestratedTestBuilder.httpException(new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return true;
            }
        });

        CamelContext context = new DefaultCamelContext();
        Exchange e = new DefaultExchange(context);
        e.setException(new HttpOperationFailedException(null,0,null,null,null,null));

        assertTrue(validator.validate(e));
    }

    @Test
    public void testHttpExceptionXmlResource() throws Exception {
        XMLUtilities xmlUtilities = new XMLUtilities();

        HttpExceptionValidator validator = OrchestratedTestBuilder.httpException(
                new XmlTestResource(xmlUtilities.getXmlAsDocument("<foo/>")));

        CamelContext context = new DefaultCamelContext();
        Exchange e = new DefaultExchange(context);
        e.setException(new HttpOperationFailedException(null,0,null,null,null,"{\"foo\":\"baz\"}"));

        validator.validate(e);
    }

    @Test
    public void testHttpExceptionJsonResource() throws Exception {
        HttpExceptionValidator validator = OrchestratedTestBuilder.httpException(new JsonTestResource("{\"foo\":\"baz\"}"));

        CamelContext context = new DefaultCamelContext();
        Exchange e = new DefaultExchange(context);
        e.setException(new HttpOperationFailedException(null,0,null,null,null,"{\"foo\":\"baz\"}"));

        validator.validate(e);
    }

    @Test
    public void testHttpExceptionPlainTextResource() throws Exception {

        HttpExceptionValidator validator = OrchestratedTestBuilder.httpException(new PlainTextTestResource("foo"));

        CamelContext context = new DefaultCamelContext();
        Exchange e = new DefaultExchange(context);
        e.setException(new HttpOperationFailedException(null,0,null,null,null,"foo"));

        validator.validate(e);
    }

    @Test
    public void testCreateNSObject() throws Exception {
        OrchestratedTestBuilder.NS ns = new OrchestratedTestBuilder.NS("foo","baz");
        assertEquals("foo", ns.getPrefix());
        assertEquals("baz",ns.getUri());
    }

    @Test
    public void testCreateNSObjectMethod() throws Exception {
        OrchestratedTestBuilder.NS ns = OrchestratedTestBuilder.namespace("foo", "baz");
        assertEquals("foo",ns.getPrefix());
        assertEquals("baz",ns.getUri());
    }

    @Test
    public void testCreateXPathSelector() throws Exception {
        XMLUtilities xmlUtilities = new XMLUtilities();

        XmlTestResource resource = new XmlTestResource(
                xmlUtilities.getXmlAsDocument("<v2:entity xmlns:v2=\"http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v2\">HREmployee</v2:entity>"));

        Document input = xmlUtilities.getXmlAsDocument("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<v1:isOfInterest xmlns:v1=\"http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1\"" +
                        " xmlns:v2=\"http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v2\">\n" +
                    "\t<v2:entity >HREmployee</v2:entity>\n" +
                    "\t<v2:identifier name=\"uoaid\">2512472</v2:identifier>\n" +
                    "</v1:isOfInterest>\n");

        XPathSelector selector = OrchestratedTestBuilder.xpathSelector("/v1:isOfInterest/v2:entity",
                OrchestratedTestBuilder.namespace("v1","http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1"),
                OrchestratedTestBuilder.namespace("v2","http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v2"));

        XmlValidator validator = new XmlValidator(resource,selector);
        assertTrue(validator.validate(input));
    }

    @Test
    public void testCreateXMLStringXpath() throws Exception {
        XPathSelector selector = OrchestratedTestBuilder.xpathSelector("/v1:isOfInterest/v2:entity",
                OrchestratedTestBuilder.namespace("v1","http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1"),
                OrchestratedTestBuilder.namespace("v2","http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v2"));

        String input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<v1:isOfInterest xmlns:v1=\"http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1\"" +
                                " xmlns:v2=\"http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v2\">\n" +
                            "\t<v2:entity >HREmployee</v2:entity>\n" +
                            "\t<v2:identifier name=\"uoaid\">2512472</v2:identifier>\n" +
                            "</v1:isOfInterest>\n";

        XmlTestResource xml = OrchestratedTestBuilder.xml(input,selector);
        DetailedDiff difference = new DetailedDiff(new Diff("<v2:entity xmlns:v2=\"http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v2\">HREmployee</v2:entity>",
                new XMLUtilities().getDocumentAsString(xml.getValue())));
        assertTrue(difference.similar());
    }

    @Test
    public void testCreateXMLFileXPath() throws Exception {
        XPathSelector selector = OrchestratedTestBuilder.xpathSelector("/v1:isOfInterest/v2:entity",
                OrchestratedTestBuilder.namespace("v1","http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1"),
                OrchestratedTestBuilder.namespace("v2","http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v2"));
        XmlTestResource xml = OrchestratedTestBuilder.xml(new File("something"),selector);
        assertNotNull(xml.getXpathSelector());
    }

    @Test
    public void testCreateXMLURLXPath() throws Exception {
        XPathSelector selector = OrchestratedTestBuilder.xpathSelector("/v1:isOfInterest/v2:entity",
                OrchestratedTestBuilder.namespace("v1","http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1"),
                OrchestratedTestBuilder.namespace("v2","http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v2"));
        XmlTestResource xml = OrchestratedTestBuilder.xml(this.getClass().getResource("/data/xml-test1.xml"),selector);
        assertNotNull(xml.getXpathSelector());
    }

    @Test
    public void testSpringContextConstructor() throws Exception {

    }

    @Test
    public void testPropertiesLocationConstructor() throws Exception {

    }

}
