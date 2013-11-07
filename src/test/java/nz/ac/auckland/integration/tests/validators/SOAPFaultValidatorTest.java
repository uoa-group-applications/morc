package nz.ac.auckland.integration.tests.validators;

import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
import nz.ac.auckland.integration.testing.resource.PlainTextTestResource;
import nz.ac.auckland.integration.testing.resource.SoapFaultTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.utility.XmlUtilities;
import nz.ac.auckland.integration.testing.validator.SoapFaultValidator;
import nz.ac.auckland.integration.testing.validator.Validator;
import nz.ac.auckland.integration.testing.validator.XmlValidator;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.cxf.binding.soap.SoapFault;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.io.IOException;


public class SOAPFaultValidatorTest extends Assert {
    @Test
    public void testNullExchange() throws Exception {
        assertFalse(new SoapFaultValidator().validate(null));
    }

    @Test
    public void testNullException() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        assertFalse(new SoapFaultValidator().validate(e));
    }

    @Test
    public void testNonSoapFaultException() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(new IOException());
        assertFalse(new SoapFaultValidator().validate(e));
    }

    @Test
    public void testFaultMessageValidator() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message", null);
        e.setException(fault);

        Validator validator = new SoapFaultValidator.Builder().faultMessageValidator("message").build();

        assertTrue(validator.validate(e));
        fault = new SoapFault("message1", null);
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

        SoapFaultValidator validator = new SoapFaultValidator.Builder().faultMessageValidator(v).build();

        assertEquals(validator.getFaultMessageValidator(), v);
    }

    @Test
    public void testPlainTextMessageValidator() throws Exception {
        SoapFaultValidator validator = new SoapFaultValidator.Builder().faultMessageValidator(
                new PlainTextTestResource("foo")).build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("foo");

        assertTrue(validator.getFaultMessageValidator().validate(e));
    }


    @Test
    public void testFaultCodeValidator() throws Exception {
        SoapFaultValidator validator = new SoapFaultValidator.Builder().codeValidator(new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                QName name = exchange.getIn().getBody(QName.class);
                return name.getNamespaceURI().equals("www.foo.com") && name.getLocalPart().equals("baz");
            }
        }).build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message", new QName("www.foo.com", "baz"));
        e.setException(fault);

        assertTrue(validator.validate(e));

        fault = new SoapFault("message", new QName("www.foo.com", "foo"));
        e.setException(fault);

        assertFalse(validator.validate(e));
    }

    @Test
    public void testQNameFaultCodeValidation() throws Exception {
        SoapFaultValidator validator = new SoapFaultValidator.Builder()
                .codeValidator(new QName("www.foo.com", "baz"))
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message", new QName("www.foo.com", "baz"));
        e.setException(fault);
        assertTrue(validator.validate(e));

        fault = new SoapFault("message", new QName("www.foo.com", "foo"));
        e.setException(fault);

        assertFalse(validator.validate(e));
    }

    @Test
    public void testFaultDetailValidator() throws Exception {
        XmlUtilities xmlUtilities = new XmlUtilities();

        SoapFaultValidator validator = new SoapFaultValidator.Builder().detailValidator(
                new XmlValidator(new XmlTestResource(xmlUtilities.getXmlAsDocument("<foo/>"))))
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message", new QName("www.foo.com", "baz"));
        fault.setDetail(xmlUtilities.getXmlAsDocument("<foo/>").getDocumentElement());
        e.setException(fault);
        assertTrue(validator.validate(e));
    }

    @Test
    public void testFaultDetailXMLResourceValidator() throws Exception {
        XmlUtilities xmlUtilities = new XmlUtilities();

        SoapFaultValidator validator = new SoapFaultValidator.Builder().detailValidator(
                new XmlTestResource(xmlUtilities.getXmlAsDocument("<foo/>")))
                .build();

        assertTrue(validator.getDetailValidator().validate("<foo/>"));
    }

    @Test
    public void testInvalidFaultMessage() throws Exception {
        SoapFaultValidator validator = new SoapFaultValidator.Builder().faultMessageValidator(new Validator() {
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

        SoapFault fault = new SoapFault("message", new QName("www.foo.com", "baz"));
        e.setException(fault);
        assertFalse(validator.validate(e));
    }

    @Test
    public void testInvalidCode() throws Exception {
        SoapFaultValidator validator = new SoapFaultValidator.Builder().faultMessageValidator(new Validator() {
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

        SoapFault fault = new SoapFault("message", new QName("www.foo.com", "baz"));
        e.setException(fault);
        assertFalse(validator.validate(e));
    }

    @Test
    public void testInvalidDetail() throws Exception {
        XmlUtilities xmlUtilities = new XmlUtilities();

        SoapFaultValidator validator = new SoapFaultValidator.Builder().detailValidator(
                new XmlTestResource(xmlUtilities.getXmlAsDocument("<foo/>")))
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        SoapFault fault = new SoapFault("message", new QName("www.foo.com", "baz"));
        fault.setDetail(xmlUtilities.getXmlAsDocument("<baz/>").getDocumentElement());
        e.setException(fault);

        assertFalse(validator.validate(e));
    }

    @Test
    public void testNullExchangeToCodeValidator() throws Exception {
        SoapFaultValidator validator = new SoapFaultValidator.Builder()
                .codeValidator(new QName("foo", "baz")).build();

        assertFalse(validator.getCodeValidator().validate(null));
    }

    @Test
    public void testNoCodeInBody() throws Exception {
        SoapFaultValidator validator = new SoapFaultValidator.Builder()
                .codeValidator(new QName("foo", "baz")).build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        assertFalse(validator.getCodeValidator().validate(e));
    }

    @Test
    public void testSoapFaultTestResourceConstructor() throws Exception {
        SoapFaultTestResource resource = new SoapFaultTestResource(OrchestratedTestBuilder.SOAPFAULT_CLIENT, "foo");
        SoapFaultValidator validator = new SoapFaultValidator(resource);

        SoapFault fault = new SoapFault("foo", OrchestratedTestBuilder.SOAPFAULT_CLIENT);
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(fault);
        assertTrue(validator.validate(e));
    }

    @Test
    public void testSoapFaultTestResourceWithDetailConstructor() throws Exception {
        XmlTestResource xmlTestResource = new XmlTestResource(new XmlUtilities().getXmlAsDocument("<detail><foo/></detail>"));

        SoapFaultTestResource resource = new SoapFaultTestResource(OrchestratedTestBuilder.SOAPFAULT_CLIENT, "foo",xmlTestResource);
        SoapFaultValidator validator = new SoapFaultValidator(resource);

        SoapFault fault = new SoapFault("foo", OrchestratedTestBuilder.SOAPFAULT_CLIENT);
        fault.setDetail(xmlTestResource.getValue().getDocumentElement());
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(fault);
        assertTrue(validator.validate(e));
    }
}
