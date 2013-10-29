package nz.ac.auckland.integration.testing.validator;

import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import org.apache.camel.Exchange;
import org.apache.cxf.binding.soap.SoapFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

public class SOAPFaultValidator implements Validator {

    private Logger logger = LoggerFactory.getLogger(SOAPFaultValidator.class);

    private String message;
    private QName code;
    private XmlTestResource detailResource;

    public SOAPFaultValidator(String message) {
        this.message = message;
    }

    public SOAPFaultValidator(QName code) {
        this.code = code;
    }

    public SOAPFaultValidator(String message, QName code) {
        this.code = code;
        this.message = message;
    }

    public SOAPFaultValidator(QName code, String message, XmlTestResource detailResource) {
        this.code = code;
        this.message = message;
        this.detailResource = detailResource;
    }


    public boolean validate(Exchange e) {

        Exception ex = e.getException();

        if (!(e instanceof SoapFault)) {
            logger.error("An unexpected error occurred during exception validation",ex);
            throw new RuntimeException(ex);
        }

        return false;
    }
}
