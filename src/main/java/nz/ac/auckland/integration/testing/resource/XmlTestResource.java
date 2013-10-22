package nz.ac.auckland.integration.testing.resource;

import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Map;

/**
 * Provides a mechanism for retrieving XML values from a file/URL/String and also
 * validating a response from a target service.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class XmlTestResource extends TestResource<String> {

    static {
        XMLUnit.setIgnoreWhitespace(true);
    }

    /**
     * Use this for limiting parts of the input for validation (i.e. you might
     * only care about the SOAP body if the header has a timestamp in it)
     */
    public static class XPathSelector {
        String xpathStatement;
        Map<String, String> namespaces;

        /**
         * @param xpathStatement a statement that must return a single element for comparison
         * @param namespaces     a collection of prefix:namespace pairs for evaluating the statement
         */
        public XPathSelector(String xpathStatement, Map<String, String> namespaces) {
            this.namespaces = namespaces;
            this.xpathStatement = xpathStatement;
        }

        /**
         * @param xpathStatement a statement that must return a single element for comparison
         */
        public XPathSelector(String xpathStatement) {
            this.xpathStatement = xpathStatement;
        }
    }

    public XmlTestResource(String value) {
        super(value);
    }

    public XmlTestResource(File file) {
        super(file);
    }

    public XmlTestResource(URL file) {
        super(file);
    }

    public XmlTestResource(String value, XPathSelector xpathSelector) {
        super(value);
        this.xpathSelector = xpathSelector;
    }

    public XmlTestResource(File file, XPathSelector xpathSelector) {
        super(file);
        this.xpathSelector = xpathSelector;
    }

    public XmlTestResource(URL file, XPathSelector xpathSelector) {
        super(file);
        this.xpathSelector = xpathSelector;
    }

    private Logger logger = LoggerFactory.getLogger(XmlTestResource.class);
    private XPathSelector xpathSelector = null;

    /**
     *
     * @param value A value to compare to this XML resource
     * @return true if the input and test resource are similar using XMLUnit's Diff.similar()
     */
    public boolean validate(String value) {

        if (value == null) return false;


        try {
            String expectedInput = getValue();
            if (value.isEmpty() || expectedInput.isEmpty()) return value.isEmpty() && expectedInput.isEmpty();

            //get the sub document matching the xpath
            if (xpathSelector != null) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setValidating(false);
                dbf.setNamespaceAware(true);
                DocumentBuilder db = dbf.newDocumentBuilder();

                Document inputDoc = db.parse(new ByteArrayInputStream(value.getBytes("UTF-8")));
                XPathFactory xpathFactory = XPathFactory.newInstance();
                XPath xpath = xpathFactory.newXPath();
                if (xpathSelector.namespaces != null) {
                    SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
                    nsContext.setBindings(xpathSelector.namespaces);
                    xpath.setNamespaceContext(nsContext);
                }

                XPathExpression xpathExpr = xpath.compile(xpathSelector.xpathStatement);
                NodeList result = (NodeList) xpathExpr.evaluate(inputDoc, XPathConstants.NODESET);
                if (result.getLength() > 1) throw new IllegalStateException("The xpath should return a single element");
                if (result.getLength() == 0) return false; //no match found
                if (!(result.item(0) instanceof Element))
                    throw new IllegalStateException("The xpath for an XML test resource must return an element");

                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                StringWriter sw = new StringWriter();
                transformer.transform(new DOMSource(result.item(0)), new StreamResult(sw));

                value = sw.toString();
            }

            DetailedDiff difference = new DetailedDiff(new Diff(expectedInput, value));
            if (!difference.similar()) {
                logger.warn("Differences exist between two documents: {}", difference.getAllDifferences());
            }
            logger.trace("No differences exist for input {}", value);
            return difference.similar();
        } catch (IOException | SAXException | ParserConfigurationException |
                XPathExpressionException | TransformerException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * @return The XML document from the resource as a String
     * @throws IOException
     */
    protected String getResource(File file) throws IOException {
        return FileUtils.readFileToString(file);
    }
}
