package nz.ac.auckland.integration.testing.utility;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;

/**
 * A class for handling XML documents. It is a bit heavy having
 * to re-initialize everything each time, but the lack of thread safe in Java
 * and the fact that performance isn't too much of a concern right now means
 * I'm leaving it as is.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class XmlUtilities {

    public DocumentBuilder getDocumentBuilder() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            dbf.setNamespaceAware(true);
            return dbf.newDocumentBuilder();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Document getXmlAsDocument(String xml) {
        try {
            return getDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Document getXmlAsDocument(File xmlFile) {
        try {
            return getDocumentBuilder().parse(xmlFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Transformer getTransformer() {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            return transformer;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getDocumentAsString(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            getTransformer().transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getDocumentAsString(Element doc) {
        try {
            StringWriter sw = new StringWriter();
            getTransformer().transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
