package nz.ac.auckland.integration.tests.resource;

import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.utility.XMLUtilities;
import nz.ac.auckland.integration.testing.utility.XPathSelector;
import nz.ac.auckland.integration.testing.validator.XmlValidator;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class XmlTestResourceTest extends Assert {

    public XmlTestResourceTest() {
        XMLUnit.setIgnoreWhitespace(true);
    }

    private static XMLUtilities xmlUtilities = new XMLUtilities();

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
        XmlValidator validator = new XmlValidator(new XmlTestResource(inputUrl));
        assertTrue(validator.validate(EXPECTED_VALUE));
    }

    @Test
    public void testCompareDifferentInput() throws Exception {
        XmlValidator validator = new XmlValidator(new XmlTestResource(inputUrl));
        assertFalse(validator.validate("<sample/>"));
    }

    @Test
    public void testNullInput() throws Exception {
        XmlValidator validator = new XmlValidator(new XmlTestResource(inputUrl));
        Document nullDoc = null;
        assertFalse(validator.validate(nullDoc));
    }

    @Test
    public void testEmptyFile() throws Exception {
        XmlValidator validator = new XmlValidator(new XmlTestResource(inputUrl2));
        assertTrue(validator.validate(""));
    }

    @Test
    public void testPassValueToConstructor() throws Exception {
        XmlValidator validator = new XmlValidator(new XmlTestResource(xmlUtilities.getXmlAsDocument("<foo/>")));
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
    public void testSimpleXPath() throws Exception {
        XPathSelector selector = new XPathSelector("/v1:isOfInterest/v1:entity", namespaceMap);
        XmlTestResource resource = new XmlTestResource(xmlUtilities.getXmlAsDocument("<v1:entity xmlns:v1=\"http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1\">HREmployee</v1:entity>"), selector);
        assertTrue(new XmlValidator(resource).validate(EXPECTED_VALUE));
    }

    @Test
    public void testWrongNodeXPath() throws Exception {
        XPathSelector selector = new XPathSelector("/v1:isOfInterest", namespaceMap);

        XmlTestResource resource = new XmlTestResource(xmlUtilities.getXmlAsDocument("<v1:entity xmlns:v1=\"http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1\">HREmployee</v1:entity>"), selector);

        assertFalse(new XmlValidator(resource).validate(EXPECTED_VALUE));
    }

    @Test
    public void testWrongNodeListXPath() throws Exception {

        String nodeListTest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<v1:isOfInterest xmlns:v1=\"http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1\">\n" +
                "\t<v1:entity>HREmployee</v1:entity>\n" +
                "\t<v1:entity>HREmployee1</v1:entity>\n" +
                "\t<v1:identifier name=\"uoaid\">2512472</v1:identifier>\n" +
                "</v1:isOfInterest>\n";

        XPathSelector selector = new XPathSelector("/v1:isOfInterest/v1:entity", namespaceMap);

        XmlTestResource resource = new XmlTestResource(xmlUtilities.getXmlAsDocument("<v1:entity xmlns:v1=\"http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1\">HREmployee</v1:entity>"), selector);

        Exception e = null;
        try {
            new XmlValidator(resource).validate(nodeListTest);
        } catch (RuntimeException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testWrongNodeValueXPath() throws Exception {
        XPathSelector selector = new XPathSelector("/v1:isOfInterest/v1:entity/text()", namespaceMap);

        XmlTestResource resource = new XmlTestResource(xmlUtilities.getXmlAsDocument("<v1:entity xmlns:v1=\"http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1\">HREmployee</v1:entity>"), selector);

        Exception e = null;

        try {
            new XmlValidator(resource).validate(EXPECTED_VALUE);
        } catch (RuntimeException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testNoXPathMatch() throws Exception {
        XPathSelector selector = new XPathSelector("/v1:isOfInterest/v1:nosuchelement", namespaceMap);

        XmlTestResource resource = new XmlTestResource(xmlUtilities.getXmlAsDocument("<v1:entity xmlns:v1=\"http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1\">HREmployee</v1:entity>"), selector);

        assertFalse(new XmlValidator(resource).validate(EXPECTED_VALUE));
    }

    @Test
    public void testInvalidXPathFoundValue() throws Exception {
        XPathSelector selector = new XPathSelector("/v1:isOfInterest/v1:entity", namespaceMap);

        XmlTestResource resource = new XmlTestResource(xmlUtilities.getXmlAsDocument("<v1:entity xmlns:v1=\"http://www.auckland.ac.nz/domain/application/wsdl/isofinterest/v1\">nosuchvalue</v1:entity>"), selector);

        assertFalse(new XmlValidator(resource).validate(EXPECTED_VALUE));
    }

    @Test
    public void testValidatorInputException() throws Exception {
        throw new Exception("todo");
    }

    @Test
    public void testExchangeInput() throws Exception {
        throw new Exception("todo");
    }
}

