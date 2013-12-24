package nz.ac.auckland.integration.tests.validators;

import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
import nz.ac.auckland.integration.testing.resource.SoapFaultTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.utility.XmlUtilities;
import nz.ac.auckland.integration.testing.validator.Validator;
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
        assertFalse(new SoapFaultTestResource(new QName("foo", "baz"), "foo").validate(null));
    }

    @Test
    public void testNullException() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        assertFalse(new SoapFaultTestResource(new QName("foo", "baz"), "foo").validate(e));
    }

    @Test
    public void testNonSoapFaultException() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(new IOException());
        assertFalse(new SoapFaultTestResource(new QName("foo", "baz"), "foo").validate(e));
    }

    @Test
    public void testFaultMessageValidator() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message", OrchestratedTestBuilder.SOAPFAULT_SERVER);
        e.setException(fault);

        Validator validator = new SoapFaultTestResource(OrchestratedTestBuilder.SOAPFAULT_SERVER, "message");

        assertTrue(validator.validate(e));
        fault = new SoapFault("message1", OrchestratedTestBuilder.SOAPFAULT_SERVER);
        e.setException(fault);

        assertFalse(validator.validate(e));
    }


    @Test
    public void testQNameFaultCodeValidation() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message", OrchestratedTestBuilder.SOAPFAULT_SERVER);
        e.setException(fault);

        Validator validator = new SoapFaultTestResource(OrchestratedTestBuilder.SOAPFAULT_SERVER, "message");

        assertTrue(validator.validate(e));
        fault = new SoapFault("message", OrchestratedTestBuilder.SOAPFAULT_CLIENT);
        e.setException(fault);

        assertFalse(validator.validate(e));
    }

    @Test
    public void testFaultDetailValidator() throws Exception {
        XmlUtilities xmlUtilities = new XmlUtilities();

        SoapFaultTestResource resource = new SoapFaultTestResource(new QName("www.foo.com", "baz"),
                "message", new XmlTestResource(xmlUtilities.getXmlAsDocument("<foo/>")));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message", new QName("www.foo.com", "baz"));
        fault.setDetail(xmlUtilities.getXmlAsDocument("<foo/>").getDocumentElement());
        e.setException(fault);
        assertTrue(resource.validate(e));
    }

    @Test
    public void testInvalidFaultDetail() throws Exception {
        XmlUtilities xmlUtilities = new XmlUtilities();

        SoapFaultTestResource resource = new SoapFaultTestResource(new QName("www.foo.com", "baz"),
                "message", new XmlTestResource(xmlUtilities.getXmlAsDocument("<foo/>")));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message", new QName("www.foo.com", "baz"));
        fault.setDetail(xmlUtilities.getXmlAsDocument("<foo1/>").getDocumentElement());
        e.setException(fault);
        assertFalse(resource.validate(e));
    }

}
