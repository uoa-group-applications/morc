package nz.ac.auckland.integration.testing.validator;

import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.utility.XMLUtilities;
import nz.ac.auckland.integration.testing.utility.XPathSelector;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

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
    private XMLUtilities xmlUtilities = new XMLUtilities();

    public XmlValidator(XmlTestResource resource) {
        this.resource = resource;
        XMLUnit.setIgnoreWhitespace(true);

    }

    public XmlValidator(XmlTestResource resource,XPathSelector xpathSelector) {
        this(resource);
        this.xpathSelector = xpathSelector;
    }

    public XMLUtilities getXmlUtilities() {
        return xmlUtilities;
    }

    /**
     * If you have some special requirements for XML parsing
     */
    public void setXmlUtilities(XMLUtilities xmlUtilities) {
        this.xmlUtilities = xmlUtilities;
    }

    /**
     * @param exchange The exchange containing the XML document to validate
     * @return true if the input and test resource are similar using XMLUnit's Diff.similar()
     */
    public boolean validate(Exchange exchange) {
        Document value;
        try {
            value = exchange.getIn().getBody(Document.class);
        } catch (TypeConversionException e) {
            logger.warn("Error attempting to convert JSON to a Document",e);
            return false;
        }
        return value != null && validate(value);
    }

    public boolean validate(String value) {
        return validate(xmlUtilities.getXmlAsDocument(value));
    }

    public boolean validate(Document value) {
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
            } else
                logger.trace("No differences exist for input {}", xmlUtilities.getDocumentAsString(value));
            return difference.similar();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
