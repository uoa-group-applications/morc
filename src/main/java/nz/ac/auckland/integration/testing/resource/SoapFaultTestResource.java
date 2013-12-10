package nz.ac.auckland.integration.testing.resource;

import nz.ac.auckland.integration.testing.utility.XmlUtilities;
import nz.ac.auckland.integration.testing.validator.Validator;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultExchange;
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
public class SoapFaultTestResource implements TestResource<SoapFault>, Validator {

    private static final Logger logger = LoggerFactory.getLogger(SoapFaultTestResource.class);

    private String message;
    private QName faultCode;
    private XmlTestResource xmlDetail;

    private XmlUtilities xmlUtilities = new XmlUtilities();

    public SoapFaultTestResource(QName faultCode, String message) {
        this.faultCode = faultCode;
        this.message = message;
    }

    public SoapFaultTestResource(QName faultCode, String message, XmlTestResource xmlDetail) {
        this.faultCode = faultCode;
        this.message = message;
        this.xmlDetail = xmlDetail;
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

    public boolean validate(Exchange e) {
        if (e == null) return false;
        Throwable t = e.getException();

        if (!(t instanceof SoapFault)) {
            logger.warn("An unexpected error occurred during SOAP Fault validation", t);
            return false;
        }

        SoapFault fault = (SoapFault) t;

        Validator faultMessageValidator;
        Validator codeValidator;
        Validator detailValidator = null;

        try {
            SoapFault expectedValue = getValue();
            faultMessageValidator = new PlainTextTestResource(expectedValue.getMessage());
            codeValidator = new QNameValidator(expectedValue.getFaultCode());
            if (expectedValue.getDetail() != null)
                detailValidator = new XmlTestResource(expectedValue.getDetail().getOwnerDocument());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        boolean validMessage = true, validCode = true, validDetail = true;

        Exchange faultMessageExchange = new DefaultExchange(e.getContext());
        faultMessageExchange.getIn().setBody(fault.getMessage());
        validMessage = faultMessageValidator.validate(faultMessageExchange);

        if (!validMessage)
            logger.warn("The SOAP Fault message is not as expected; received {}", fault.getCode());

        Exchange codeExchange = new DefaultExchange(e.getContext());
        codeExchange.getIn().setBody(fault.getFaultCode());
        validCode = codeValidator.validate(codeExchange);

        if (!validCode) logger.warn("The SOAP Fault code is not as expected; received {}", fault.getCode());

        if (detailValidator != null) {
            Exchange detailExchange = new DefaultExchange(e.getContext());
            detailExchange.getIn().setBody(fault.getDetail());
            validDetail = detailValidator.validate(detailExchange);

            String detail = xmlUtilities.getDocumentAsString(fault.getDetail());
            if (!validDetail) logger.warn("The SOAP Fault detail is not as expected; received {}", detail);
        }

        return validMessage && validCode && validDetail;
    }

    private static class QNameValidator implements Validator {

        private QName expectedCode;

        public QNameValidator(QName expectedCode) {
            this.expectedCode = expectedCode;
        }

        @Override
        public boolean validate(Exchange exchange) {
            //The receive QName is passed in from the body
            return exchange != null &&
                    exchange.getIn().getBody(QName.class) != null &&
                    exchange.getIn().getBody(QName.class).equals(expectedCode);
        }
    }

}
