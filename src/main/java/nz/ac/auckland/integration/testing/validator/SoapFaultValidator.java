package nz.ac.auckland.integration.testing.validator;

import nz.ac.auckland.integration.testing.resource.PlainTextTestResource;
import nz.ac.auckland.integration.testing.resource.SoapFaultTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.utility.XmlUtilities;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultExchange;
import org.apache.cxf.binding.soap.SoapFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;

/**
 * A validator for ensuring a SOAP fault being returned - this will likely only work on SOAP 1.1.
 * I will think more about Soap 1.2 if there's demand
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SoapFaultValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(SoapFaultValidator.class);

    private Validator faultMessageValidator;
    private Validator codeValidator;
    private XmlValidator detailValidator;

    private XmlUtilities xmlUtilities = new XmlUtilities();

    public SoapFaultValidator() {
        //possible to just expect some kind of SOAP Fault
    }

    public SoapFaultValidator(SoapFaultTestResource resource) {
        try {
            faultMessageValidator = new PlainTextValidator(new PlainTextTestResource(resource.getValue().getMessage()));
            codeValidator = new QNameValidator(resource.getValue().getFaultCode());
            if (resource.getValue().getDetail() != null)
                detailValidator = new XmlValidator(new XmlTestResource(resource.getValue().getDetail().getOwnerDocument()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Validator getFaultMessageValidator() {
        return faultMessageValidator;
    }

    public Validator getCodeValidator() {
        return codeValidator;
    }

    public XmlValidator getDetailValidator() {
        return detailValidator;
    }

    public boolean validate(Exchange e) {
        if (e == null) return false;
        Throwable t = e.getException();

        if (!(t instanceof SoapFault)) {
            logger.warn("An unexpected error occurred during SOAP Fault validation", t);
            return false;
        }

        SoapFault fault = (SoapFault) t;

        boolean validMessage = true, validCode = true, validDetail = true;

        if (faultMessageValidator != null) {
            Exchange faultMessageExchange = new DefaultExchange(e.getContext());
            faultMessageExchange.getIn().setBody(fault.getMessage());
            validMessage = faultMessageValidator.validate(faultMessageExchange);

            if (!validMessage)
                logger.warn("The SOAP Fault message is not as expected; received {}", fault.getCode());
        }

        if (codeValidator != null) {
            Exchange codeExchange = new DefaultExchange(e.getContext());
            codeExchange.getIn().setBody(fault.getFaultCode());
            validCode = codeValidator.validate(codeExchange);

            if (!validCode) logger.warn("The SOAP Fault code is not as expected; received {}", fault.getCode());
        }

        if (detailValidator != null) {
            Exchange detailExchange = new DefaultExchange(e.getContext());
            detailExchange.getIn().setBody(fault.getDetail());
            validDetail = detailValidator.validate(detailExchange);

            String detail = xmlUtilities.getDocumentAsString(fault.getDetail());
            if (!validDetail) logger.warn("The SOAP Fault detail is not as expected; received {}", detail);
        }

        return validMessage && validCode && validDetail;
    }

    public static class Builder {
        private Validator faultMessageValidator;
        private Validator codeValidator;
        private XmlValidator detailValidator;

        /**
         * @param faultMessageValidator A validator for the SOAP Fault Message
         */
        public Builder faultMessageValidator(Validator faultMessageValidator) {
            this.faultMessageValidator = faultMessageValidator;
            return this;
        }

        /**
         * @param resource A plain text resource to validate the SOAP Fault message against
         */
        public Builder faultMessageValidator(PlainTextTestResource resource) {
            this.faultMessageValidator = new PlainTextValidator(resource);
            return this;
        }

        /**
         * @param expectedMessage A string for the expected SOAP Fault Message
         */
        public Builder faultMessageValidator(String expectedMessage) {
            this.faultMessageValidator = new PlainTextValidator(new PlainTextTestResource(expectedMessage));
            return this;
        }

        /**
         * @param codeValidator A validator for the expected code, where the code is passed
         *                      as a QName in the exchange body
         */
        public Builder codeValidator(Validator codeValidator) {
            this.codeValidator = codeValidator;
            return this;
        }

        /**
         * @param expectedCode A QName that we expect to receive
         */
        public Builder codeValidator(final QName expectedCode) {
            this.codeValidator = new QNameValidator(expectedCode);
            return this;
        }

        /**
         * @param detailValidator A validator for the SOAP Fault (XML) element
         */
        public Builder detailValidator(XmlValidator detailValidator) {
            this.detailValidator = detailValidator;
            return this;
        }

        /**
         * @param resource An XML resource of the expected SOAP Fault (XML) element
         */
        public Builder detailValidator(XmlTestResource resource) {
            this.detailValidator = new XmlValidator(resource);
            return this;
        }

        public SoapFaultValidator build() {
            SoapFaultValidator validator = new SoapFaultValidator();
            validator.faultMessageValidator = this.faultMessageValidator;
            validator.codeValidator = this.codeValidator;
            validator.detailValidator = this.detailValidator;
            return validator;
        }
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
