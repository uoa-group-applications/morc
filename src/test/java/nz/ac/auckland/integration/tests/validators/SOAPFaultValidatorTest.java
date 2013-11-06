package nz.ac.auckland.integration.tests.validators;

import nz.ac.auckland.integration.testing.resource.PlainTextTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.utility.XMLUtilities;
import nz.ac.auckland.integration.testing.validator.SOAPFaultValidator;
import nz.ac.auckland.integration.testing.validator.Validator;
import nz.ac.auckland.integration.testing.validator.XmlValidator;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.cxf.binding.soap.SoapFault;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.io.IOException;


public class SOAPFaultValidatorTest extends Assert {
    @Test
    public void testNullExchange() throws Exception {
        assertFalse(new SOAPFaultValidator().validate(null));
    }

    @Test
    public void testNullException() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        assertFalse(new SOAPFaultValidator().validate(e));
    }

    @Test
    public void testNonSoapFaultException() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(new IOException());
        assertFalse(new SOAPFaultValidator().validate(e));
    }

    @Test
    public void testFaultMessageValidator() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message",null);
        e.setException(fault);

        Validator validator = new SOAPFaultValidator.Builder().faultMessageValidator("message").build();

        assertTrue(validator.validate(e));
        fault = new SoapFault("message1",null);
        e.setException(fault);

        assertFalse(validator.validate(e));
    }

    @Test
    public void testValidatorFaultMessage() throws Exception {
        Validator v = new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return true;
            }
        };

        SOAPFaultValidator validator = new SOAPFaultValidator.Builder().faultMessageValidator(v).build();

        assertEquals(validator.getFaultMessageValidator(), v);
    }

    @Test
    public void testPlainTextMessageValidator() throws Exception {
        SOAPFaultValidator validator = new SOAPFaultValidator.Builder().faultMessageValidator(
                new PlainTextTestResource("foo")).build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("foo");

        assertTrue(validator.getFaultMessageValidator().validate(e));
    }


    @Test
    public void testFaultCodeValidator() throws Exception {
        SOAPFaultValidator validator = new SOAPFaultValidator.Builder().codeValidator(new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                QName name = exchange.getIn().getBody(QName.class);
                return name.getNamespaceURI().equals("www.foo.com") && name.getLocalPart().equals("baz");
            }
        }).build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message",new QName("www.foo.com","baz"));
        e.setException(fault);

        assertTrue(validator.validate(e));

        fault = new SoapFault("message",new QName("www.foo.com","foo"));
        e.setException(fault);

        assertFalse(validator.validate(e));
    }

    @Test
    public void testQNameFaultCodeValidation() throws Exception {
        SOAPFaultValidator validator = new SOAPFaultValidator.Builder()
                .codeValidator(new QName("www.foo.com","baz"))
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message",new QName("www.foo.com","baz"));
        e.setException(fault);
        assertTrue(validator.validate(e));

        fault = new SoapFault("message",new QName("www.foo.com","foo"));
        e.setException(fault);

        assertFalse(validator.validate(e));
    }

    @Test
    public void testFaultDetailValidator() throws Exception {
        XMLUtilities xmlUtilities = new XMLUtilities();

        SOAPFaultValidator validator = new SOAPFaultValidator.Builder().detailValidator(
                new XmlValidator(new XmlTestResource(xmlUtilities.getXmlAsDocument("<foo/>"))))
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message",new QName("www.foo.com","baz"));
        fault.setDetail(xmlUtilities.getXmlAsDocument("<foo/>").getDocumentElement());
        e.setException(fault);
        assertTrue(validator.validate(e));
    }

    @Test
    public void testFaultDetailXMLResourceValidator() throws Exception {
        XMLUtilities xmlUtilities = new XMLUtilities();

        SOAPFaultValidator validator = new SOAPFaultValidator.Builder().detailValidator(
                        new XmlTestResource(xmlUtilities.getXmlAsDocument("<foo/>")))
                        .build();

        assertTrue(validator.getDetailValidator().validate("<foo/>"));
    }

    @Test
    public void testInvalidFaultMessage() throws Exception {
        SOAPFaultValidator validator = new SOAPFaultValidator.Builder().faultMessageValidator(new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return false;
            }
        }).codeValidator(new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return true;
            }
        }).build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        SoapFault fault = new SoapFault("message",new QName("www.foo.com","baz"));
        e.setException(fault);
        assertFalse(validator.validate(e));
    }

    @Test
    public void testInvalidCode() throws Exception {
        SOAPFaultValidator validator = new SOAPFaultValidator.Builder().faultMessageValidator(new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return true;
            }
        }).codeValidator(new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return false;
            }
        }).build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        SoapFault fault = new SoapFault("message",new QName("www.foo.com","baz"));
        e.setException(fault);
        assertFalse(validator.validate(e));
    }

    @Test
    public void testInvalidDetail() throws Exception {
        XMLUtilities xmlUtilities = new XMLUtilities();

        SOAPFaultValidator validator = new SOAPFaultValidator.Builder().detailValidator(
                        new XmlTestResource(xmlUtilities.getXmlAsDocument("<foo/>")))
                        .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        SoapFault fault = new SoapFault("message",new QName("www.foo.com","baz"));
        fault.setDetail(xmlUtilities.getXmlAsDocument("<baz/>").getDocumentElement());
        e.setException(fault);

        assertFalse(validator.validate(e));
    }

    @Test
    public void testNullExchangeToCodeValidator() throws Exception {
        SOAPFaultValidator validator = new SOAPFaultValidator.Builder()
                .codeValidator(new QName("foo", "baz")).build();

        assertFalse(validator.getCodeValidator().validate(null));
    }

    @Test
    public void testNoCodeInBody() throws Exception {
        SOAPFaultValidator validator = new SOAPFaultValidator.Builder()
                .codeValidator(new QName("foo","baz")).build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        assertFalse(validator.getCodeValidator().validate(e));
    }

}
