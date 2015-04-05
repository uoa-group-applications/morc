package nz.ac.auckland.morc.resource;

import nz.ac.auckland.morc.utility.XmlUtilities;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * Provides a mechanism for retrieving XML values from a file/URL/String and also
 * validating a response from a target service.
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class XmlTestResource extends StaticTestResource<Document> {

    private static final Logger logger = LoggerFactory.getLogger(XmlTestResource.class);
    private XmlUtilities xmlUtilities = new XmlUtilities();

    public XmlTestResource(Document value) {
        super(value);
    }

    public XmlTestResource(Document value, XmlUtilities xmlUtilities) {
        super(value);
        this.xmlUtilities = xmlUtilities;
    }

    public XmlTestResource(File file) {
        super(file);
    }

    public XmlTestResource(File file, XmlUtilities xmlUtilities) {
        super(file);
        this.xmlUtilities = xmlUtilities;
    }

    public XmlTestResource(URL url) {
        super(url);
    }

    public XmlTestResource(URL url, XmlUtilities xmlUtilities) {
        super(url);
        this.xmlUtilities = xmlUtilities;
    }

    public XmlTestResource(InputStream stream) {
        super(stream);
    }

    public XmlTestResource(InputStream stream, XmlUtilities xmlUtilities) {
        super(stream);
        this.xmlUtilities = xmlUtilities;
    }

    public XmlUtilities getXmlUtilities() {
        return xmlUtilities;
    }

    /**
     * In case you have any special XML requirements
     */
    public void setXmlUtilities(XmlUtilities xmlUtilities) {
        this.xmlUtilities = xmlUtilities;
    }

    /**
     * @return The XML document from the resource as a String
     * @throws Exception XPathEvaluationException thrown if the xpath could not be evaluated correctly
     */
    protected Document getResource(InputStream stream) throws Exception {
        return xmlUtilities.getXmlAsDocument(stream);
    }

    /**
     * @return The test resource in the appropriate format
     * @throws java.io.IOException
     */
    @Override
    public Document getValue() throws Exception {
        return super.getValue();
    }

    /**
     * @param exchange The exchange containing the XML document to validate
     * @return true if the input and test resource are similar using XMLUnit's Diff.similar()
     */
    public synchronized boolean matches(Exchange exchange) {
        Document value;
        try {
            value = exchange.getIn().getBody(Document.class);
        } catch (TypeConversionException e) {
            logger.warn("Error attempting to convert XML to a Document", e);
            return false;
        }
        return value != null && validate(value);
    }

    public boolean validate(String value) {
        Document doc;
        try {
            doc = xmlUtilities.getXmlAsDocument(value);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof SAXParseException) {
                logger.warn("Unable to parse the input value for validation", e);
                return false;
            }
            throw e;
        }
        return validate(doc);
    }

    public boolean validate(Document value) {
        if (value == null) return false;
        try {
            Document expectedValue = getValue();

            logger.debug("Expected XML Value: {},\nActual XML Value: {}", xmlUtilities.getDocumentAsString(expectedValue).trim()
                    , xmlUtilities.getDocumentAsString(value).trim());

            DetailedDiff difference = new DetailedDiff(new Diff(expectedValue, value));
            if (!difference.similar())
                logger.warn("Differences exist between two documents: {}", difference.getAllDifferences());
            else
                logger.debug("No differences exist for input");
            return difference.similar();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        try {
            String value = "XmlTestResource:" + xmlUtilities.getDocumentAsString(getValue());
            value = value.replace("\n", "").replace("\r", "");
            if (value.length() < 100) return value;
            else return value.substring(0, 97) + "...";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getContentType() {
        return "application/xml";
    }
}
