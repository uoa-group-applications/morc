package nz.ac.auckland.integration.testing.resource;

import nz.ac.auckland.integration.testing.utility.XPathSelector;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * Provides a mechanism for retrieving XML values from a file/URL/String and also
 * validating a response from a target service.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class XmlTestResource extends StaticTestResource<Document> {

    private XPathSelector xpathSelector;

    public XmlTestResource(String value) {
        this(getXmlAsDocument(value));
    }

    public XmlTestResource(Document value) {
        super(value);
    }

    public XmlTestResource(File file) {
        super(file);
    }

    public XmlTestResource(URL url) {
        super(url);
    }

    public XmlTestResource(String value, XPathSelector xpathSelector) {
        this(getXmlAsDocument(value),xpathSelector);
    }

    public XmlTestResource(Document value, XPathSelector xpathSelector) {
        super(value);
        this.xpathSelector = xpathSelector;
    }

    public XmlTestResource(File file, XPathSelector xpathSelector) {
        super(file);
        this.xpathSelector = xpathSelector;
    }

    public XmlTestResource(URL url, XPathSelector xpathSelector) {
        super(url);
        this.xpathSelector = xpathSelector;
    }

    protected static synchronized Document getXmlAsDocument(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected synchronized Document getXmlAsDocument(File xmlFile) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(xmlFile);
    }

    /**
     * @return The XML document from the resource as a String
     * @throws Exception XPathEvaluationException thrown if the xpath could not be evaluated correctly
     */
    protected Document getResource(File file) throws Exception {
        if (xpathSelector != null)
            return xpathSelector.evalute(getXmlAsDocument(file));
        else
            return getXmlAsDocument(file);
    }


}
