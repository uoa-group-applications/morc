package nz.ac.auckland.morc.tests.resource;

import nz.ac.auckland.morc.resource.XmlTestResource;
import nz.ac.auckland.morc.utility.XmlUtilities;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class XmlTestResourceTest extends Assert {

    public XmlTestResourceTest() {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalizeWhitespace(true);
        XMLUnit.setIgnoreComments(true);
    }

    private static XmlUtilities xmlUtilities = new XmlUtilities();

    private static Map<String, String> namespaceMap = new HashMap<>();
    private static final Document EXPECTED_VALUE = xmlUtilities.getXmlAsDocument("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<v1:isOfInterest xmlns:v1=\"http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1\">\n" +
            "\t<v1:entity>HREmployee</v1:entity>\n" +
            "\t<v1:identifier name=\"uoaid\">2512472</v1:identifier>\n" +
            "</v1:isOfInterest>\n");

    URL inputUrl = this.getClass().getResource("/data/xml-test1.xml");
    URL inputUrl2 = this.getClass().getResource("/data/xml-test2.xml");

    static {
        namespaceMap.put("v1", "http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1");
    }

    @Test
    public void testReadFileFromClasspath() throws Exception {
        XmlTestResource resource = new XmlTestResource(inputUrl);

        Document actualValue = resource.getValue();

        Diff diff = new Diff(EXPECTED_VALUE, actualValue);
        assertTrue(diff.similar());
    }

    @Test
    public void testCompareInput() throws Exception {
        XmlTestResource validator = new XmlTestResource(inputUrl);
        assertTrue(validator.validate(EXPECTED_VALUE));
    }

    @Test
    public void testCompareDifferentInput() throws Exception {
        XmlTestResource validator = new XmlTestResource(inputUrl);
        assertFalse(validator.validate("<sample/>"));
    }

    @Test
    public void testNullInput() throws Exception {
        XmlTestResource validator = new XmlTestResource(inputUrl);
        Document nullDoc = null;
        assertFalse(validator.validate(nullDoc));
    }

    @Test
    public void testInvalidXmlInput() throws Exception {
        XmlTestResource validator = new XmlTestResource(inputUrl2);
        assertFalse(validator.validate(""));
    }

    @Test
    public void testPassValueToConstructor() throws Exception {
        XmlTestResource validator = new XmlTestResource(xmlUtilities.getXmlAsDocument("<foo/>"));
        assertTrue(validator.validate("<foo/>"));
    }

    @Test
    public void testNoSuchFile() throws Exception {
        Exception e = null;

        try {
            XmlTestResource resource = new XmlTestResource(this.getClass().getResource("/nosuchfile.xml"));
        } catch (RuntimeException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testXMLUtilities() throws Exception {
        XmlTestResource resource = new XmlTestResource(inputUrl);
        resource.setXmlUtilities(new FakeXmlUtilities());
        Document d = null;
        assertEquals("test", resource.getXmlUtilities().getDocumentAsString(d));
    }

    static class FakeXmlUtilities extends XmlUtilities {
        @Override
        public String getDocumentAsString(Document doc) {
            return "test";
        }
    }

    @Test
    public void testMissingFile() throws Exception {

        Throwable e = null;
        try {
            XmlTestResource resource = new XmlTestResource(new File("broken"));
        } catch (RuntimeException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testXmlUtilitiesGetterSetter() throws Exception {
        XmlTestResource validator = new XmlTestResource(inputUrl);
        assertNotNull(validator.getXmlUtilities());
        validator.setXmlUtilities(null);
        assertNull(validator.getXmlUtilities());
    }

    @Test
    public void testEmptyExchange() throws Exception {
        assertFalse(new XmlTestResource(inputUrl).matches(new DefaultExchange(new DefaultCamelContext())));
    }

    @Test
    public void testTrimmedToString() throws Exception {
        assertEquals(100, new XmlTestResource(EXPECTED_VALUE).toString().length());
    }

}

