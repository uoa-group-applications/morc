package nz.ac.auckland.integration.tests.dsl;

import nz.ac.auckland.integration.testing.dsl.SpecificationBuilderHelper;
import nz.ac.auckland.integration.testing.expectation.*;
import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.JsonTestResource;
import nz.ac.auckland.integration.testing.resource.PlainTextTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.specification.AsyncOrchestratedTestSpecification;
import nz.ac.auckland.integration.testing.specification.SyncOrchestratedTestSpecification;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SpecificationBuilderHelperTest extends Assert {

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
        AsyncOrchestratedTestSpecification.Builder builder = SpecificationBuilderHelper
                .asyncTest("endpointUri", "description")
                .addExpectation(SpecificationBuilderHelper.exceptionExpectation("foo"));
        AsyncOrchestratedTestSpecification spec = builder.build();

        assertEquals(spec.getDescription(), "description");
        assertEquals(spec.getTargetServiceUri(), "endpointUri");
    }

    @Test
    public void testSyncTest() {
        SyncOrchestratedTestSpecification.Builder builder = SpecificationBuilderHelper.syncTest("endpointUri", "description");
        SyncOrchestratedTestSpecification spec = builder.build();

        assertEquals(spec.getDescription(), "description");
        assertEquals(spec.getTargetServiceUri(), "endpointUri");
    }

    @Test
    public void testXmlString() throws Exception {
        XmlTestResource xml = SpecificationBuilderHelper.xml("<foo/>");
        assertEquals("<foo/>", xml.getValue());
    }

    @Test
    public void testXmlFile() throws Exception {
        File f = new File(this.getClass().getResource("/data/xml-test1.xml").toURI());
        XmlTestResource xml = SpecificationBuilderHelper.xml(f);
        assertTrue(xml.validateInput(EXPECTED_XML_VALUE));
    }

    @Test
    public void testXmlURL() throws Exception {
        XmlTestResource xml = SpecificationBuilderHelper.xml(this.getClass().getResource("/data/xml-test1.xml"));
        assertTrue(xml.validateInput(EXPECTED_XML_VALUE));
    }

    @Test
    public void testJsonString() throws Exception {
        JsonTestResource json = SpecificationBuilderHelper.json("{foo:\"foo\"}");
        assertEquals("{foo:\"foo\"}", json.getValue());
    }

    @Test
    public void testJsonFile() throws Exception {
        File f = new File(this.getClass().getResource("/data/json-test1.json").toURI());
        JsonTestResource json = SpecificationBuilderHelper.json(f);
        assertTrue(json.validateInput(EXPECTED_JSON_VALUE));
    }

    @Test
    public void testJsonURL() throws Exception {
        JsonTestResource json = SpecificationBuilderHelper.json(this.getClass().getResource("/data/json-test1.json"));
        assertTrue(json.validateInput(EXPECTED_JSON_VALUE));
    }

    @Test
    public void testPlainTextString() throws Exception {
        PlainTextTestResource text = SpecificationBuilderHelper.text("foo");
        assertEquals("foo", text.getValue());
    }

    @Test
    public void testPlainTextFile() throws Exception {
        File f = new File(this.getClass().getResource("/data/plaintext-test1.txt").toURI());
        PlainTextTestResource text = SpecificationBuilderHelper.text(f);
        assertTrue(text.validateInput("test"));
    }

    @Test
    public void testPlainTextURL() throws Exception {
        PlainTextTestResource text = SpecificationBuilderHelper.text(this.getClass().getResource("/data/plaintext-test1.txt"));
        assertTrue(text.validateInput("test"));
    }

    @Test
    public void testMapHeaders() throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put("foo", "baz");

        HeadersTestResource headersTest = SpecificationBuilderHelper.headers(headers);

        assertEquals(1, headersTest.getValue().size());
        assertEquals("baz", headersTest.getValue().get("foo"));
    }

    @Test
    public void testHeadersFile() throws Exception {
        File f = new File(this.getClass().getResource("/data/header-test1.properties").toURI());
        HeadersTestResource headers = SpecificationBuilderHelper.headers(f);

        assertEquals(2, headers.getValue().size());
        assertEquals("baz", headers.getValue().get("foo"));
        assertEquals("123", headers.getValue().get("abc"));
    }

    @Test
    public void testHeadersURL() throws Exception {
        HeadersTestResource headers = SpecificationBuilderHelper.headers(this.getClass().getResource("/data/header-test1.properties"));

        assertEquals(2, headers.getValue().size());
        assertEquals("baz", headers.getValue().get("foo"));
        assertEquals("123", headers.getValue().get("abc"));
    }

    @Test
    public void testHeadersHeaderValue() throws Exception {
        HeadersTestResource headers = SpecificationBuilderHelper.headers(
                SpecificationBuilderHelper.headervalue("foo", "baz"), SpecificationBuilderHelper.headervalue("abc", "123"));

        assertEquals(2, headers.getValue().size());
        assertEquals("baz", headers.getValue().get("foo"));
        assertEquals("123", headers.getValue().get("abc"));
    }

    @Test
    public void testClasspath() throws Exception {
        URL classpath = SpecificationBuilderHelper.classpath("/data/header-test1.properties");
        assertTrue(classpath.getPath().contains("/data/header-test1.properties"));
    }

    @Test
    public void testFile() throws Exception {
        File file = SpecificationBuilderHelper.file("/data/header-test1.properties");
        assertEquals(file.getPath(), "/data/header-test1.properties");
    }

    @Test
    public void testAsyncExpectation() throws Exception {
        AsyncMockExpectation.Builder builder = SpecificationBuilderHelper.asyncExpectation("foo");
        assertEquals("foo", builder.build().getEndpointUri());
    }

    @Test
    public void testSyncExpectation() throws Exception {
        SyncMockExpectation.Builder builder = SpecificationBuilderHelper.syncExpectation("foo");
        assertEquals("foo", builder.build().getEndpointUri());
    }


    @Test
    public void testCxfFaultExpectation() throws Exception {
        WsFaultMockExpectation.Builder builder = SpecificationBuilderHelper.wsFaultExpectation("foo");
        assertEquals("foo", builder.build().getEndpointUri());
    }

    @Test
    public void testExceptionExpectation() throws Exception {
        ExceptionMockExpectation.Builder builder = SpecificationBuilderHelper.exceptionExpectation("foo");
        assertEquals("foo", builder.build().getEndpointUri());
    }

    @Test
    public void testUnreceivedExpectation() throws Exception {
        UnreceivedMockExpectation.Builder builder = SpecificationBuilderHelper.unreceivedExpectation("foo");
        assertEquals("foo", builder.build().getEndpointUri());
    }

    @Test
    public void testNonExistentResource() throws Exception {
        Exception e = null;
        try {
            SpecificationBuilderHelper.classpath("/nosuchfile.xml");
        } catch (RuntimeException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

}
