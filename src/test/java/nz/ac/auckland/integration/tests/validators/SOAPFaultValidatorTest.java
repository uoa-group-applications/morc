package nz.ac.auckland.integration.tests.validators;

import nz.ac.auckland.integration.testing.MorcTestBuilder;
import nz.ac.auckland.integration.testing.resource.SoapFaultTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.utility.XmlUtilities;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.cxf.binding.soap.SoapFault;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.io.IOException;


public class SOAPFaultValidatorTest extends Assert {

    public SOAPFaultValidatorTest() {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalizeWhitespace(true);
        XMLUnit.setIgnoreComments(true);
    }

    @Test
    public void testNullExchange() throws Exception {
        assertFalse(new SoapFaultTestResource(new QName("foo", "baz"), "foo").matches(null));
    }

    @Test
    public void testNullException() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        assertFalse(new SoapFaultTestResource(new QName("foo", "baz"), "foo").matches(e));
    }

    @Test
    public void testNonSoapFaultException() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new IOException());
        assertFalse(new SoapFaultTestResource(new QName("foo", "baz"), "foo").matches(e));
    }

    @Test
    public void testFaultMessageValidator() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message", MorcTestBuilder.SOAPFAULT_SERVER);
        e.setProperty(Exchange.EXCEPTION_CAUGHT, fault);

        Predicate predicate = new SoapFaultTestResource(MorcTestBuilder.SOAPFAULT_SERVER, "message");

        assertTrue(predicate.matches(e));
        fault = new SoapFault("message1", MorcTestBuilder.SOAPFAULT_SERVER);
        e.setProperty(Exchange.EXCEPTION_CAUGHT, fault);

        assertFalse(predicate.matches(e));
    }


    @Test
    public void testQNameFaultCodeValidation() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message", MorcTestBuilder.SOAPFAULT_SERVER);
        e.setProperty(Exchange.EXCEPTION_CAUGHT, fault);

        Predicate predicate = new SoapFaultTestResource(MorcTestBuilder.SOAPFAULT_SERVER, "message");

        assertTrue(predicate.matches(e));
        fault = new SoapFault("message", MorcTestBuilder.SOAPFAULT_CLIENT);
        e.setProperty(Exchange.EXCEPTION_CAUGHT, fault);

        assertFalse(predicate.matches(e));
    }

    @Test
    public void testFaultDetailValidator() throws Exception {
        XmlUtilities xmlUtilities = new XmlUtilities();

        SoapFaultTestResource resource = new SoapFaultTestResource(new QName("www.foo.com", "baz"),
                "message", new XmlTestResource(xmlUtilities.getXmlAsDocument("<foo/>")));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message", new QName("www.foo.com", "baz"));
        fault.setDetail(xmlUtilities.getXmlAsDocument("<detail><foo/></detail>").getDocumentElement());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, fault);
        assertTrue(resource.matches(e));
    }

    @Test
    public void testInvalidFaultDetail() throws Exception {
        XmlUtilities xmlUtilities = new XmlUtilities();

        SoapFaultTestResource resource = new SoapFaultTestResource(new QName("www.foo.com", "baz"),
                "message", new XmlTestResource(xmlUtilities.getXmlAsDocument("<foo/>")));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        SoapFault fault = new SoapFault("message", new QName("www.foo.com", "baz"));
        fault.setDetail(xmlUtilities.getXmlAsDocument("<foo1/>").getDocumentElement());
        e.setProperty(Exchange.EXCEPTION_CAUGHT, fault);
        assertFalse(resource.matches(e));
    }

}
