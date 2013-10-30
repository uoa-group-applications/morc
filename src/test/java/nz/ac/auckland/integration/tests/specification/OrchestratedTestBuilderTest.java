package nz.ac.auckland.integration.tests.specification;

import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
import nz.ac.auckland.integration.testing.expectation.*;
import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.JsonTestResource;
import nz.ac.auckland.integration.testing.resource.PlainTextTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.specification.AsyncOrchestratedTestSpecification;
import nz.ac.auckland.integration.testing.specification.SyncOrchestratedTestSpecification;
import nz.ac.auckland.integration.testing.utility.XMLUtilities;
import nz.ac.auckland.integration.testing.validator.JsonValidator;
import nz.ac.auckland.integration.testing.validator.PlainTextValidator;
import nz.ac.auckland.integration.testing.validator.XmlValidator;
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

}
