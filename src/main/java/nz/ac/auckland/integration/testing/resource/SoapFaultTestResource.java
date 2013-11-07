package nz.ac.auckland.integration.testing.resource;

import nz.ac.auckland.integration.testing.utility.XMLUtilities;
import org.apache.cxf.binding.soap.SoapFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;

/**
 * A way to specify a SOAP Fault to return back to the application under testing
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SoapFaultTestResource implements TestResource {

    private static final Logger logger = LoggerFactory.getLogger(SoapFaultTestResource.class);

    private String message;
    private QName faultCode;
    private XmlTestResource xmlDetail;

    private XMLUtilities xmlUtilities = new XMLUtilities();

    public SoapFaultTestResource(QName faultCode, String message) {
        this.faultCode = faultCode;
        this.message = message;
    }

    public SoapFaultTestResource(QName faultCode, String message, XmlTestResource xmlDetail) {
        this.faultCode = faultCode;
        this.message = message;
        this.xmlDetail = xmlDetail;
    }

    public XMLUtilities getXmlUtilities() {
        return xmlUtilities;
    }

    /**
     * In case you have any special XML requirements
     */
    public void setXmlUtilities(XMLUtilities xmlUtilities) {
        this.xmlUtilities = xmlUtilities;
    }

    @Override
    public SoapFault getValue() throws Exception {
        SoapFault fault = new SoapFault(message, faultCode);
        if (xmlDetail != null) {
            Document document = xmlDetail.getValue();
            if (!document.getDocumentElement().getNodeName().equals("detail")) {
                logger.warn("The provided XML is not wrapped in a detail element, this is required and one will be added");

                DocumentBuilder builder = xmlUtilities.getDocumentBuilder();
                Document newDetailDocument = builder.newDocument();
                Element newDetailElement = newDetailDocument.createElement("detail");
                newDetailElement.appendChild(newDetailDocument.importNode(document.getDocumentElement(), true));

                fault.setDetail(newDetailElement);
            } else
                fault.setDetail(document.getDocumentElement());
        }

        return fault;
    }
}
