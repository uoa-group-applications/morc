package nz.ac.auckland.integration.testing.validator;

import nz.ac.auckland.integration.testing.resource.StaticTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.utility.XPathSelector;
import org.apache.camel.Exchange;
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
public class XmlValidator implements Validator {

    private Logger logger = LoggerFactory.getLogger(XmlValidator.class);
    private XmlTestResource resource;
    private XPathSelector xpathSelector;

    public XmlValidator(XmlTestResource resource) {
        this.resource = resource;
        XMLUnit.setIgnoreWhitespace(true);

    }

    public XmlValidator(XmlTestResource resource,XPathSelector xpathSelector) {
        this(resource);
        this.xpathSelector = xpathSelector;
    }

    /**
     * @param exchange The exchange containing the XML document to validate
     * @return true if the input and test resource are similar using XMLUnit's Diff.similar()
     */
    public boolean validate(Exchange exchange) {
        Document value = exchange.getIn().getBody(Document.class);
        if (value == null) return false;

        try {
            Document expectedValue;
            try {
                if (xpathSelector != null)
                    value = xpathSelector.evaluate(value);
                expectedValue = resource.getValue();
            } catch (XPathSelector.XPathEvaluationException e) {
                logger.warn("The XPath evaluation failed on the value for validation",e);
                return false;
            }

            DetailedDiff difference = new DetailedDiff(new Diff(expectedValue, value));
            if (!difference.similar()) {
                logger.warn("Differences exist between two documents: {}", difference.getAllDifferences());
            }
            logger.trace("No differences exist for input {}", value);
            return difference.similar();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
